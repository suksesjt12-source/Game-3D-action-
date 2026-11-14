package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AetherfallColorScheme = darkColorScheme(
    primary = AetherCyan,
    onPrimary = ObsidianVoid,
    secondary = RunicGold,
    onSecondary = ObsidianVoid,
    tertiary = ArcaneViolet,
    background = ObsidianVoid,
    onBackground = ParchmentText,
    surface = ObsidianSurface,
    onSurface = ParchmentText,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = MutedSlateText,
    outline = ObsidianBorder,
    error = CrimsonHealth,
    onError = ParchmentText
)

@Composable
fun AetherfallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherfallColorScheme,
        typography = AetherfallTypography,
        content = content
    )
}
