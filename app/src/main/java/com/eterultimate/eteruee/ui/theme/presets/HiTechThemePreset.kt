package com.eterultimate.eteruee.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ui.theme.HiTechThemeId
import com.eterultimate.eteruee.ui.theme.PresetTheme

val HiTechThemePreset by lazy {
    PresetTheme(
        id = HiTechThemeId,
        name = {
            Text(stringResource(id = R.string.theme_name_hitech))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF2E6CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE6FF),
    onPrimaryContainer = Color(0xFF082A68),
    secondary = Color(0xFF52627A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7F5),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF18898F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7F3F5),
    onTertiaryContainer = Color(0xFF002024),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF171B22),
    surface = Color(0xFFFAFBFF),
    onSurface = Color(0xFF171B22),
    surfaceVariant = Color(0xFFE2E7F2),
    onSurfaceVariant = Color(0xFF444A57),
    outline = Color(0xFF747B88),
    outlineVariant = Color(0xFFC4CAD6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F6FF),
    surfaceContainer = Color(0xFFEAF0FB),
    surfaceContainerHigh = Color(0xFFE4EAF5),
    surfaceContainerHighest = Color(0xFFDDE4F0),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFD8DEEA),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF9DB7FF),
    onPrimary = Color(0xFF002F6D),
    primaryContainer = Color(0xFF1C4EAD),
    onPrimaryContainer = Color(0xFFDDE6FF),
    secondary = Color(0xFFC0C7DB),
    onSecondary = Color(0xFF2B3140),
    secondaryContainer = Color(0xFF414858),
    onSecondaryContainer = Color(0xFFE0E7F5),
    tertiary = Color(0xFF8AD5DA),
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF005055),
    onTertiaryContainer = Color(0xFFC7F3F5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF111827),
    onBackground = Color(0xFFE5EAF4),
    surface = Color(0xFF131B2A),
    onSurface = Color(0xFFE5EAF4),
    surfaceVariant = Color(0xFF444A57),
    onSurfaceVariant = Color(0xFFC4CAD6),
    outline = Color(0xFF8E95A2),
    outlineVariant = Color(0xFF444A57),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE5EAF4),
    inverseOnSurface = Color(0xFF263140),
    inversePrimary = Color(0xFF2E6CF6),
    surfaceDim = Color(0xFF111827),
    surfaceBright = Color(0xFF384354),
    surfaceContainerLowest = Color(0xFF0C111C),
    surfaceContainerLow = Color(0xFF171F2F),
    surfaceContainer = Color(0xFF1B2435),
    surfaceContainerHigh = Color(0xFF263145),
    surfaceContainerHighest = Color(0xFF303B50),
)
