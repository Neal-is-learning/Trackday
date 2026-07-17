package com.example.trackday.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.trackday.MainActivity
import com.example.trackday.R
import com.example.trackday.data.TrackdayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A long-running foreground service that keeps the process alive so the
 * check-in alarm keeps firing even after the app is swiped away or the screen
 * is off. Shows a low-key persistent "Trackday 运行中" notification (required by
 * Android for foreground services).
 */
class ReminderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(ONGOING_ID, buildOngoingNotification())
                // (re)arm the alarm loop
                scope.launch {
                    val settings = TrackdayRepository.get(applicationContext).loadReminderSettings()
                    if (settings.enabled) {
                        ReminderNotifier.ensureChannel(applicationContext)
                        ReminderScheduler.reschedule(applicationContext, settings)
                    }
                }
            }
        }
        // STICKY so the system restarts the service (and our保活) if killed
        return START_STICKY
    }

    private fun buildOngoingNotification(): Notification {
        ensureOngoingChannel(this)
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Trackday 正在记录时间")
            .setContentText("到点会问你现在在做什么")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.trackday.STOP_REMINDER_SERVICE"
        private const val ONGOING_ID = 7100
        private const val ONGOING_CHANNEL_ID = "trackday_ongoing"

        fun ensureOngoingChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ONGOING_CHANNEL_ID,
                    "后台运行",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "保持定时提醒在后台运行"
                    setShowBadge(false)
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ReminderService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
