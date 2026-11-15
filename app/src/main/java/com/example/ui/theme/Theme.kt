package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioViolet,
    onPrimary = TextPrimary,
    primaryContainer = NavyElevated,
    onPrimaryContainer = StudioVioletLight,
    secondary = StudioElectricBlue,
    onSecondary = TextPrimary,
    secondaryContainer = NavyHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = StudioCyan,
    onTertiary = NavyAbyss,
    background = NavyAbyss,
    onBackground = TextPrimary,
    surface = NavyDeep,
    onSurface = TextPrimary,
    surfaceVariant = NavySurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderStrong,
    error = StatusError,
    onError = TextPrimary
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object StudioGradients {
    val PrimaryAction = Brush.horizontalGradient(
        colors = listOf(StudioViolet, StudioElectricBlue, StudioCyan)
    )
    val VioletBlue = Brush.linearGradient(
        colors = listOf(StudioViolet, StudioElectricBlue)
    )
    val BackgroundGlow = Brush.verticalGradient(
        colors = listOf(NavyDeep, NavyAbyss, Color(0xFF040714))
    )
    val GlassHeader = Brush.horizontalGradient(
        colors = listOf(
            StudioViolet.copy(alpha = 0.28f),
            StudioElectricBlue.copy(alpha = 0.22f),
            StudioCyan.copy(alpha = 0.16f)
        )
    )
}

@Composable
fun AiVideoStudioTheme(
    accentStyle: String = "violet_blue",
    content: @Composable () -> Unit
) {
    val primaryColor = when (accentStyle) {
        "electric_cyan" -> StudioCyan
        "neon_magenta" -> StudioMagenta
        else -> StudioViolet
    }
    val scheme = StudioDarkColorScheme.copy(
        primary = primaryColor
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
