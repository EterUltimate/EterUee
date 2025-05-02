package me.rerere.rikkahub.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

data class PresetScheme(
    val id: String,
    val name: @Composable () -> Unit,
    val standardLight: ColorScheme,
    val standardDark: ColorScheme,
    val mediumContrastLight: ColorScheme,
    val mediumContrastDark: ColorScheme,
    val highContrastLight: ColorScheme,
    val highContrastDark: ColorScheme,
)