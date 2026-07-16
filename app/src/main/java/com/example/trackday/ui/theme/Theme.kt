package com.example.trackday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom token holder so screens can access brand colours directly.
data class TrackDayColors(
    val bg: Color,
    val surface: Color,
    val fg: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val learn: Color,
    val rest: Color,
    val work: Color,
    val life: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerBorder: Color
)

val LocalTdColors = staticCompositionLocalOf {
    TrackDayColors(
        bg = TdBg, surface = TdSurface, fg = TdFg, muted = TdMuted,
        border = TdBorder, accent = TdAccent, accentSoft = TdAccentSoft,
        accentInk = TdAccentInk, learn = TdLearn, rest = TdRest,
        work = TdWork, life = TdLife, danger = TdDanger,
        dangerSoft = TdDangerSoft, dangerBorder = TdDangerBorder
    )
}

private val TdColorScheme = lightColorScheme(
    primary         = TdAccent,
    onPrimary       = Color.White,
    primaryContainer   = TdAccentSoft,
    onPrimaryContainer = TdAccentInk,
    secondary       = TdMuted,
    background      = TdBg,
    surface         = TdSurface,
    onBackground    = TdFg,
    onSurface       = TdFg,
    outline         = TdBorder,
    error           = TdDanger
)

@Composable
fun TrackdayTheme(content: @Composable () -> Unit) {
    val tdColors = TrackDayColors(
        bg = TdBg, surface = TdSurface, fg = TdFg, muted = TdMuted,
        border = TdBorder, accent = TdAccent, accentSoft = TdAccentSoft,
        accentInk = TdAccentInk, learn = TdLearn, rest = TdRest,
        work = TdWork, life = TdLife, danger = TdDanger,
        dangerSoft = TdDangerSoft, dangerBorder = TdDangerBorder
    )
    CompositionLocalProvider(LocalTdColors provides tdColors) {
        MaterialTheme(
            colorScheme = TdColorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
