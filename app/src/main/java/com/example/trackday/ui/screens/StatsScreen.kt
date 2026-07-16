package com.example.trackday.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.*
import com.example.trackday.ui.common.*
import com.example.trackday.ui.theme.LocalTdColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.min

private val keyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val WK = listOf("日", "一", "二", "三", "四", "五", "六")

private data class LegendItem(val color: Long, val name: String, val minutes: Int, val pct: Int)
private data class StatModel(
    val eyebrow: String,
    val totalText: String,
    val hint: String,
    val legend: List<LegendItem>,
    val trendTitle: String,
    val trend: List<Int>,
    val trendLabels: List<String>,
    val trendActive: Int,
    val empty: Boolean,
    val summaryTitle: String,
    val autoNote: String
)

@Composable
fun StatsScreen(vm: TrackdayViewModel) {
    val td = LocalTdColors.current
    val toast = rememberToastState()
    var range by remember { mutableStateOf("day") }
    val records = vm.records

    val tagGroups = vm.tagGroups   // observe so renames/recolors recompute
    val model = remember(range, records, tagGroups) { computeModel(vm, range) }
    val dateKey = vm.today.format(keyFmt)

    val savedSummary = vm.getSummary(range, dateKey)
    val summaryText = savedSummary ?: autoSummary(range, model)
    val photosMap = vm.photos           // observe
    val photos = photosMap["$range|$dateKey"] ?: emptyList()

    var editOpen by remember { mutableStateOf(false) }

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // copy into app storage (stable, no permission races) then add
            vm.importAndAddPhoto(range, dateKey, uri) { ok ->
                toast.show(if (ok) "已添加${rangeName(range)}配图" else "图片添加失败")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(td.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp)) {
                Text("数据复盘", style = MaterialTheme.typography.headlineLarge, color = td.fg,
                    modifier = Modifier.padding(bottom = 12.dp))
                SegmentedControl(range) { range = it }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp)
                    .padding(bottom = 88.dp)
            ) {
                HeroCard(model)
                Spacer(Modifier.height(14.dp))
                BreakdownPanel(model)
                Spacer(Modifier.height(14.dp))
                TrendPanel(model)
                Spacer(Modifier.height(14.dp))
                SummaryCard(
                    title = model.summaryTitle,
                    body = summaryText,
                    note = if (savedSummary != null) "已手动编辑 · 点撰写总结可再改" else model.autoNote,
                    photos = photos,
                    onEdit = { editOpen = true },
                    onPhoto = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onRemovePhoto = { uri ->
                        vm.removePhoto(range, dateKey, uri)
                        toast.show("已移除配图")
                    }
                )
            }
        }

        SummaryEditSheet(
            open = editOpen,
            rangeName = rangeName(range),
            initial = summaryText,
            onDismiss = { editOpen = false },
            onSave = {
                vm.saveSummaryText(range, dateKey, it)
                editOpen = false
                toast.show("已保存${rangeName(range)}小结")
            },
            onError = { toast.show(it) }
        )

        TrackToast(toast)
    }
}

@Composable
private fun SegmentedControl(current: String, onSelect: (String) -> Unit) {
    val td = LocalTdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEDEEF1))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("day" to "日", "week" to "周", "month" to "月").forEach { (key, label) ->
            val active = current == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) td.surface else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (active) td.fg else td.muted
                )
            }
        }
    }
}

@Composable
private fun HeroCard(model: StatModel) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(model.eyebrow, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.06.sp, color = td.muted)
        Spacer(Modifier.height(6.dp))
        Text(model.totalText, style = MaterialTheme.typography.displayLarge, color = td.fg, fontSize = 36.sp)
        Spacer(Modifier.height(6.dp))
        Text(model.hint, fontSize = 13.sp, color = td.muted)

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(model.legend, modifier = Modifier.size(140.dp))
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (model.legend.isEmpty()) {
                    Text("暂无数据", fontSize = 13.sp, color = td.muted)
                }
                model.legend.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                .background(Color(item.color))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = td.fg, modifier = Modifier.weight(1f))
                        Text(fmtShort(item.minutes), fontSize = 13.sp, color = td.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(legend: List<LegendItem>, modifier: Modifier = Modifier) {
    val td = LocalTdColors.current
    val total = legend.sumOf { it.minutes }.toFloat()
    val colors = legend.map { Color(it.color) }
    val sweep = animateFloatAsState(if (total > 0) 1f else 0f, tween(600), label = "donut")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.19f
            val diameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f, (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            if (total <= 0f) {
                drawArc(
                    color = td.border, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = stroke)
                )
            } else {
                var startAngle = -90f
                legend.forEachIndexed { i, item ->
                    val fraction = item.minutes / total
                    val angle = 360f * fraction * sweep.value
                    drawArc(
                        color = colors[i], startAngle = startAngle, sweepAngle = angle, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(width = stroke)
                    )
                    startAngle += angle
                }
            }
        }
    }
}

@Composable
private fun BreakdownPanel(model: StatModel) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("大标签占比", style = MaterialTheme.typography.headlineSmall, color = td.fg, fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 14.dp))
        if (model.legend.isEmpty()) {
            Text("暂无数据", fontSize = 13.sp, color = td.muted)
        }
        model.legend.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, fontSize = 12.sp, color = td.muted, modifier = Modifier.width(48.dp))
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(td.bg)
                ) {
                    val pct by animateFloatAsState(item.pct / 100f, tween(600), label = "bar")
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(item.color))
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("${item.pct}%", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = td.fg, modifier = Modifier.width(44.dp))
            }
        }
    }
}

@Composable
private fun TrendPanel(model: StatModel) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(model.trendTitle, style = MaterialTheme.typography.headlineSmall, color = td.fg, fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 14.dp))
        // indices that actually have a label (drop empty padding slots)
        val visible = model.trend.indices.filter {
            model.trendLabels.getOrNull(it)?.isNotEmpty() == true
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            visible.forEach { i ->
                val active = i == model.trendActive
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // fixed-height plotting area; bar fills a fraction of THIS,
                    // so the tallest (100%) can never overflow into the label.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val frac by animateFloatAsState(
                            (model.trend[i].coerceIn(0, 100)) / 100f,
                            tween(600), label = "trend"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(frac.coerceAtLeast(0.06f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (active) td.accent else td.accent.copy(alpha = 0.5f))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        model.trendLabels[i], fontSize = 11.sp, maxLines = 1,
                        color = if (active) td.accentInk else td.muted,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    body: String,
    note: String,
    photos: List<String>,
    onEdit: () -> Unit,
    onPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit
) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE9F6F0), td.surface)))
            .border(1.dp, Color(0xFFCFE9DF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg, fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        Text(body, fontSize = 14.sp, color = td.fg, lineHeight = 22.sp)
        Spacer(Modifier.height(12.dp))
        Text(note, fontSize = 12.sp, color = td.muted)

        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { uri ->
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCFE9DF), RoundedCornerShape(12.dp))
                    ) {
                        coil.compose.AsyncImage(
                            model = uri,
                            contentDescription = "小结配图",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x99262A2E))
                                .clickable { onRemovePhoto(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("撰写总结", Modifier.weight(1f), onClick = onEdit)
            PrimaryButton("添加图片", Modifier.weight(1f), onClick = onPhoto)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(td.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun BoxScope.SummaryEditSheet(
    open: Boolean,
    rangeName: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onError: (String) -> Unit
) {
    val td = LocalTdColors.current
    var text by remember(open, initial) { mutableStateOf(initial) }
    BottomSheetOverlay(visible = open, onDismiss = onDismiss) {
        Text("撰写${rangeName}小结", style = MaterialTheme.typography.headlineSmall, color = td.fg)
        Text("可在自动生成的基础上修改，或整段重写。", fontSize = 12.sp, color = td.muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("写下今天的时间复盘…", color = td.muted) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
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
            SheetButton("保存小结", Modifier.weight(1f), primary = true) {
                if (text.isBlank()) { onError("小结不能为空"); return@SheetButton }
                onSave(text.trim())
            }
        }
    }
}

private fun rangeName(r: String) = when (r) { "day" -> "今日"; "week" -> "本周"; else -> "本月" }

private fun computeModel(vm: TrackdayViewModel, range: String): StatModel {
    val today = vm.today
    val keys = when (range) {
        "day" -> listOf(today.format(keyFmt))
        "week" -> (6 downTo 0).map { today.minusDays(it.toLong()).format(keyFmt) }
        else -> {
            val ym = YearMonth.from(today)
            (1..ym.lengthOfMonth()).map { ym.atDay(it).format(keyFmt) }
        }
    }
    val agg = vm.aggregateDays(keys)
    val total = agg.totalMinutes

    // 统计按大标签汇总：用实时标签组（含最新名称 / 颜色）聚合
    val legend = vm.aggregateByGroup(keys).map { slice ->
        LegendItem(
            color = slice.color,
            name = slice.name,
            minutes = slice.minutes,
            pct = if (total > 0) Math.round(slice.minutes / total.toFloat() * 100) else 0
        )
    }

    val trend: List<Int>
    val trendLabels: List<String>
    val trendActive: Int
    val trendTitle: String
    val eyebrow: String
    val hint: String

    if (range == "month") {
        eyebrow = "本月已记录"
        trendTitle = "本月四周趋势"
        val ym = YearMonth.from(today)
        val weeks = IntArray(5)
        for (dd in 1..ym.lengthOfMonth()) {
            val wi = min(4, (dd - 1) / 7)
            weeks[wi] += vm.dayTotal(ym.atDay(dd))
        }
        weeks[3] += weeks[4]
        val used = weeks.copyOfRange(0, 4).toList()
        val maxW = (used.maxOrNull() ?: 1).coerceAtLeast(1)
        trend = used.map { Math.round(it / maxW.toFloat() * 100) } + listOf(0, 0, 0)
        trendLabels = listOf("第1周", "第2周", "第3周", "第4周", "", "", "")
        trendActive = min(3, (today.dayOfMonth - 1) / 7)
        hint = "本月有记录 ${agg.activeDays} 天 · 共 ${agg.count} 条"
    } else {
        eyebrow = if (range == "day") "今日已记录" else "本周已记录"
        trendTitle = if (range == "day") "近 7 日记录时长" else "本周每日时长"
        val weekDates = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val totals = weekDates.map { vm.dayTotal(it) }
        val maxD = (totals.maxOrNull() ?: 1).coerceAtLeast(1)
        trend = totals.map { if (it > 0) Math.round(it / maxD.toFloat() * 100) else 0 }
        trendLabels = weekDates.map { WK[it.dayOfWeek.value % 7] }
        trendActive = 6
        hint = if (range == "day") {
            "${agg.count} 条记录 · 覆盖清醒时段约 ${Math.round(total / (16f * 60) * 100)}%"
        } else {
            val avg = if (agg.activeDays > 0) total / agg.activeDays else 0
            "日均约 ${fmtDurationLong(avg)} · 本周有记录 ${agg.activeDays} 天"
        }
    }

    return StatModel(
        eyebrow = eyebrow,
        totalText = fmtDurationLong(total),
        hint = hint,
        legend = legend,
        trendTitle = trendTitle,
        trend = trend,
        trendLabels = trendLabels,
        trendActive = trendActive,
        empty = total == 0,
        summaryTitle = rangeName(range) + "小结",
        autoNote = "由${rangeName(range)}标签时长自动生成 · 可手动改写"
    )
}

private fun autoSummary(range: String, model: StatModel): String {
    val scope = rangeName(range)
    if (model.empty || model.legend.isEmpty()) return "${scope}还没有记录，去「记录」页补几条吧。"
    val top = model.legend[0]
    var s = "${scope}「${top.name}」占比最高，约 ${top.pct}%，累计 ${fmtShort(top.minutes)}。"
    if (model.legend.size > 1) s += "其次是「${model.legend[1].name}」。"
    return s
}

private fun fmtShort(m: Int): String {
    if (m < 60) return "${m}m"
    val h = m / 60; val r = m % 60
    return if (r > 0) "${h}h${r}m" else "${h}h"
}
