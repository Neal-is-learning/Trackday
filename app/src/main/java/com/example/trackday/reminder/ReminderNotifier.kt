package com.example.trackday.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.trackday.CheckInActivity
import com.example.trackday.R

/**
 * Builds and posts the check-in reminder notification. When the popup channel
 * is enabled it attaches a full-screen intent so the check-in screen pops over
 * the lock screen like an alarm; otherwise it stays a normal heads-up notice.
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "trackday_checkin"
    const val NOTIFICATION_ID = 7001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "打卡提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "定时提醒你记录当前在做什么"
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun showCheckIn(context: Context, popupEnabled: Boolean, sysNotifyEnabled: Boolean) {
        if (!sysNotifyEnabled && !popupEnabled) return
        ensureChannel(context)

        val contentIntent = Intent(context, CheckInActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(context, 0, contentIntent, piFlags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("现在在做什么？")
            .setContentText("点一下记录这段时间")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)

        // full-screen intent: the OS-sanctioned way to pop the whole screen over
        // the lock screen. The AlarmReceiver separately does a direct
        // startActivity (needs SYSTEM_ALERT_WINDOW) to also cover the case where
        // the screen is on and the user is inside another app.
        if (popupEnabled) {
            builder.setFullScreenIntent(contentPi, true)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
