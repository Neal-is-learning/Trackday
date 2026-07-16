package com.example.trackday.data

import kotlinx.serialization.Serializable

@Serializable
data class TimeRecord(
    val id: String,
    val tag: String,
    val cat: String,       // "learn" | "rest" | "work" | "life"
    val start: String,     // "HH:mm"
    val end: String,       // "HH:mm"
    val note: String = ""
) {
    val startMinutes: Int get() {
        val p = start.split(":")
        return p[0].toInt() * 60 + p[1].toInt()
    }
    val endMinutes: Int get() {
        val p = end.split(":")
        return p[0].toInt() * 60 + p[1].toInt()
    }
    val durationMinutes: Int get() {
        val m = endMinutes - startMinutes
        return if (m < 0) m + 24 * 60 else m
    }
}

@Serializable
data class TagGroup(
    val id: String,
    val name: String,
    val color: Long,       // ARGB packed (0xFFrrggbb)
    val icon: String,      // icon key
    val children: List<ChildTag> = emptyList()
)

@Serializable
data class ChildTag(
    val id: String,
    val name: String
)

@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 30,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val dndEnabled: Boolean = true,
    val sysNotifyEnabled: Boolean = true,
    val popupEnabled: Boolean = true,
    val snoozeMinutes: Int = 60
)

// ── Seed data ─────────────────────────────────────────────────────────────────
// No pre-imported time records: the timeline starts empty and only fills up
// from real check-ins. (Tag groups below are kept so users have tags to pick.)

val DEFAULT_RECORDS: Map<String, List<TimeRecord>> = emptyMap()

val DEFAULT_TAG_GROUPS: List<TagGroup> = listOf(
    TagGroup("g1", "学习", 0xFF6B9BE8.toLong(), "book",
        listOf(ChildTag("c1","看书"), ChildTag("c2","看网课"), ChildTag("c3","写代码"))),
    TagGroup("g2", "休息", 0xFFB5C27A.toLong(), "sun",
        listOf(ChildTag("c4","散步"), ChildTag("c5","小憩"))),
    TagGroup("g3", "工作", 0xFFD4845A.toLong(), "bag",
        listOf(ChildTag("c6","开会"))),
    TagGroup("g4", "生活", 0xFFBF7AB5.toLong(), "globe",
        listOf(ChildTag("c7","吃饭"), ChildTag("c8","通勤")))
)

// ── Tag editor palettes ───────────────────────────────────────────────────────

val TAG_COLORS: List<Long> = listOf(
    0xFF6B9BE8.toLong(), // blue
    0xFFB5C27A.toLong(), // olive
    0xFFD4845A.toLong(), // orange
    0xFFBF7AB5.toLong(), // pink
    0xFF2DAB80.toLong(), // teal (accent)
    0xFF9B72D4.toLong(), // purple
    0xFF6FBF7A.toLong(), // green
    0xFFD4A05A.toLong(), // amber
    0xFFD46A6A.toLong(), // red
    0xFF6A82D4.toLong(), // indigo
    0xFF5AB5C4.toLong(), // cyan
    0xFF808792.toLong()  // grey
)

val ICON_KEYS: List<String> = listOf(
    "book", "sun", "bag", "globe", "heart", "star",
    "run", "cup", "pen", "music", "cart", "dumbbell"
)

// ── Color helpers (cat key → UI color int) ────────────────────────────────────

fun catColor(cat: String): Long = when (cat) {
    "learn" -> 0xFF6B9BE8.toLong()
    "rest"  -> 0xFFB5C27A.toLong()
    "work"  -> 0xFFD4845A.toLong()
    else    -> 0xFFBF7AB5.toLong()
}

fun fmtDuration(minutes: Int): String {
    if (minutes <= 0) return "0 分"
    val h = minutes / 60; val m = minutes % 60
    return when {
        h == 0 -> "$m 分"
        m == 0 -> "$h 小时"
        else   -> "${h}h${m}m"
    }
}

fun fmtDurationLong(minutes: Int): String {
    if (minutes <= 0) return "0 分"
    val h = minutes / 60; val m = minutes % 60
    return when {
        h == 0 -> "$m 分"
        m == 0 -> "$h 小时"
        else   -> "$h 小时 $m 分"
    }
}

fun pad2(n: Int) = if (n < 10) "0$n" else "$n"
