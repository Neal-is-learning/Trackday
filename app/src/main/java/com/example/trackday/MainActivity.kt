package com.example.trackday

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.trackday.data.TrackdayViewModel
import com.example.trackday.ui.AppUiState
import com.example.trackday.ui.BottomNav
import com.example.trackday.ui.Dest
import com.example.trackday.ui.screens.CheckInPopup
import com.example.trackday.ui.screens.RemindersScreen
import com.example.trackday.ui.screens.StatsScreen
import com.example.trackday.ui.screens.TagsScreen
import com.example.trackday.ui.screens.TimelineScreen
import com.example.trackday.ui.common.TrackToast
import com.example.trackday.ui.common.rememberToastState
import com.example.trackday.ui.theme.LocalTdColors
import com.example.trackday.ui.theme.TrackdayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TrackdayViewModel by viewModels {
        TrackdayViewModel.Factory(applicationContext)
    }

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.trackday.reminder.ReminderNotifier.ensureChannel(this)
        maybeRequestNotificationPermission()
        maybeRequestExactAlarmPermission()
        maybeRequestOverlayPermission()

        setContent {
            TrackdayTheme {
                TrackdayApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // pick up records logged via the background check-in flow + date rollover
        viewModel.refreshToday()
        viewModel.reload()
        // ensure the background service is running if reminders are enabled
        val s = viewModel.reminderSettings
        if (s.enabled) {
            com.example.trackday.reminder.ReminderService.start(this)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun maybeRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(android.app.AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:$packageName"))
                    )
                }
            }
        }
    }

    // "显示在其他应用上层" — lets the reminder pop the full-screen check-in from
    // the background even while another app is in the foreground.
    private fun maybeRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }
}

@Composable
fun TrackdayApp(vm: TrackdayViewModel) {
    val td = LocalTdColors.current
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Dest.TIMELINE.route

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(td.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // real system status bar (time / wifi / battery drawn by the OS)
                .statusBarsPadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Dest.TIMELINE.route,
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End, tween(220)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End, tween(220)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Dest.TIMELINE.route) { TimelineScreen(vm) }
                    composable(Dest.STATS.route) { StatsScreen(vm) }
                    composable(Dest.TAGS.route) { TagsScreen(vm) }
                    composable(Dest.REMINDERS.route) { RemindersScreen(vm) }
                }
            }
            BottomNav(current = current) { route ->
                navController.navigate(route) {
                    popUpTo(Dest.TIMELINE.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        // Full-screen check-in popup — hosted at the app root so it covers the
        // bottom nav like a real alarm. Triggered from the timeline bell.
        val popupToast = rememberToastState()
        CheckInPopup(
            visible = AppUiState.checkInVisible,
            vm = vm,
            onDismiss = { AppUiState.closeCheckIn() },
            onLogged = { tag ->
                AppUiState.closeCheckIn()
                popupToast.show("已记录「$tag」")
            },
            onSnoozed = { min ->
                AppUiState.closeCheckIn()
                // actually suppress reminders for the snooze window
                vm.snoozeReminders(min)
                popupToast.show("已暂停 $min 分钟，期间不再提醒")
            }
        )
        TrackToast(popupToast)
    }
}
