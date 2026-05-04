package com.eterultimate.eteruee.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ui.theme.PresetTheme

val CyberpunkThemePreset by lazy {
    PresetTheme(
        id = "cyberpunk",
        name = {
            Text(stringResource(id = R.string.theme_name_cyberpunk))
        },
        // Light scheme is identical to dark — cyberpunk is always dark
        standardLight = darkScheme,
        standardDark = darkScheme,
    )
}

// === CYBERPUNK INDUSTRIAL COLOR SYSTEM ===
// RGB Pure Colors + Pure Black + Pure White

// Primary: RGB Pure Red #FF0000
private val CyberRed = Color(0xFFFF0000)
// Secondary: RGB Pure Green #00FF00
private val CyberGreen = Color(0xFF00FF00)
// Tertiary: RGB Pure Blue #0000FF
private val CyberBlue = Color(0xFF0000FF)
// Background: Pure Black #000000
private val PureBlack = Color(0xFF000000)
// Text & Borders: Pure White #FFFFFF
private val PureWhite = Color(0xFFFFFFFF)
// Error: Pure Red (same as primary, sharp)
private val CyberError = Color(0xFFFF0000)
// Surface containers: subtle gradations from pure black
private val SurfaceLowest = Color(0xFF000000)
private val SurfaceLow = Color(0xFF050505)
private val SurfaceContainer = Color(0xFF0A0A0A)
private val SurfaceHigh = Color(0xFF0F0F0F)
private val SurfaceHighest = Color(0xFF141414)
private val SurfaceDim = Color(0xFF000000)
private val SurfaceBright = Color(0xFF1A1A1A)
// Outline: Pure White for borders
private val CyberOutline = Color(0xFFFFFFFF)
private val CyberOutlineVariant = Color(0xFF808080)

private val darkScheme = darkColorScheme(
    primary = CyberRed,
    onPrimary = PureWhite,
    primaryContainer = CyberRed.copy(alpha = 0.15f),
    onPrimaryContainer = CyberRed,
    secondary = CyberGreen,
    onSecondary = PureBlack,
    secondaryContainer = CyberGreen.copy(alpha = 0.15f),
    onSecondaryContainer = CyberGreen,
    tertiary = CyberBlue,
    onTertiary = PureWhite,
    tertiaryContainer = CyberBlue.copy(alpha = 0.15f),
    onTertiaryContainer = CyberBlue,
    error = CyberError,
    onError = PureWhite,
    errorContainer = CyberError.copy(alpha = 0.15f),
    onErrorContainer = CyberError,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = PureWhite,
    outline = CyberOutline,
    outlineVariant = CyberOutlineVariant,
    scrim = PureBlack,
    inverseSurface = PureWhite,
    inverseOnSurface = PureBlack,
    inversePrimary = CyberRed,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
)
