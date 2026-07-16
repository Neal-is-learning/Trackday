package com.example.trackday.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackday.ui.theme.LocalTdColors
import kotlinx.coroutines.delay

// Toast (bottom pill)

class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    private var counter by mutableStateOf(0)
    val trigger get() = counter

    fun show(msg: String) {
        message = msg
        counter++
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun BoxScope.TrackToast(state: ToastState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.trigger) {
        if (state.message != null) {
            visible = true
            delay(1600)
            visible = false
        }
    }
    val td = LocalTdColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 },
        exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 3 },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 88.dp)
    ) {
        Text(
            text = state.message ?: "",
            color = td.surface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(CircleShape)
                .background(td.fg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// iOS-style switch

@Composable
fun TrackSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val td = LocalTdColors.current
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(200), label = "switch")
    val bg = if (checked) td.accent else Color(0xFFDADCE0)
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .offset(x = offset)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun rememberPressScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")
    return scale
}
