package com.example.trackday.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.trackday.data.TagGroup
import com.example.trackday.data.pad2
import com.example.trackday.ui.theme.LocalTdColors
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Stateless full-screen check-in UI — the "全屏打卡弹窗" core flow, launched by
 * the reminder's full-screen intent via [com.example.trackday.CheckInActivity].
 *
 * Callbacks: [onLog] gives the chosen tag (record writing is the caller's job),
 * [onSnooze] and [onSkip] just dismiss.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(
    tagGroups: List<TagGroup>,
    intervalMinutes: Int,
    snoozeMinutes: Int,
    onLog: (tag: String) -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    animateIn: Boolean = false,
    // When set, an unanswered popup auto-resolves after [autoDismissSeconds]:
    // logs [autoInheritTag] if non-null, otherwise skips without recording.
    autoInheritTag: String? = null,
    autoDismissSeconds: Int = 0
) {
    val td = LocalTdColors.current

    val slot = remember(intervalMinutes) {
        val end = LocalTime.now().withSecond(0).withNano(0)
        val start = end.minusMinutes(intervalMinutes.toLong())
        "${pad2(start.hour)}:${pad2(start.minute)}" to "${pad2(end.hour)}:${pad2(end.minute)}"
    }

    val allTags = tagGroups.flatMap { g -> g.children.map { it.name } }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var customMode by remember { mutableStateOf(false) }
    var customTag by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = LocalTime.now()
            delay(1000)
        }
    }

    // Auto-dismiss countdown: any user interaction cancels it.
    var interacted by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(autoDismissSeconds) }
    if (autoDismissSeconds > 0) {
        LaunchedEffect(interacted) {
            if (interacted) return@LaunchedEffect
            remaining = autoDismissSeconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
            }
            // time's up with no interaction: inherit last tag, or skip if none
            if (autoInheritTag != null) onLog(autoInheritTag) else onSkip()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF12403A), Color(0xFF1A1D21))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        val content: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive, null,
                        tint = Color.White, modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "${pad2(clock.hour)}:${pad2(clock.minute)}",
                    color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "现在在做什么？",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "记录这段时间 ${slot.first} – ${slot.second}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )

                // countdown hint (only while auto-dismiss is armed & untouched)
                if (autoDismissSeconds > 0 && !interacted && remaining > 0) {
                    Text(
                        if (autoInheritTag != null)
                            "${remaining}s 后自动记为「$autoInheritTag」"
                        else
                            "${remaining}s 后无记录自动关闭",
                        color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(td.surface)
                        .padding(18.dp)
                ) {
                    Text("选择标签", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = td.muted, modifier = Modifier.padding(bottom = 10.dp))
                    PopupTagPicker(
                        tags = allTags,
                        selected = selectedTag,
                        customMode = customMode,
                        onSelect = { selectedTag = it; customMode = false; error = null; interacted = true },
                        onCustom = { customMode = true; selectedTag = null; error = null; interacted = true }
                    )
                    if (customMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customTag,
                            onValueChange = { if (it.length <= 12) customTag = it; interacted = true },
                            placeholder = { Text("自定义，将归入「其他」", color = td.muted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = td.accent,
                                unfocusedBorderColor = td.border,
                                focusedContainerColor = td.surface,
                                unfocusedContainerColor = td.bg
                            )
                        )
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = td.danger, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    SheetButton("记下这段时间", Modifier.fillMaxWidth(), primary = true) {
                        val tag = if (customMode) customTag.trim() else selectedTag
                        if (tag.isNullOrBlank()) {
                            error = if (customMode) "请输入自定义标签" else "请先选择一个标签"
                            return@SheetButton
                        }
                        onLog(tag)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SheetButton("暂停 $snoozeMinutes 分", Modifier.weight(1f), primary = false) {
                            onSnooze()
                        }
                        SheetButton("跳过", Modifier.weight(1f), primary = false, onClick = onSkip)
                    }
                }
            }
        }

        if (animateIn) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(tween(260), initialScale = 0.9f) + fadeIn(tween(260)),
                exit = scaleOut(tween(180)) + fadeOut(tween(180))
            ) { content() }
        } else {
            content()
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PopupTagPicker(
    tags: List<String>,
    selected: String?,
    customMode: Boolean,
    onSelect: (String) -> Unit,
    onCustom: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val on = !customMode && tag == selected
            PopupChip(tag, on) { onSelect(tag) }
        }
        PopupChip("＋自定义", customMode) { onCustom() }
    }
}

@Composable
private fun PopupChip(text: String, on: Boolean, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) td.accentSoft else td.surface)
            .border(1.5.dp, if (on) td.accent else td.border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (on) td.accentInk else td.fg)
    }
}
