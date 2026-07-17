package com.example.trackday.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.pad2
import com.example.trackday.ui.theme.LocalTdColors
import kotlinx.coroutines.delay

/**
 * Shown when the user taps the bell on the timeline. Instead of forcing a
 * check-in, it just tells them how long until the next reminder, counting down
 * to the second. Purely informational — dismiss by tapping anywhere.
 */
@Composable
fun NextReminderCountdown(
    visible: Boolean,
    enabled: Boolean,
    nextAtProvider: () -> Long?,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    val td = LocalTdColors.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC12403A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // recompute remaining seconds every second
            var remaining by remember { mutableStateOf(computeRemaining(nextAtProvider())) }
            LaunchedEffect(visible, enabled) {
                if (!visible) return@LaunchedEffect
                while (true) {
                    remaining = computeRemaining(nextAtProvider())
                    delay(1000)
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(240), initialScale = 0.9f) + fadeIn(tween(240)),
                exit = scaleOut(tween(160)) + fadeOut(tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(td.surface, Color(0xFFF2FAF6))))
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(td.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Timer, null, tint = td.accent, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(18.dp))

                    if (!enabled) {
                        Text("提醒已关闭", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = td.fg)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "在「提醒」页打开后台定时提醒",
                            fontSize = 13.sp, color = td.muted
                        )
                    } else {
                        Text("距离下次提醒", fontSize = 14.sp, color = td.muted)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            formatCountdown(remaining),
                            fontSize = 44.sp, fontWeight = FontWeight.SemiBold, color = td.fg
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "到点会问你现在在做什么",
                            fontSize = 13.sp, color = td.muted
                        )
                        Spacer(Modifier.height(20.dp))
                        // restart the countdown from the full interval
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(td.accent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onRestart()
                                    // reflect immediately in the countdown
                                    remaining = computeRemaining(nextAtProvider())
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("重新开始倒计时", fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "点击任意处关闭",
                        fontSize = 12.sp, color = td.muted.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun computeRemaining(nextAt: Long?): Long {
    if (nextAt == null) return 0
    val millisLeft = nextAt - System.currentTimeMillis()
    if (millisLeft <= 0) return 0
    // round UP: a 20-min alarm shows "20:00" at the start, not "19:59"
    return ((millisLeft + 999) / 1000)
}

/** seconds → "HH:MM:SS" or "MM:SS" */
private fun formatCountdown(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "${pad2(h.toInt())}:${pad2(m.toInt())}:${pad2(s.toInt())}"
    else "${pad2(m.toInt())}:${pad2(s.toInt())}"
}
