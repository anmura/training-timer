package com.trainingtimer.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object C {
    val Bg = Color(0xFF0F1113)
    val Surface = Color(0xFF1A1D21)
    val Surface2 = Color(0xFF23272C)
    val Surface3 = Color(0xFF2F343A)
    val NavBg = Color(0xFF15181B)
    val Line = Color(0xFF2E3338)
    val Text = Color(0xFFF2F3F1)
    val Muted = Color(0xFFA3A9B0)
    val Faint = Color(0xFF5A6068)
    val Lime = Color(0xFFC8F04A)
    val OnLime = Color(0xFF12140F)
    val LimeDim = Color(0xFF34410F)
    val LimeBg = Color(0xFF1E2412)
    val LimeBorder = Color(0xFF6E8A22)
    val CycleBg = Color(0xFF161A10)
    val Orange = Color(0xFFFF9F43)
    val OnOrange = Color(0xFF1A120A)
    val RestBg = Color(0xFF120F0C)
    val RestSurface = Color(0xFF1C1915)
    val RestSurface2 = Color(0xFF2A241D)
    val RestLine = Color(0xFF3A2E22)
    val RestMuted = Color(0xFFB3A99F)
    val Red = Color(0xFFFF7A7A)
    /** Reps sets (counted, not timed). */
    val Sky = Color(0xFF6CC4FF)
    val SkyTrack = Color(0xFF1D2A33)
    /** Background of alarm badges and warnings. */
    val AlarmBg = Color(0xFF3A2A18)
}

/** Condensed bold system font, used for big numbers and display titles. */
val Condensed: FontFamily = FontFamily(
    android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Lime,
            onPrimary = C.OnLime,
            primaryContainer = C.LimeDim,
            onPrimaryContainer = C.Lime,
            secondary = C.Orange,
            onSecondary = C.OnOrange,
            background = C.Bg,
            onBackground = C.Text,
            surface = C.Surface,
            onSurface = C.Text,
            surfaceVariant = C.Surface2,
            onSurfaceVariant = C.Muted,
            surfaceContainerLowest = C.Bg,
            surfaceContainerLow = C.Surface,
            surfaceContainer = C.Surface,
            surfaceContainerHigh = C.Surface,
            surfaceContainerHighest = C.Surface2,
            outline = C.Line,
            error = C.Red,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified),
            content = content,
        )
    }
}
