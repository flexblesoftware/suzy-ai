package com.suzy.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF07080F)
val SurfaceGlass = Color(0xFF121424)
val NeonPurple = Color(0xFFA855F7)
val NeonCyan = Color(0xFF22D3EE)
val NeonPink = Color(0xFFEC4899)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val SuzyDarkColors = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = NeonPink,
    background = BgDark,
    surface = SurfaceGlass,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SuzyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SuzyDarkColors,
        content = content
    )
}
