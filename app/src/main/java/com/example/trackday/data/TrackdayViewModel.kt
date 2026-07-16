package com.example.trackday.data

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TrackdayViewModel(private val context: Context) : ViewModel() {

    private val repo = TrackdayRepository.get(context)

    // ── Today (real system date) ──────────────────────────────────────────────
    var today: LocalDate = LocalDate.now()
        private set
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Refresh "today" when returning to the app (e.g. after midnight). */
    fun refreshToday() { today = LocalDate.now() }

    // ── State ─────────────────────────────────────────────────────────────────
    var records by mutableStateOf<Map<String, List<TimeRecord>>>(DEFAULT_RECORDS)
        private set

    var tagGroups by mutableStateOf<List<TagGroup>>(DEFAULT_TAG_GROUPS)
        private set

    var reminderSettings by mutableStateOf(ReminderSettings())
        private set

    // summaries: "day|2026-07-15" -> text, "week|2026-07-15" -> text, etc.
    var summaries by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    // photos: "range|dateKey" -> list of persisted image URI strings
    var photos by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    init {
        viewModelScope.launch { loadAll() }
    }

    // ── Persistence (delegates to the shared repository) ───────────────────────

    /** Re-read everything from disk — used on resume so records logged by the
     *  background check-in flow show up immediately. */
    fun reload() {
        viewModelScope.launch { loadAll() }
    }

    private suspend fun loadAll() {
        records = repo.loadRecords()
        tagGroups = repo.loadTags()
        reminderSettings = repo.loadReminderSettings()
        summaries = repo.loadSummaries()
        photos = repo.loadPhotos()
    }

    private fun saveRecords() {
        val snapshot = records
        viewModelScope.launch { repo.saveRecords(snapshot) }
    }

    private fun saveTags() {
        val snapshot = tagGroups
        viewModelScope.launch { repo.saveTags(snapshot) }
    }

    fun saveReminderSettings(s: ReminderSettings) {
        reminderSettings = s
        viewModelScope.launch {
            repo.saveReminderSettings(s)
            // (re)arm or cancel the background reminder to match the new settings
            com.example.trackday.reminder.ReminderNotifier.ensureChannel(context)
            if (s.enabled) {
                // the foreground service keeps the process alive + arms the alarm
                com.example.trackday.reminder.ReminderService.start(context)
            } else {
                com.example.trackday.reminder.ReminderService.stop(context)
                com.example.trackday.reminder.ReminderScheduler.cancel(context)
                com.example.trackday.reminder.ReminderNotifier.cancel(context)
            }
        }
    }

    // ── Record CRUD ───────────────────────────────────────────────────────────

    fun getRecordsForDate(date: LocalDate): List<TimeRecord> =
        records[date.format(fmt)] ?: emptyList()

    fun addRecord(date: LocalDate, tag: String, cat: String, start: String, end: String, note: String) {
        val key = date.format(fmt)
        val newRecord = TimeRecord(
            id = UUID.randomUUID().toString(),
            tag = tag, cat = cat, start = start, end = end, note = note
        )
        records = records + (key to (records[key] ?: emptyList()) + newRecord)
        saveRecords()
    }

    fun updateRecord(date: LocalDate, updated: TimeRecord) {
        val key = date.format(fmt)
        records = records + (key to (records[key] ?: emptyList()).map {
            if (it.id == updated.id) updated else it
        })
        saveRecords()
    }

    fun deleteRecord(date: LocalDate, recordId: String) {
        val key = date.format(fmt)
        val updated = (records[key] ?: emptyList()).filter { it.id != recordId }
        records = if (updated.isEmpty()) records - key else records + (key to updated)
        saveRecords()
    }

    fun hasRecordsOnDate(date: LocalDate): Boolean =
        records[date.format(fmt)]?.isNotEmpty() == true

    // ── Tag CRUD ──────────────────────────────────────────────────────────────

    fun addTagGroup(name: String, color: Long, icon: String) {
        val g = TagGroup(UUID.randomUUID().toString(), name, color, icon)
        tagGroups = tagGroups + g
        saveTags()
    }

    fun updateTagGroup(id: String, name: String, color: Long, icon: String) {
        tagGroups = tagGroups.map {
            if (it.id == id) it.copy(name = name, color = color, icon = icon) else it
        }
        saveTags()
    }

    fun deleteTagGroup(id: String) {
        tagGroups = tagGroups.filter { it.id != id }
        saveTags()
    }

    fun addChildTag(groupId: String, name: String) {
        tagGroups = tagGroups.map {
            if (it.id == groupId)
                it.copy(children = it.children + ChildTag(UUID.randomUUID().toString(), name))
            else it
        }
        saveTags()
    }

    fun updateChildTag(groupId: String, childId: String, name: String) {
        // capture the old name so we can propagate the rename into records
        val oldName = tagGroups.firstOrNull { it.id == groupId }
            ?.children?.firstOrNull { it.id == childId }?.name
        tagGroups = tagGroups.map { g ->
            if (g.id == groupId)
                g.copy(children = g.children.map { c -> if (c.id == childId) c.copy(name = name) else c })
            else g
        }
        saveTags()
        // keep existing records in sync: rename their tag reference so the
        // timeline / stats screens reflect the new label immediately
        if (oldName != null && oldName != name) {
            records = records.mapValues { (_, list) ->
                list.map { r -> if (r.tag == oldName) r.copy(tag = name) else r }
            }
            saveRecords()
        }
    }

    fun deleteChildTag(groupId: String, childId: String) {
        tagGroups = tagGroups.map { g ->
            if (g.id == groupId)
                g.copy(children = g.children.filter { it.id != childId })
            else g
        }
        saveTags()
    }

    /** All child tags as (name, cat-key) pairs for record editing */
    fun allChildTags(): List<Pair<String, String>> = tagGroups.flatMap { g ->
        g.children.map { c -> c.name to g.name.toCatKey() }
    }

    /** Cat key from group name (fallback to "life") */
    private fun String.toCatKey(): String = when (this) {
        "学习" -> "learn"
        "休息" -> "rest"
        "工作" -> "work"
        else -> "life"
    }

    // Category from tag name
    fun catForTag(tagName: String): String {
        tagGroups.forEach { g ->
            if (g.children.any { it.name == tagName }) return g.name.toCatKey()
        }
        return "life"
    }

    /** The owning tag group for a record's tag, or null if orphaned. */
    fun groupForTag(tagName: String): TagGroup? =
        tagGroups.firstOrNull { g -> g.children.any { it.name == tagName } }

    /**
     * Resolve the display color for a record. Prefers the live tag group's
     * color (so editing a big tag's colour updates records + stats), falling
     * back to the category default for orphaned records.
     */
    fun colorForTag(tagName: String, fallbackCat: String): Long =
        groupForTag(tagName)?.color ?: catColor(fallbackCat)

    /** Live category key for a record (follows the tag group, not the snapshot). */
    fun liveCatForTag(tagName: String, fallbackCat: String): String =
        groupForTag(tagName)?.name?.toCatKey() ?: fallbackCat

    // ── Summaries ─────────────────────────────────────────────────────────────

    fun getSummary(range: String, dateKey: String): String? = summaries["$range|$dateKey"]

    fun saveSummaryText(range: String, dateKey: String, text: String) {
        summaries = summaries + ("$range|$dateKey" to text)
        val snapshot = summaries
        viewModelScope.launch { repo.saveSummaries(snapshot) }
    }

    // ── Summary photos ─────────────────────────────────────────────────────────

    fun getPhotos(range: String, dateKey: String): List<String> =
        photos["$range|$dateKey"] ?: emptyList()

    fun addPhoto(range: String, dateKey: String, uri: String) {
        val key = "$range|$dateKey"
        photos = photos + (key to (photos[key] ?: emptyList()) + uri)
        val snapshot = photos
        viewModelScope.launch { repo.savePhotos(snapshot) }
    }

    /**
     * Import a picked image into app storage, then add it. onDone is invoked on
     * the main thread with success/failure so the UI can toast appropriately.
     */
    fun importAndAddPhoto(range: String, dateKey: String, sourceUri: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val stored = repo.importPhoto(sourceUri)
            if (stored != null) {
                val key = "$range|$dateKey"
                photos = photos + (key to (photos[key] ?: emptyList()) + stored)
                repo.savePhotos(photos)
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun removePhoto(range: String, dateKey: String, uri: String) {
        val key = "$range|$dateKey"
        val updated = (photos[key] ?: emptyList()).filter { it != uri }
        photos = if (updated.isEmpty()) photos - key else photos + (key to updated)
        val snapshot = photos
        viewModelScope.launch { repo.savePhotos(snapshot) }
    }

    // ── Aggregation helpers ───────────────────────────────────────────────────

    data class AggResult(
        val totalMinutes: Int,
        val count: Int,
        val activeDays: Int,
        val byCategory: Map<String, Int>   // cat key -> minutes
    )

    fun aggregateDays(keys: List<String>): AggResult {
        var total = 0; var count = 0; var active = 0
        val by = mutableMapOf<String, Int>()
        keys.forEach { k ->
            val list = records[k] ?: return@forEach
            if (list.isNotEmpty()) active++
            list.forEach { r ->
                val m = r.durationMinutes
                total += m; count++
                by[r.cat] = (by[r.cat] ?: 0) + m
            }
        }
        return AggResult(total, count, active, by)
    }

    // ── Group-based aggregation (统计按大标签汇总) ───────────────────────────────
    // Sums minutes per live tag group so renames / recolors on the 标签 screen
    // flow straight through to the 统计 charts. Records whose tag no longer
    // maps to any group fall into "其他".

    data class GroupSlice(
        val groupId: String,
        val name: String,
        val color: Long,
        val minutes: Int
    )

    fun aggregateByGroup(keys: List<String>): List<GroupSlice> {
        val minutesByGroup = linkedMapOf<String, Int>()
        keys.forEach { k ->
            (records[k] ?: emptyList()).forEach { r ->
                val gid = groupForTag(r.tag)?.id ?: "__other__"
                minutesByGroup[gid] = (minutesByGroup[gid] ?: 0) + r.durationMinutes
            }
        }
        return minutesByGroup.mapNotNull { (gid, mins) ->
            if (mins <= 0) return@mapNotNull null
            if (gid == "__other__") {
                GroupSlice("__other__", "其他", catColor("life"), mins)
            } else {
                val g = tagGroups.firstOrNull { it.id == gid } ?: return@mapNotNull null
                GroupSlice(g.id, g.name, g.color, mins)
            }
        }.sortedByDescending { it.minutes }
    }

    fun dayTotal(date: LocalDate): Int = aggregateDays(listOf(date.format(fmt))).totalMinutes

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackdayViewModel(context.applicationContext) as T
    }
}
