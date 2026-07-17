package com.example.trackday.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.trackday.data.ReminderSettings
import java.util.Calendar

/**
 * Schedules check-in alarms via AlarmManager.
 *
 * Uses [AlarmManager.setAlarmClock] for the main interval alarm: unlike
 * setExactAndAllowWhileIdle, alarm-clock alarms are exempt from Doze batching,
 * so they fire on time even when the screen has been off for a long time.
 *
 * A second, shorter alarm ([ACTION_AUTO_INHERIT]) implements the 30-second
 * "no response → inherit last tag" timeout entirely in the background, so it
 * runs whether or not the user ever looks at the popup.
 */
object ReminderScheduler {

    const val ACTION_ALARM = "com.example.trackday.ACTION_CHECK_IN_ALARM"
    const val ACTION_AUTO_INHERIT = "com.example.trackday.ACTION_AUTO_INHERIT"
    private const val REQUEST_CODE = 4021
    private const val REQUEST_CODE_AUTO = 4022

    /** Seconds a popup waits for the user before auto-inheriting the last tag. */
    const val AUTO_INHERIT_SECONDS = 30

    /**
     * Arm the next interval alarm.
     *
     * Crucially this does NOT move an already-scheduled future alarm: if a valid
     * trigger time is already stored and still in the future, it re-arms the
     * SAME time. Otherwise (first run, alarm already elapsed, or [force] = true)
     * it computes a fresh "now + interval". This is what stops the countdown
     * from resetting — and stops the 20-min alarm from being pushed back every
     * time the app is opened.
     */
    fun reschedule(context: Context, settings: ReminderSettings, force: Boolean = false) {
        cancel(context)
        if (!settings.enabled) {
            ReminderPrefs.clearNextTrigger(context)
            return
        }
        val now = System.currentTimeMillis()
        val existing = ReminderPrefs.getNextTrigger(context)

        val triggerAt = if (!force && existing > now && withinSnoozeOk(settings, existing, now)) {
            // keep the previously fixed target so the countdown is stable
            existing
        } else {
            val base = if (settings.snoozeUntil > now) settings.snoozeUntil else now
            nextTriggerMillis(settings, base)
        }
        ReminderPrefs.setNextTrigger(context, triggerAt)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        if (canExact) {
            // Alarm-clock alarms are treated like a user alarm: NOT deferred by
            // Doze, so this fires precisely even with the screen off.
            val showIntent = PendingIntent.getActivity(
                context, 9001,
                Intent(context, com.example.trackday.CheckInActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
            am.setAlarmClock(info, alarmPendingIntent(context))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmPendingIntent(context))
        }
    }

    /** An existing target is only reusable if it doesn't fall inside a snooze. */
    private fun withinSnoozeOk(settings: ReminderSettings, existing: Long, now: Long): Boolean {
        return settings.snoozeUntil <= now || existing >= settings.snoozeUntil
    }

    /** Force a brand-new "now + interval" alarm (used after firing / restart / manual). */
    fun restart(context: Context, settings: ReminderSettings) = reschedule(context, settings, force = true)

    /** Schedule the background auto-inherit timeout [AUTO_INHERIT_SECONDS] from now. */
    fun scheduleAutoInherit(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + AUTO_INHERIT_SECONDS * 1000L
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, autoInheritPendingIntent(context))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, autoInheritPendingIntent(context))
        }
    }

    fun cancelAutoInherit(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(autoInheritPendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_ALARM)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun autoInheritPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_AUTO_INHERIT)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE_AUTO, intent, flags)
    }

    /**
     * Next fire time = now + interval, but snapped into the active window.
     * If it lands after the window end, push to the window start of next day.
     *
     * Keeps second precision (does NOT zero the seconds): a 20-min interval
     * fires exactly 20:00 from now, so the countdown is accurate to the second.
     */
    fun nextTriggerMillis(settings: ReminderSettings, nowMillis: Long): Long {
        val startMin = settings.startHour * 60 + settings.startMinute
        val endMin = settings.endHour * 60 + settings.endMinute

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.add(Calendar.MINUTE, settings.intervalMinutes)
        cal.set(Calendar.MILLISECOND, 0)

        val candidateMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        if (startMin <= endMin) {
            when {
                candidateMin < startMin -> setTime(cal, startMin)
                candidateMin > endMin -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    setTime(cal, startMin)
                }
            }
        } else {
            val insideOvernight = candidateMin >= startMin || candidateMin <= endMin
            if (!insideOvernight) setTime(cal, startMin)
        }
        return cal.timeInMillis
    }

    private fun setTime(cal: Calendar, minuteOfDay: Int) {
        cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        cal.set(Calendar.MINUTE, minuteOfDay % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    /** True if [nowMillis] is within the active reminder window. */
    fun isWithinWindow(settings: ReminderSettings, nowMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMin = settings.startHour * 60 + settings.startMinute
        val endMin = settings.endHour * 60 + settings.endMinute
        return if (startMin <= endMin) nowMin in startMin..endMin
        else nowMin >= startMin || nowMin <= endMin
    }
}
