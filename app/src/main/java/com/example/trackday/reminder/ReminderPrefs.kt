package com.example.trackday.reminder

import android.content.Context

/**
 * Tiny synchronous store for the single volatile value the countdown UI needs
 * to read every second: the fixed epoch-millis at which the next reminder is
 * scheduled. Kept in SharedPreferences (not DataStore) so it can be read
 * synchronously without a coroutine.
 */
object ReminderPrefs {
    private const val FILE = "trackday_reminder_prefs"
    private const val KEY_NEXT_TRIGGER = "next_trigger_at"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setNextTrigger(context: Context, epochMillis: Long) {
        prefs(context).edit().putLong(KEY_NEXT_TRIGGER, epochMillis).apply()
    }

    /** 0 if no reminder is scheduled. */
    fun getNextTrigger(context: Context): Long =
        prefs(context).getLong(KEY_NEXT_TRIGGER, 0L)

    fun clearNextTrigger(context: Context) {
        prefs(context).edit().remove(KEY_NEXT_TRIGGER).apply()
    }
}
