package com.example.trackday.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.*
import com.example.trackday.ui.common.*
import com.example.trackday.ui.theme.LocalTdColors

private val INTERVAL_CHIPS = listOf(15, 20, 30, 45, 60)

@Composable
fun RemindersScreen(vm: TrackdayViewModel) {
    val td = LocalTdColors.current
    val toast = rememberToastState()
    val s = vm.reminderSettings

    fun update(new: ReminderSettings) = vm.saveReminderSettings(new)

    var timePickerOpen by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }
    var numOpen by remember { mutableStateOf(false) }
    var numMode by remember { mutableStateOf("interval") }

    val intervalMatchesChip = INTERVAL_CHIPS.contains(s.intervalMinutes)

    Box(modifier = Modifier.fillMaxSize().background(td.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp)) {
                Text("提醒设置", style = MaterialTheme.typography.headlineLarge, color = td.fg)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 88.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFE6F5EE), td.surface)))
                        .border(1.dp, Color(0xFFCFE9DF), RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("后台定时提醒", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            letterSpacing = 0.06.sp, color = td.muted)
                        Text("每 ${s.intervalMinutes} 分", style = MaterialTheme.typography.displayLarge,
                            color = td.fg, fontSize = 34.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            "每日 ${fmtHM(s.startHour, s.startMinute)} – ${fmtHM(s.endHour, s.endMinute)} 生效 · 后台运行",
                            fontSize = 13.sp, color = td.muted, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    TrackSwitch(s.enabled, {
                        update(s.copy(enabled = it))
                        toast.show(if (it) "后台提醒已开启" else "后台提醒已关闭")
                    })
                }
                Spacer(Modifier.height(12.dp))

                Panel(title = "提醒间隔") {
                    FlowChips(
                        chips = INTERVAL_CHIPS.map { it to "$it 分" } + (-1 to "自定义"),
                        selected = if (intervalMatchesChip) s.intervalMinutes else -1,
                        onSelect = { v ->
                            if (v == -1) { numMode = "interval"; numOpen = true }
                            else { update(s.copy(intervalMinutes = v)); toast.show("间隔已设为 $v 分钟") }
                        }
                    )
                    RowItem(title = "自定义间隔", desc = "任意分钟数，精确到分", divider = true) {
                        ValueButton("${s.intervalMinutes} 分") { numMode = "interval"; numOpen = true }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Panel(title = "生效时段（24 小时）") {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReminderTimeBox("开始", fmtHM(s.startHour, s.startMinute), Modifier.weight(1f)) {
                            pickingStart = true; timePickerOpen = true
                        }
                        Text("至", fontSize = 12.sp, color = td.muted, modifier = Modifier.padding(horizontal = 10.dp))
                        ReminderTimeBox("结束", fmtHM(s.endHour, s.endMinute), Modifier.weight(1f)) {
                            pickingStart = false; timePickerOpen = true
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDF7F2))
                            .border(1.dp, Color(0xFFCFE9DF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("点击开始 / 结束，直接输入时与分 · 覆盖 00:00–23:59",
                            fontSize = 12.sp, color = td.accentInk, lineHeight = 17.sp)
                    }
                    RowItem(title = "夜间免打扰", desc = "结束时段后不再弹窗与通知", divider = false) {
                        TrackSwitch(s.dndEnabled, { update(s.copy(dndEnabled = it)) })
                    }
                }
                Spacer(Modifier.height(12.dp))

                Panel(title = "提醒方式") {
                    RowItem(title = "系统通知", desc = "锁屏、通知栏，后台可触发", divider = false) {
                        TrackSwitch(s.sysNotifyEnabled, { update(s.copy(sysNotifyEnabled = it)) })
                    }
                    RowItem(title = "全屏打卡弹窗", desc = "类似闹钟，覆盖当前界面", divider = true) {
                        TrackSwitch(s.popupEnabled, { update(s.copy(popupEnabled = it)) })
                    }
                }
                Spacer(Modifier.height(12.dp))

                Panel(title = "快捷操作") {
                    RowItem(title = "短时暂停时长", desc = "弹窗里「暂停」的默认值，可自定义", divider = false) {
                        ValueButton("${s.snoozeMinutes} 分") { numMode = "snooze"; numOpen = true }
                    }
                }
            }
        }

        ReminderTimeInputSheet(
            open = timePickerOpen,
            title = if (pickingStart) "选择开始时间" else "选择结束时间",
            initHour = if (pickingStart) s.startHour else s.endHour,
            initMin = if (pickingStart) s.startMinute else s.endMinute,
            onDismiss = { timePickerOpen = false },
            onConfirm = { h, m ->
                if (pickingStart) update(s.copy(startHour = h, startMinute = m))
                else update(s.copy(endHour = h, endMinute = m))
                timePickerOpen = false
                toast.show("${if (pickingStart) "开始" else "结束"} ${fmtHM(h, m)}")
            }
        )

        NumberInputSheet(
            open = numOpen,
            mode = numMode,
            initial = if (numMode == "interval") s.intervalMinutes else s.snoozeMinutes,
            onDismiss = { numOpen = false },
            onConfirm = { n ->
                if (numMode == "interval") {
                    update(s.copy(intervalMinutes = n)); toast.show("间隔已设为 $n 分钟")
                } else {
                    update(s.copy(snoozeMinutes = n)); toast.show("短时暂停 $n 分钟")
                }
                numOpen = false
            },
            onError = { toast.show(it) }
        )

        TrackToast(toast)
    }
}

private fun fmtHM(h: Int, m: Int) = "${pad2(h)}:${pad2(m)}"

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(20.dp))
    ) {
        Text(
            title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.06.sp, color = td.muted,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun RowItem(title: String, desc: String, divider: Boolean, trailing: @Composable () -> Unit) {
    val td = LocalTdColors.current
    Column {
        if (divider) HorizontalDivider(color = td.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = td.fg)
                Text(desc, fontSize = 12.sp, color = td.muted, modifier = Modifier.padding(top = 3.dp))
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun ValueButton(text: String, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(td.bg)
            .border(1.5.dp, td.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = td.fg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(chips: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    val td = LocalTdColors.current
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) td.accentSoft else td.surface)
                    .border(1.5.dp, if (on) td.accent else td.border, RoundedCornerShape(999.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = if (on) td.accentInk else td.fg)
            }
        }
    }
}

@Composable
private fun ReminderTimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(td.bg)
            .border(1.5.dp, td.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, fontSize = 11.sp, color = td.muted, letterSpacing = 0.04.sp)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = td.fg, fontSize = 22.sp,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun BoxScope.ReminderTimeInputSheet(
    open: Boolean,
    title: String,
    initHour: Int,
    initMin: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val td = LocalTdColors.current
    var hour by remember(open, initHour) { mutableStateOf(initHour) }
    var min by remember(open, initMin) { mutableStateOf(initMin) }

    BottomSheetOverlay(visible = open, onDismiss = onDismiss, scrollable = false) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("直接输入或点 +/− 微调 · 时 0–23，分 0–59",
            fontSize = 12.sp, color = td.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            StepperField("时", hour, { hour = it }, Modifier.weight(1f))
            Text(":", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = td.fg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
            StepperField("分", min, { min = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton("确定", Modifier.weight(1f), primary = true) { onConfirm(hour, min) }
        }
    }
}

@Composable
private fun StepperField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    val td = LocalTdColors.current
    val max = if (label == "时") 23 else 59
    // local editable text mirrors the value; committed on change / focus loss
    var text by remember(value) { mutableStateOf(pad2(value)) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.sp,
            color = td.muted, modifier = Modifier.padding(bottom = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepBtn("−") { onChange((value - 1 + (max + 1)) % (max + 1)) }
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    // keep only digits, max 2 chars, clamp to valid range
                    val digits = raw.filter { it.isDigit() }.take(2)
                    text = digits
                    val n = digits.toIntOrNull()
                    if (n != null && n in 0..max) onChange(n)
                },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = td.fg
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = td.accent,
                    unfocusedBorderColor = td.border,
                    focusedContainerColor = td.surface,
                    unfocusedContainerColor = td.bg
                )
            )
            StepBtn("+") { onChange((value + 1) % (max + 1)) }
        }
    }
}

@Composable
private fun StepBtn(text: String, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .size(44.dp, 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(td.bg)
            .border(1.5.dp, td.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 24.sp, color = td.fg)
    }
}

@Composable
private fun BoxScope.NumberInputSheet(
    open: Boolean,
    mode: String,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onError: (String) -> Unit
) {
    val td = LocalTdColors.current
    var text by remember(open, initial) { mutableStateOf(initial.toString()) }
    val title = if (mode == "interval") "自定义提醒间隔" else "自定义短时暂停"
    val hint = if (mode == "interval") "输入 1–180 分钟" else "输入 5–240 分钟"

    BottomSheetOverlay(visible = open, onDismiss = onDismiss, scrollable = false) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(hint, fontSize = 12.sp, color = td.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = td.accent,
                    unfocusedBorderColor = td.border,
                    focusedContainerColor = td.surface,
                    unfocusedContainerColor = td.bg
                )
            )
            Spacer(Modifier.width(10.dp))
            Text("分钟", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = td.muted)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton("确定", Modifier.weight(1f), primary = true) {
                val n = text.toIntOrNull()
                if (mode == "interval") {
                    if (n == null || n < 1 || n > 180) { onError("请输入 1–180 分钟"); return@SheetButton }
                } else {
                    if (n == null || n < 5 || n > 240) { onError("请输入 5–240 分钟"); return@SheetButton }
                }
                onConfirm(n)
            }
        }
    }
}
