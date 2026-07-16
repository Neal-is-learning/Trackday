package com.example.trackday.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.*
import com.example.trackday.ui.common.*
import com.example.trackday.ui.theme.LocalTdColors

private sealed class TagEditCtx {
    data class ParentNew(var color: Long, var icon: String) : TagEditCtx()
    data class ParentEdit(val group: TagGroup, var color: Long, var icon: String) : TagEditCtx()
    data class ChildNew(val group: TagGroup) : TagEditCtx()
    data class ChildEdit(val group: TagGroup, val child: ChildTag) : TagEditCtx()
}

@Composable
fun TagsScreen(vm: TrackdayViewModel) {
    val td = LocalTdColors.current
    val toast = rememberToastState()
    val groups = vm.tagGroups

    var openIds by remember { mutableStateOf(setOf(groups.firstOrNull()?.id)) }
    var editCtx by remember { mutableStateOf<TagEditCtx?>(null) }
    var confirm by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(td.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("标签", style = MaterialTheme.typography.headlineLarge, color = td.fg)
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(td.accent)
                        .clickable {
                            editCtx = TagEditCtx.ParentNew(TAG_COLORS[4], "star")
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("大标签", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            Text(
                "打卡时只选小标签；统计会按大标签汇总时长。",
                fontSize = 13.sp, color = td.muted, lineHeight = 19.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 88.dp)
            ) {
                items(groups, key = { it.id }) { g ->
                    TagGroupCard(
                        group = g,
                        expanded = openIds.contains(g.id),
                        onToggle = {
                            openIds = if (openIds.contains(g.id)) openIds - g.id else openIds + g.id
                        },
                        onEditParent = { editCtx = TagEditCtx.ParentEdit(g, g.color, g.icon) },
                        onDeleteParent = {
                            confirm = Triple(
                                "删除大标签",
                                "确定删除大标签「${g.name}」及其 ${g.children.size} 个小标签吗？"
                            ) {
                                vm.deleteTagGroup(g.id)
                                toast.show("已删除大标签「${g.name}」")
                            }
                        },
                        onAddChild = { editCtx = TagEditCtx.ChildNew(g) },
                        onEditChild = { c -> editCtx = TagEditCtx.ChildEdit(g, c) },
                        onDeleteChild = { c ->
                            confirm = Triple(
                                "删除小标签",
                                "确定删除小标签「${c.name}」吗？相关历史记录会保留。"
                            ) {
                                vm.deleteChildTag(g.id, c.id)
                                toast.show("已删除小标签「${c.name}」")
                            }
                        }
                    )
                }
            }
        }

        TagEditorSheet(
            ctx = editCtx,
            onDismiss = { editCtx = null },
            onSaveParentNew = { name, color, icon ->
                vm.addTagGroup(name, color, icon)
                openIds = openIds + vm.tagGroups.last().id
                toast.show("已创建大标签「$name」")
                editCtx = null
            },
            onSaveParentEdit = { id, name, color, icon ->
                vm.updateTagGroup(id, name, color, icon)
                toast.show("已更新大标签「$name」")
                editCtx = null
            },
            onSaveChildNew = { gid, name ->
                vm.addChildTag(gid, name)
                openIds = openIds + gid
                toast.show("已添加小标签「$name」")
                editCtx = null
            },
            onSaveChildEdit = { gid, cid, name ->
                vm.updateChildTag(gid, cid, name)
                toast.show("已更新小标签「$name」")
                editCtx = null
            },
            onError = { toast.show(it) }
        )

        confirm?.let { (title, body, action) ->
            ConfirmSheet(
                title = title, body = body,
                onDismiss = { confirm = null },
                onConfirm = { action(); confirm = null }
            )
        }

        TrackToast(toast)
    }
}

@Composable
private fun TagGroupCard(
    group: TagGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditParent: () -> Unit,
    onDeleteParent: () -> Unit,
    onAddChild: () -> Unit,
    onEditChild: (ChildTag) -> Unit,
    onDeleteChild: (ChildTag) -> Unit
) {
    val td = LocalTdColors.current
    Column(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(td.surface)
            .border(1.dp, td.border, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggle() }
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(group.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tagIcon(group.icon), null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = td.fg)
                Text(
                    "${group.children.size} 个小标签",
                    fontSize = 12.sp, color = td.muted, modifier = Modifier.padding(top = 2.dp)
                )
            }
            val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "chev")
            Icon(
                Icons.Rounded.KeyboardArrowDown, null, tint = td.muted,
                modifier = Modifier.size(28.dp).rotate(rot)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
        ) {
            Column {
                Column(modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 12.dp)
                ) {
                    HorizontalDivider(color = td.border)
                    group.children.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(td.border)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = td.fg, modifier = Modifier.weight(1f))
                            ChipButton("编辑") { onEditChild(c) }
                            Spacer(Modifier.width(6.dp))
                            ChipButton("删除", danger = true) { onDeleteChild(c) }
                        }
                        HorizontalDivider(color = td.border)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAddChild() }
                            .border(1.5.dp, td.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ 添加小标签", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = td.muted)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FootButton("编辑大标签", Modifier.weight(1f), onClick = onEditParent)
                    FootButton("删除", Modifier.weight(1f), danger = true, onClick = onDeleteParent)
                }
            }
        }
    }
}

@Composable
private fun ChipButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (danger) td.dangerSoft else td.bg)
            .border(1.5.dp, if (danger) td.dangerBorder else td.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (danger) td.danger else td.fg)
    }
}

@Composable
private fun FootButton(text: String, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val td = LocalTdColors.current
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (danger) td.dangerSoft else td.bg)
            .border(1.5.dp, if (danger) td.dangerBorder else td.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (danger) td.danger else td.fg)
    }
}

@Composable
private fun BoxScope.TagEditorSheet(
    ctx: TagEditCtx?,
    onDismiss: () -> Unit,
    onSaveParentNew: (String, Long, String) -> Unit,
    onSaveParentEdit: (String, String, Long, String) -> Unit,
    onSaveChildNew: (String, String) -> Unit,
    onSaveChildEdit: (String, String, String) -> Unit,
    onError: (String) -> Unit
) {
    val td = LocalTdColors.current
    val open = ctx != null
    val isParent = ctx is TagEditCtx.ParentNew || ctx is TagEditCtx.ParentEdit

    var name by remember(ctx) {
        mutableStateOf(
            when (ctx) {
                is TagEditCtx.ParentEdit -> ctx.group.name
                is TagEditCtx.ChildEdit -> ctx.child.name
                else -> ""
            }
        )
    }
    var color by remember(ctx) {
        mutableStateOf(
            when (ctx) {
                is TagEditCtx.ParentNew -> ctx.color
                is TagEditCtx.ParentEdit -> ctx.color
                else -> TAG_COLORS[4]
            }
        )
    }
    var icon by remember(ctx) {
        mutableStateOf(
            when (ctx) {
                is TagEditCtx.ParentNew -> ctx.icon
                is TagEditCtx.ParentEdit -> ctx.icon
                else -> "star"
            }
        )
    }

    val title = when (ctx) {
        is TagEditCtx.ParentNew -> "新建大标签"
        is TagEditCtx.ParentEdit -> "编辑大标签"
        is TagEditCtx.ChildNew -> "新建小标签"
        is TagEditCtx.ChildEdit -> "编辑小标签"
        null -> ""
    }

    BottomSheetOverlay(visible = open, onDismiss = onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg,
            modifier = Modifier.padding(bottom = 12.dp))

        if (isParent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(td.bg)
                    .border(1.5.dp, td.border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tagIcon(icon), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name.ifBlank { "标签名称" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = td.fg)
                    Text("底色 + 图形组合后的效果", fontSize = 12.sp, color = td.muted)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Text("名称", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = td.muted,
            modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 12) name = it },
            placeholder = { Text(if (isParent) "例如：写作业" else "例如：写周报", color = td.muted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = td.accent,
                unfocusedBorderColor = td.border,
                focusedContainerColor = td.surface,
                unfocusedContainerColor = td.bg
            )
        )
        Spacer(Modifier.height(12.dp))

        if (isParent) {
            Text("底色", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = td.muted,
                modifier = Modifier.padding(bottom = 6.dp))
            SwatchGrid(color) { color = it }
            Spacer(Modifier.height(12.dp))
            Text("图形", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = td.muted,
                modifier = Modifier.padding(bottom = 6.dp))
            IconGrid(icon) { icon = it }
            Spacer(Modifier.height(14.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            SheetButton("保存", Modifier.weight(1f), primary = true) {
                if (name.isBlank()) { onError("请填写名称"); return@SheetButton }
                when (ctx) {
                    is TagEditCtx.ParentNew -> onSaveParentNew(name.trim(), color, icon)
                    is TagEditCtx.ParentEdit -> onSaveParentEdit(ctx.group.id, name.trim(), color, icon)
                    is TagEditCtx.ChildNew -> onSaveChildNew(ctx.group.id, name.trim())
                    is TagEditCtx.ChildEdit -> onSaveChildEdit(ctx.group.id, ctx.child.id, name.trim())
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun SwatchGrid(selected: Long, onSelect: (Long) -> Unit) {
    val td = LocalTdColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TAG_COLORS.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { col ->
                    val sel = col == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(col))
                            .then(if (sel) Modifier.border(2.dp, td.fg, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onSelect(col) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sel) {
                            Box(
                                modifier = Modifier.size(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    val td = LocalTdColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ICON_KEYS.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val sel = key == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) td.surface else td.bg)
                            .border(1.5.dp, if (sel) td.fg else td.border, RoundedCornerShape(12.dp))
                            .clickable { onSelect(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tagIcon(key), null, tint = if (sel) td.fg else td.muted,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ConfirmSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val td = LocalTdColors.current
    BottomSheetOverlay(visible = true, onDismiss = onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = td.fg,
            modifier = Modifier.padding(bottom = 8.dp))
        Text(body, fontSize = 14.sp, color = td.muted, lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetButton("取消", Modifier.weight(1f), primary = false, onClick = onDismiss)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(td.danger)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) {
                Text("删除", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}
