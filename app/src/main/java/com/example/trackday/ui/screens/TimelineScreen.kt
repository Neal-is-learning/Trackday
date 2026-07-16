package com.example.trackday.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.*
import com.example.trackday.ui.AppUiState
import com.example.trackday.ui.common.*
import com.example.trackday.ui.theme.LocalTdColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val WEEK = listOf("日", "一", "二", "三", "四", "五", "六")
private val keyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun TimelineScreen(vm: TrackdayViewModel) {
    val td = LocalTdColors.current
    val toast = rememberToastState()
    val today = vm.today

    var selected by remember { mutableStateOf(today) }
    val records = vm.records

    var editorRecord by remember { mutableStateOf<TimeRecord?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }

    val dayRecords = (records[selected.format(keyFmt)] ?: emptyList())
        .sortedByDescending { it.startMinutes }

    val totalMinutes = dayRecords.sumOf { it.durationMinutes }
    val tagCount = dayRecords.map { it.tag }.distinct().size

    Box(modifier = Modifier.fillMaxSize().background(td.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("时间线", style = MaterialTheme.typography.headlineLarge, color = td.fg)
                        Text(
                            viewDateLabel(selected, today),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = td.muted, modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // demo trigger for the full-screen check-in popup
                        IconBtn(icon = Icons.Rounded.NotificationsActive, desc = "模拟打卡提醒") {
                            AppUiState.openCheckIn()
                        }
                        IconBtn(icon = Icons.Rounded.CalendarMonth, desc = "日历回看") {
                            calendarOpen = true
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                DateStrip(selected, today, records) { selected = it }
            }

            DaySummary(totalMinutes, dayRecords.size, tagCount)

            if (dayRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "这一天还没有记录\n点右下角可以手动补记",
                        color = td.muted, fontSize = 14.sp,
                        textAlign = TextAlign.Center, lineHeight = 21.sp
                    )
                }
            } else {
                RecordList(
                    records = dayRecords,
                    modifier = Modifier.weight(1f),
                    colorForRecord = { r -> Color(vm.colorForTag(r.tag, r.cat)) },
                    onEdit = { editorRecord = it; editorOpen = true },
                    onDelete = { r ->
                        vm.deleteRecord(selected, r.id)
                        toast.show("已删除「${r.tag}」")
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { editorRecord = null; editorOpen = true },
            containerColor = td.accent,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Rounded.Add, "手动补记", modifier = Modifier.size(24.dp))
        }

        EditRecordSheet(
            open = editorOpen,
            record = editorRecord,
            allTags = vm.tagGroups.flatMap { g -> g.children.map { c -> c.name } },
            catResolver = { vm.catForTag(it) },
            onDismiss = { editorOpen = false },
            onSave = { tag, cat, start, end, note ->
                val existing = editorRecord
                if (existing != null) {
                    vm.updateRecord(selected, existing.copy(tag = tag, cat = cat, start = start, end = end, note = note))
                    toast.show("已保存「$tag」")
                } else {
                    vm.addRecord(selected, tag, cat, start, end, note)
                    toast.show("已补记「$tag」")
                }
                editorOpen = false
            },
            onError = { toast.show(it) }
        )

        CalendarSheet(
            open = calendarOpen,
            today = today,
            selected = selected,
            hasRecords = { records[it.format(keyFmt)]?.isNotEmpty() == true },
            onDismiss = { calendarOpen = false },
            onPick = {
                selected = it
                calendarOpen = false
                toast.show("已切换到 ${it.monthValue}月${it.dayOfMonth}日")
            },
            onToday = {
                selected = today
                calendarOpen = false
            }
        )

        TrackToast(toast)
    }
}

private fun viewDateLabel(selected: LocalDate, today: LocalDate): String {
    val label = when (selected) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "周" + WEEK[selected.dayOfWeek.value % 7]
    }
    return "$label · ${selected.monthValue}月${selected.dayOfMonth}日"
}

@Composable
private fun IconBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(td.surface)
            .border(1.5.dp, td.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = td.fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DateStrip(
    selected: LocalDate,
    today: LocalDate,
    records: Map<String, List<TimeRecord>>,
    onSelect: (LocalDate) -> Unit
) {
    val td = LocalTdColors.current
    var anchor = today
    val gap = ChronoUnit.DAYS.between(selected, today)
    if (gap > 6) anchor = selected.plusDays(3)
    if (anchor.isAfter(today)) anchor = today

    val days = (6 downTo 0).mapNotNull { i ->
        val d = anchor.minusDays(i.toLong())
        if (d.isAfter(today)) null else d
    }

    // Start scrolled to the end so today (rightmost) is visible; users swipe
    // LEFT to reveal older days. Re-align whenever the selected day changes.
    val listState = rememberLazyListState()
    LaunchedEffect(days.size, selected) {
        val idx = days.indexOf(selected).takeIf { it >= 0 } ?: days.lastIndex
        if (idx >= 0) listState.scrollToItem(idx)
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { d ->
            val active = d == selected
            val has = records[d.format(keyFmt)]?.isNotEmpty() == true
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) td.accent else td.surface)
                    .border(
                        1.5.dp,
                        if (active) td.accent else td.border,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(d) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    WEEK[d.dayOfWeek.value % 7],
                    fontSize = 12.sp,
                    color = if (active) Color.White.copy(alpha = 0.9f) else td.muted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    d.dayOfMonth.toString(),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.White else td.fg
                )
                if (has) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else td.muted.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummary(totalMin: Int, count: Int, tags: Int) {
    val td = LocalTdColors.current
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        SummaryCell(if (totalMin == 0) "0" else fmtDuration(totalMin).replace(" ", ""), "已记录", Modifier.weight(1f))
        SummaryCell(count.toString(), "打卡次数", Modifier.weight(1f))
        SummaryCell(tags.toString(), "小标签", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCell(num: String, label: String, modifier: Modifier) {
    val td = LocalTdColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, style = MaterialTheme.typography.headlineMedium, color = td.fg)
        Text(label, fontSize = 11.sp, color = td.muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RecordList(
    records: List<TimeRecord>,
    modifier: Modifier = Modifier,
    colorForRecord: (TimeRecord) -> Color,
    onEdit: (TimeRecord) -> Unit,
    onDelete: (TimeRecord) -> Unit
) {
    val groups = linkedMapOf<String, MutableList<TimeRecord>>(
        "晚上" to mutableListOf(), "下午" to mutableListOf(), "上午" to mutableListOf()
    )
    records.forEach { r ->
        val h = r.start.split(":")[0].toInt()
        val g = if (h >= 18) "晚上" else if (h >= 12) "下午" else "上午"
        groups[g]!!.add(r)
    }
    var openId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 88.dp)
    ) {
        groups.forEach { (label, list) ->
            if (list.isNotEmpty()) {
                item(key = "label-$label") {
                    Text(
                        label,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        letterSpacing = 0.06.sp,
                        color = LocalTdColors.current.muted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                    )
                }
                items(list, key = { it.id }) { r ->
                    RecordCard(
                        record = r,
                        railColor = colorForRecord(r),
                        expanded = openId == r.id,
                        onToggle = { openId = if (openId == r.id) null else r.id },
                        onEdit = { onEdit(r) },
                        onDelete = { onDelete(r) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: TimeRecord,
    railColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(railColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.tag, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = td.fg)
                Text(
                    "${record.start} – ${record.end}",
                    fontSize = 12.sp, color = td.muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (record.note.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(td.bg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(record.note, fontSize = 13.sp, color = td.fg, lineHeight = 19.sp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                fmtDuration(record.durationMinutes),
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = td.fg
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniButton("修改", Modifier.weight(1f), onClick = onEdit)
                MiniButton("删除", Modifier.weight(1f), danger = true, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun MiniButton(text: String, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (danger) td.dangerSoft else td.bg)
            .border(
                1.5.dp,
                if (danger) td.dangerBorder else td.border,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (danger) td.danger else td.fg
        )
    }
}

@Composable
private fun BoxScope.EditRecordSheet(
    open: Boolean,
    record: TimeRecord?,
    allTags: List<String>,
    catResolver: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (tag: String, cat: String, start: String, end: String, note: String) -> Unit,
    onError: (String) -> Unit
) {
    val td = LocalTdColors.current
    // a record's tag may be a preset child tag, or a free-form word that maps
    // to "其他". Detect which on open so the UI restores the right state.
    val startIsCustom = record?.tag?.let { it.isNotBlank() && it !in allTags } == true
    var selectedTag by remember(open, record) { mutableStateOf(if (startIsCustom) null else record?.tag) }
    var customMode by remember(open, record) { mutableStateOf(startIsCustom) }
    var customTag by remember(open, record) { mutableStateOf(if (startIsCustom) record?.tag ?: "" else "") }
    var startVal by remember(open, record) { mutableStateOf(record?.start ?: "10:00") }
    var endVal by remember(open, record) { mutableStateOf(record?.end ?: "10:30") }
    var note by remember(open, record) { mutableStateOf(record?.note ?: "") }

    var pickerOpen by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }

    BottomSheetOverlay(visible = open, onDismiss = onDismiss) {
        Text(
            if (record != null) "修改记录" else "手动补记",
            style = MaterialTheme.typography.headlineSmall, color = td.fg,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        FieldLabel("小标签")
        FlowTagPicker(
            tags = allTags,
            selected = selectedTag,
            customMode = customMode,
            onSelect = { selectedTag = it; customMode = false },
            onCustom = { customMode = true; selectedTag = null }
        )
        if (customMode) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customTag,
                onValueChange = { if (it.length <= 12) customTag = it },
                placeholder = { Text("输入自定义标签，将归入「其他」", color = td.muted) },
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
        Spacer(Modifier.height(12.dp))

        FieldLabel("时段（点击滑动选择时 / 分）")
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBox("开始", startVal, pickerOpen && pickingStart, Modifier.weight(1f)) {
                pickingStart = true; pickerOpen = true
            }
            Text("至", fontSize = 13.sp, color = td.muted, modifier = Modifier.padding(horizontal = 8.dp))
            TimeBox("结束", endVal, pickerOpen && !pickingStart, Modifier.weight(1f)) {
                pickingStart = false; pickerOpen = true
            }
        }
        Spacer(Modifier.height(12.dp))

        FieldLabel("备注")
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 120) note = it },
            placeholder = { Text("可选备注", color = td.muted) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = td.accent,
                unfocusedBorderColor = td.border,
                focusedContainerColor = td.surface,
                unfocusedContainerColor = td.bg
            )
        )
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton(if (record == null) "保存补记" else "保存修改", Modifier.weight(1f), primary = true) {
                val tag = if (customMode) customTag.trim() else selectedTag
                if (tag.isNullOrBlank()) {
                    onError(if (customMode) "请输入自定义标签" else "请选择小标签")
                    return@SheetButton
                }
                if (startVal.toMin() >= endVal.toMin()) { onError("结束须晚于开始"); return@SheetButton }
                // catResolver returns "life" (→ 其他) for unknown words
                onSave(tag, catResolver(tag), startVal, endVal, note.trim())
            }
        }
    }

    TimeWheelSheet(
        open = pickerOpen,
        title = if (pickingStart) "选择开始时间" else "选择结束时间",
        initial = if (pickingStart) startVal else endVal,
        onDismiss = { pickerOpen = false },
        onConfirm = { v ->
            if (pickingStart) startVal = v else endVal = v
            pickerOpen = false
        }
    )
}

private fun String.toMin(): Int {
    val p = split(":"); return p[0].toInt() * 60 + p[1].toInt()
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.04.sp, color = LocalTdColors.current.muted,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowTagPicker(
    tags: List<String>,
    selected: String?,
    customMode: Boolean,
    onSelect: (String) -> Unit,
    onCustom: () -> Unit
) {
    val td = LocalTdColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val on = !customMode && tag == selected
            TagChip(text = tag, on = on) { onSelect(tag) }
        }
        // "自定义" chip — reveals a free-form input, categorised as 其他
        TagChip(text = "＋自定义", on = customMode, onClick = onCustom)
    }
}

@Composable
private fun TagChip(text: String, on: Boolean, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) td.accentSoft else td.surface)
            .border(
                1.5.dp,
                if (on) td.accent else td.border,
                RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (on) td.accentInk else td.fg
        )
    }
}

@Composable
private fun TimeBox(label: String, value: String, picking: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (picking) td.accentSoft else td.bg)
            .border(
                1.5.dp,
                if (picking) td.accent else td.border,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, fontSize = 11.sp, color = td.muted, letterSpacing = 0.04.sp)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = td.fg, fontSize = 22.sp)
    }
}

@Composable
fun SheetButton(text: String, modifier: Modifier = Modifier, primary: Boolean, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) td.accent else td.surface)
            .then(if (primary) Modifier else Modifier.border(1.5.dp, td.border, RoundedCornerShape(14.dp)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = if (primary) Color.White else td.fg
        )
    }
}

@Composable
private fun BoxScope.TimeWheelSheet(
    open: Boolean,
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val td = LocalTdColors.current
    var hour by remember(open, initial) { mutableStateOf(initial.split(":")[0].toInt()) }
    var min by remember(open, initial) { mutableStateOf(initial.split(":")[1].toInt()) }

    BottomSheetOverlay(visible = open, onDismiss = onDismiss, scrollable = false) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg)
        Text(
            "上下滑动 · 时 / 分突出显示",
            fontSize = 12.sp, color = td.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("时", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = td.muted,
                    modifier = Modifier.padding(bottom = 6.dp))
                WheelPicker(24, hour, { hour = it }, Modifier.fillMaxWidth())
            }
            Text(":", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = td.fg,
                modifier = Modifier.padding(horizontal = 8.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("分", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = td.muted,
                    modifier = Modifier.padding(bottom = 6.dp))
                WheelPicker(60, min, { min = it }, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton("确定", Modifier.weight(1f), primary = true) {
                onConfirm("${pad2(hour)}:${pad2(min)}")
            }
        }
    }
}

@Composable
private fun BoxScope.CalendarSheet(
    open: Boolean,
    today: LocalDate,
    selected: LocalDate,
    hasRecords: (LocalDate) -> Boolean,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    onToday: () -> Unit
) {
    val td = LocalTdColors.current
    var viewMonth by remember(open) { mutableStateOf(YearMonth.from(selected)) }

    BottomSheetOverlay(visible = open, onDismiss = onDismiss, scrollable = false) {
        Text("选择日期", style = MaterialTheme.typography.headlineSmall, color = td.fg,
            modifier = Modifier.padding(bottom = 14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalNavBtn(Icons.Rounded.ChevronLeft) { viewMonth = viewMonth.minusMonths(1) }
            Text(
                "${viewMonth.year}年${viewMonth.monthValue}月",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = td.fg
            )
            CalNavBtn(Icons.Rounded.ChevronRight) {
                val next = viewMonth.plusMonths(1)
                if (!next.isAfter(YearMonth.from(today))) viewMonth = next
            }
        }
        CalendarGrid(viewMonth, today, selected, hasRecords, onPick)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("关闭", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton("回到今天", Modifier.weight(1f), primary = true, onClick = onToday)
        }
    }
}

@Composable
private fun CalNavBtn(icon: ImageVector, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(td.bg)
            .border(1.5.dp, td.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = td.fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    hasRecords: (LocalDate) -> Boolean,
    onPick: (LocalDate) -> Unit
) {
    val td = LocalTdColors.current
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK.forEach { w ->
                Text(
                    w, fontSize = 11.sp, color = td.muted, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
        }
        val first = month.atDay(1)
        val startPad = first.dayOfWeek.value % 7
        val daysInMonth = month.lengthOfMonth()
        var cell = 0
        while (cell < 42) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val idx = cell
                    val dayNum = idx - startPad + 1
                    val inMonth = dayNum in 1..daysInMonth
                    val date = if (inMonth) month.atDay(dayNum) else null
                    Box(modifier = Modifier.weight(1f).heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val future = date.isAfter(today)
                            val isSel = date == selected
                            val has = hasRecords(date)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) td.accent else Color.Transparent)
                                    .then(
                                        if (!future) Modifier.clickable { onPick(date) } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayNum.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                    color = when {
                                        isSel -> Color.White
                                        future -> td.fg.copy(alpha = 0.35f)
                                        else -> td.fg
                                    }
                                )
                                if (has) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) Color.White else td.accent)
                                    )
                                }
                            }
                        }
                    }
                    cell++
                }
            }
            if (cell >= startPad + daysInMonth) break
        }
    }
}
