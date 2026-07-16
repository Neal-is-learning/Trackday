package com.example.trackday.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.trackday.ui.theme.LocalTdColors

/**
 * A modal bottom sheet that rises from the bottom with a scrim, matching the
 * prototype's `.overlay` + `.sheet` (translateY + fade, 0.28s).
 */
@Composable
fun BoxScope.BottomSheetOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val td = LocalTdColors.current
    // Scrim
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.matchParentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73262A2E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }
    // Sheet
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(280)) { it } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(td.surface)
                // block click-through
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .then(
                    if (scrollable) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp, bottom = 22.dp)
        ) {
            // handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(td.border)
            )
            content()
        }
    }
}
