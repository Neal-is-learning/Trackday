package com.example.trackday.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.ui.theme.LocalTdColors

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    TIMELINE("timeline", "记录", Icons.AutoMirrored.Rounded.List),
    STATS("stats", "统计", Icons.Rounded.BarChart),
    TAGS("tags", "标签", Icons.Rounded.Sell),
    REMINDERS("reminders", "提醒", Icons.Rounded.Notifications)
}

@Composable
fun BottomNav(current: String, onNavigate: (String) -> Unit) {
    val td = LocalTdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(td.surface.copy(alpha = 0.96f))
            .border(width = 1.dp, color = td.border)
            // lift the whole bar above the system gesture / navigation bar so
            // the icons + labels are never clipped
            .navigationBarsPadding()
            .height(76.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dest.entries.forEach { dest ->
            val active = current == dest.route
            val bg by animateColorAsState(
                if (active) td.accentSoft else td.accentSoft.copy(alpha = 0.4f),
                tween(150), label = "navbg"
            )
            val fg = if (active) td.accentInk else td.accentInk.copy(alpha = 0.75f)
            val iconTint = if (active) td.accent else td.accent.copy(alpha = 0.6f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .then(
                        if (active)
                            Modifier.border(1.dp, td.accent.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                        else Modifier.border(1.dp, td.accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (!active) onNavigate(dest.route) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(dest.icon, dest.label, tint = iconTint, modifier = Modifier.size(22.dp))
                    Text(
                        dest.label,
                        color = fg,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
