package com.example.trackday.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.trackday.CheckInActivity
import com.example.trackday.data.TrackdayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Handles both reminder alarms:
 *
 * ACTION_ALARM (interval) → mark a pending check-in, pop the full-screen
 * check-in (directly if we can draw over other apps, otherwise via a
 * full-screen-intent notification), arm the 30s auto-inherit timeout, and
 * chain the next interval.
 *
 * ACTION_AUTO_INHERIT (30s later) → if the check-in is still unanswered,
 * auto-log the previous tag; if there is no previous tag, just clear it
 * without recording. Runs in the background regardless of user interaction.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ReminderScheduler.ACTION_ALARM -> handleInterval(appContext)
            ReminderScheduler.ACTION_AUTO_INHERIT -> handleAutoInherit(appContext)
        }
    }

    private fun handleInterval(appContext: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TrackdayRepository.get(appContext)
                val settings = repo.loadReminderSettings()
                val now = System.currentTimeMillis()
                val snoozed = settings.snoozeUntil > now
                if (settings.enabled && !snoozed &&
                    ReminderScheduler.isWithinWindow(settings, now)
                ) {
                    // record the slot as a pending check-in
                    val end = LocalTime.now().withSecond(0).withNano(0)
                    val start = end.minusMinutes(settings.intervalMinutes.toLong())
                    val startStr = "%02d:%02d".format(start.hour, start.minute)
                    val endStr = "%02d:%02d".format(end.hour, end.minute)
                    repo.setPendingCheckIn(LocalDate.now(), startStr, endStr)

                    if (settings.popupEnabled) {
                        launchFullScreen(appContext)
                    }
                    // notification: OS-sanctioned path for lock screen / fallback
                    ReminderNotifier.showCheckIn(
                        appContext,
                        popupEnabled = settings.popupEnabled,
                        sysNotifyEnabled = settings.sysNotifyEnabled
                    )
                    // background 30s auto-inherit timeout, independent of the UI
                    ReminderScheduler.scheduleAutoInherit(appContext)
                }
                // chain next interval
                ReminderScheduler.reschedule(appContext, settings)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleAutoInherit(appContext: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TrackdayRepository.get(appContext)
                val slot = repo.getPendingCheckIn() ?: return@launch // already answered
                val lastTag = repo.lastTag()
                if (lastTag != null) {
                    // inherit the previous tag without disturbing the user
                    val cat = repo.catForTag(lastTag)
                    repo.appendRecord(slot.first, lastTag, cat, slot.second, slot.third, "")
                }
                // whether or not we recorded, the check-in is resolved
                repo.clearPendingCheckIn()
                ReminderNotifier.cancel(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    private fun launchFullScreen(appContext: Context) {
        // Direct start works from the background only if we can draw over other
        // apps (SYSTEM_ALERT_WINDOW). Guarded so we never crash if not granted.
        if (!OverlayPermission.canDrawOverlays(appContext)) return
        runCatching {
            appContext.startActivity(
                Intent(appContext, CheckInActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }
}
