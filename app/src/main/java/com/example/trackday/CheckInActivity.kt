package com.example.trackday

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.trackday.data.ReminderSettings
import com.example.trackday.data.TagGroup
import com.example.trackday.data.TrackdayRepository
import com.example.trackday.reminder.ReminderNotifier
import com.example.trackday.reminder.ReminderScheduler
import com.example.trackday.ui.screens.CheckInScreen
import com.example.trackday.ui.theme.TrackdayTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Standalone full-screen check-in screen launched by the reminder's
 * full-screen intent. Shows over the lock screen like an alarm, writes the
 * record straight to the repository, then finishes.
 */
class CheckInActivity : ComponentActivity() {

    private val repo by lazy { TrackdayRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        enableEdgeToEdge()

        // The user is now looking at the popup, so the background 30s
        // auto-inherit alarm must NOT also fire — this screen owns the outcome.
        ReminderScheduler.cancelAutoInherit(this)

        setContent {
            TrackdayTheme {
                var tags by remember { mutableStateOf<List<TagGroup>>(emptyList()) }
                var settings by remember { mutableStateOf(ReminderSettings()) }
                var lastTag by remember { mutableStateOf<String?>(null) }
                // the slot recorded when the reminder fired ("HH:mm" start/end)
                var slotStart by remember { mutableStateOf<String?>(null) }
                var slotEnd by remember { mutableStateOf<String?>(null) }
                var ready by remember { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    tags = repo.loadTags()
                    settings = repo.loadReminderSettings()
                    lastTag = repo.lastTag()
                    val pendingSlot = repo.getPendingCheckIn()
                    if (pendingSlot != null) {
                        slotStart = pendingSlot.second
                        slotEnd = pendingSlot.third
                    }
                    ready = true
                }

                fun logAndFinish(tag: String) {
                    lifecycleScope.launch {
                        val cat = repo.catForTag(tag)
                        val now = java.time.LocalTime.now().withSecond(0).withNano(0)
                        val fallbackStart = now.minusMinutes(settings.intervalMinutes.toLong())
                        repo.appendRecord(
                            date = LocalDate.now(),
                            tag = tag,
                            cat = cat,
                            start = slotStart ?: "%02d:%02d".format(fallbackStart.hour, fallbackStart.minute),
                            end = slotEnd ?: "%02d:%02d".format(now.hour, now.minute),
                            note = ""
                        )
                        repo.clearPendingCheckIn()
                        ReminderNotifier.cancel(this@CheckInActivity)
                        finish()
                    }
                }

                // Skip: just drop this check-in; the normal schedule continues.
                fun dismiss() {
                    lifecycleScope.launch {
                        repo.clearPendingCheckIn()
                        ReminderNotifier.cancel(this@CheckInActivity)
                        finish()
                    }
                }

                // Snooze: suppress reminders for snoozeMinutes, then resume.
                fun snooze() {
                    lifecycleScope.launch {
                        repo.clearPendingCheckIn()
                        ReminderNotifier.cancel(this@CheckInActivity)
                        ReminderScheduler.snoozeFor(this@CheckInActivity, settings.snoozeMinutes)
                        finish()
                    }
                }

                if (ready) {
                    CheckInScreen(
                        tagGroups = tags,
                        intervalMinutes = settings.intervalMinutes,
                        snoozeMinutes = settings.snoozeMinutes,
                        autoInheritTag = lastTag,
                        autoDismissSeconds = ReminderScheduler.AUTO_INHERIT_SECONDS,
                        onLog = { tag -> logAndFinish(tag) },
                        onSnooze = { snooze() },
                        onSkip = { dismiss() }
                    )
                }
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
