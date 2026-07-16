package com.example.trackday.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.trackday.data.TrackdayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms the reminder alarm after a device reboot or app update, since
 * AlarmManager alarms do not survive a restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = TrackdayRepository.get(appContext).loadReminderSettings()
                if (settings.enabled) {
                    ReminderNotifier.ensureChannel(appContext)
                    // restart the foreground service, which re-arms the alarm loop
                    ReminderService.start(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
