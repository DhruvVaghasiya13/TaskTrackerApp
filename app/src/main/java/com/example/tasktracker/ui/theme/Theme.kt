package com.example.tasktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightPaper = Color(0xFFF6F4EE)
val LightCard = Color(0xFFFFFFFF)
val LightInk = Color(0xFF1C231F)
val LightLine = Color(0xFFDCD8CA)
val LightAccent = Color(0xFF2F6F5E)
val LightAccentSoft = Color(0xFFDCECE5)
val LightMuted = Color(0xFF8B8677)

val DarkPaper = Color(0xFF15171A)
val DarkCard = Color(0xFF1F2320)
val DarkInk = Color(0xFFECE9E1)
val DarkLine = Color(0xFF33372F)
val DarkAccent = Color(0xFF6FBFA4)
val DarkAccentSoft = Color(0xFF23332C)
val DarkMuted = Color(0xFF8B8677)

val ColorHigh = Color(0xFFB0503F)
val ColorMed = Color(0xFFC08A2E)
val ColorLow = Color(0xFF4C7A4A)

data class ExtraColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val muted: Color,
    val high: Color = ColorHigh,
    val med: Color = ColorMed,
    val low: Color = ColorLow
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(
        paper = LightPaper,
        card = LightCard,
        ink = LightInk,
        line = LightLine,
        accent = LightAccent,
        accentSoft = LightAccentSoft,
        muted = LightMuted
    )
}

@Composable
fun TaskTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extraColors = if (darkTheme) {
        ExtraColors(
            paper = DarkPaper,
            card = DarkCard,
            ink = DarkInk,
            line = DarkLine,
            accent = DarkAccent,
            accentSoft = DarkAccentSoft,
            muted = DarkMuted
        )
    } else {
        ExtraColors(
            paper = LightPaper,
            card = LightCard,
            ink = LightInk,
            line = LightLine,
            accent = LightAccent,
            accentSoft = LightAccentSoft,
            muted = LightMuted
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = DarkAccent,
            secondary = DarkAccentSoft,
            background = DarkPaper,
            surface = DarkCard,
            onPrimary = Color.Black,
            onBackground = DarkInk,
            onSurface = DarkInk,
            outline = DarkLine
        )
    } else {
        lightColorScheme(
            primary = LightAccent,
            secondary = LightAccentSoft,
            background = LightPaper,
            surface = LightCard,
            onPrimary = Color.White,
            onBackground = LightInk,
            onSurface = LightInk,
            outline = LightLine
        )
    }

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

val MaterialTheme.extraColors: ExtraColors
    @Composable
    get() = LocalExtraColors.current
