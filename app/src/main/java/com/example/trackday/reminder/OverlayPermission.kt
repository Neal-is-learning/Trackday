package com.example.trackday.reminder

import android.content.Context
import android.provider.Settings

/**
 * Helper around SYSTEM_ALERT_WINDOW ("显示在其他应用上层"). With this permission
 * the app can launch the full-screen check-in from the background at any time —
 * not just over the lock screen — which is what lets the popup appear even
 * while the user is in another app.
 */
object OverlayPermission {
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
}
