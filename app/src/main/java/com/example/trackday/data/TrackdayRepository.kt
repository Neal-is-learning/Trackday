package com.example.trackday.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// Single DataStore for the whole process. Both the UI ViewModel and the
// background reminder infrastructure (alarm receiver / check-in activity) go
// through this repository, so there is exactly one DataStore instance.
private val Context.dataStore by preferencesDataStore("trackday_prefs")

/**
 * Single source of truth for persistence. Suspend read-modify-write helpers so
 * short-lived components (BroadcastReceiver via goAsync, standalone Activity)
 * can safely append a record without racing the in-memory UI state.
 */
class TrackdayRepository private constructor(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // v2: dropped the pre-imported demo records — old v1 data is ignored so
    // devices that already ran the app start with an empty timeline too.
    private val recordsKey = stringPreferencesKey("records_v2")
    private val tagsKey = stringPreferencesKey("tags_v1")
    private val remindersKey = stringPreferencesKey("reminders_v1")
    private val summariesKey = stringPreferencesKey("summaries_v1")
    private val photosKey = stringPreferencesKey("summary_photos_v1")
    // marks a check-in that fired but hasn't been answered yet ("date|start|end")
    private val pendingKey = stringPreferencesKey("pending_checkin_v1")

    // ── Records ───────────────────────────────────────────────────────────────

    suspend fun loadRecords(): Map<String, List<TimeRecord>> {
        val raw = appContext.dataStore.data.first()[recordsKey]
        return if (raw == null) {
            // seed on first launch so both UI + reminders see the same data
            saveRecords(DEFAULT_RECORDS)
            DEFAULT_RECORDS
        } else {
            runCatching { json.decodeFromString<Map<String, List<TimeRecord>>>(raw) }
                .getOrDefault(DEFAULT_RECORDS)
        }
    }

    suspend fun saveRecords(map: Map<String, List<TimeRecord>>) {
        appContext.dataStore.edit { it[recordsKey] = json.encodeToString(map) }
    }

    /** Read-modify-write append of a single record for [date]. Returns nothing. */
    suspend fun appendRecord(date: LocalDate, tag: String, cat: String, start: String, end: String, note: String) {
        val current = loadRecords().toMutableMap()
        val key = date.format(fmt)
        val record = TimeRecord(UUID.randomUUID().toString(), tag, cat, start, end, note)
        current[key] = (current[key] ?: emptyList()) + record
        saveRecords(current)
    }

    // ── Pending check-in (fired but not yet answered) ─────────────────────────
    // Guards against double-recording: the popup and the 30s auto-inherit alarm
    // both consult this. Whoever handles it first clears it.

    /** Format: "yyyy-MM-dd|HH:mm|HH:mm" */
    suspend fun setPendingCheckIn(date: LocalDate, start: String, end: String) {
        val value = "${date.format(fmt)}|$start|$end"
        appContext.dataStore.edit { it[pendingKey] = value }
    }

    /** Returns Triple(date, start, end) or null if none pending. */
    suspend fun getPendingCheckIn(): Triple<LocalDate, String, String>? {
        val raw = appContext.dataStore.data.first()[pendingKey] ?: return null
        val parts = raw.split("|")
        if (parts.size != 3) return null
        return runCatching {
            Triple(LocalDate.parse(parts[0]), parts[1], parts[2])
        }.getOrNull()
    }

    suspend fun clearPendingCheckIn() {
        appContext.dataStore.edit { it.remove(pendingKey) }
    }

    /**
     * The most recent record's tag (by latest end time on the latest day with
     * records), used to auto-inherit when the user ignores a check-in popup.
     * Returns null if there are no records yet.
     */
    suspend fun lastTag(): String? {
        val all = loadRecords()
        if (all.isEmpty()) return null
        val latestDayKey = all.keys.maxOrNull() ?: return null
        val list = all[latestDayKey].orEmpty()
        return list.maxByOrNull { it.endMinutes }?.tag
    }

    // ── Tags ────────────────────────────────────────────────────────────────

    suspend fun loadTags(): List<TagGroup> {
        val raw = appContext.dataStore.data.first()[tagsKey]
        return if (raw == null) DEFAULT_TAG_GROUPS
        else runCatching { json.decodeFromString<List<TagGroup>>(raw) }
            .getOrDefault(DEFAULT_TAG_GROUPS)
    }

    suspend fun saveTags(groups: List<TagGroup>) {
        appContext.dataStore.edit { it[tagsKey] = json.encodeToString(groups) }
    }

    /** Category key for a tag, resolving through the live tag groups. */
    suspend fun catForTag(tagName: String): String {
        loadTags().forEach { g ->
            if (g.children.any { it.name == tagName }) return g.name.toCatKey()
        }
        return "life"
    }

    // ── Reminder settings ─────────────────────────────────────────────────────

    suspend fun loadReminderSettings(): ReminderSettings {
        val raw = appContext.dataStore.data.first()[remindersKey]
        return if (raw == null) ReminderSettings()
        else runCatching { json.decodeFromString<ReminderSettings>(raw) }
            .getOrDefault(ReminderSettings())
    }

    suspend fun saveReminderSettings(s: ReminderSettings) {
        appContext.dataStore.edit { it[remindersKey] = json.encodeToString(s) }
    }

    // ── Summaries ─────────────────────────────────────────────────────────────

    suspend fun loadSummaries(): Map<String, String> {
        val raw = appContext.dataStore.data.first()[summariesKey]
        return if (raw == null) emptyMap()
        else runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrDefault(emptyMap())
    }

    suspend fun saveSummaries(map: Map<String, String>) {
        appContext.dataStore.edit { it[summariesKey] = json.encodeToString(map) }
    }

    // ── Summary photos ("range|dateKey" -> list of persisted image URIs) ───────

    suspend fun loadPhotos(): Map<String, List<String>> {
        val raw = appContext.dataStore.data.first()[photosKey]
        return if (raw == null) emptyMap()
        else runCatching { json.decodeFromString<Map<String, List<String>>>(raw) }
            .getOrDefault(emptyMap())
    }

    suspend fun savePhotos(map: Map<String, List<String>>) {
        appContext.dataStore.edit { it[photosKey] = json.encodeToString(map) }
    }

    /**
     * Copy a picked image (a temporary content:// URI from the photo picker)
     * into app-private storage and return a stable file:// URI. This avoids the
     * flaky persistable-permission dance on picker URIs (which caused the
     * "must add twice before it shows" bug) — the copied file is always
     * readable afterwards.
     */
    suspend fun importPhoto(sourceUri: android.net.Uri): String? {
        return runCatching {
            val dir = java.io.File(appContext.filesDir, "summary_photos").apply { mkdirs() }
            val dest = java.io.File(dir, "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            android.net.Uri.fromFile(dest).toString()
        }.getOrNull()
    }

    private fun String.toCatKey(): String = when (this) {
        "学习" -> "learn"
        "休息" -> "rest"
        "工作" -> "work"
        else -> "life"
    }

    companion object {
        @Volatile private var INSTANCE: TrackdayRepository? = null
        fun get(context: Context): TrackdayRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackdayRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
