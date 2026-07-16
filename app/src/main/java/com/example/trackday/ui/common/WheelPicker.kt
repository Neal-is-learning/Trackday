package com.example.trackday.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.data.pad2
import com.example.trackday.ui.theme.LocalTdColors
import kotlinx.coroutines.flow.distinctUntilChanged

private val ITEM_HEIGHT = 44.dp

/**
 * A vertical snapping wheel selector, matching the prototype's time wheel.
 * With 3 visible rows, the centre row is the selection. We add one blank row of
 * padding above/below so the first/last real items can reach the centre band.
 */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val td = LocalTdColors.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // Centre item = firstVisibleItemIndex (because top padding = 1 row).
    val centerIndex by remember {
        derivedStateOf {
            val off = state.firstVisibleItemScrollOffset
            val base = state.firstVisibleItemIndex
            if (off > (ITEM_HEIGHT.value * 1.5f)) base + 1 else base
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val clamped = idx.coerceIn(0, count - 1)
                if (clamped != value) onValueChange(clamped)
            }
    }

    Box(
        modifier = modifier
            .height(ITEM_HEIGHT * 3)
            .clip(RoundedCornerShape(16.dp))
            .background(td.bg)
            .border(1.5.dp, td.border, RoundedCornerShape(16.dp))
    ) {
        // highlight band (centre row)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(ITEM_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(td.accentSoft)
                .border(1.5.dp, td.accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = ITEM_HEIGHT) // 1 blank row each side
        ) {
            itemsIndexed((0 until count).toList()) { index, item ->
                val selected = index == value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pad2(item),
                        textAlign = TextAlign.Center,
                        fontSize = if (selected) 26.sp else 22.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) td.fg else td.muted
                    )
                }
            }
        }
    }
}
