package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FuturisticCyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberObsidian,
    primaryContainer = CyberDarkSurface,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCobalt,
    onSecondary = TechWhite,
    secondaryContainer = CyberCardSurface,
    onSecondaryContainer = TechWhite,
    tertiary = NeonAqua,
    onTertiary = CyberObsidian,
    background = CyberObsidian,
    onBackground = TechWhite,
    surface = CyberDarkSurface,
    onSurface = TechWhite,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TechTextSecondary,
    outline = CyberCardBorder,
    error = TechError,
    onError = TechWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FuturisticCyberColorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for semantic clarity
@Composable
fun NexusTheme(
    content: @Composable () -> Unit
) {
    MyApplicationTheme(content = content)
}
