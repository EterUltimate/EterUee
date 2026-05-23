package com.eterultimate.eteruee.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.eterultimate.eteruee.ui.theme.presets.ChatGptThemePreset
import com.eterultimate.eteruee.ui.theme.presets.ClaudeThemePreset
import com.eterultimate.eteruee.ui.theme.presets.GeminiThemePreset
import com.eterultimate.eteruee.ui.theme.presets.HiTechThemePreset
import com.eterultimate.eteruee.ui.theme.presets.MinimalThemePreset
import com.eterultimate.eteruee.ui.theme.presets.SoftGlassThemePreset

data class PresetTheme(
    val id: String,
    val name: @Composable () -> Unit,
    val standardLight: ColorScheme,
    val standardDark: ColorScheme,
) {
    fun getColorScheme(dark: Boolean): ColorScheme {
        return if (dark) standardDark else standardLight
    }
}

private const val LegacyCyberpunkThemeId = "cyberpunk"
const val HiTechThemeId = "hitech"

val PresetThemes by lazy {
    listOf(
        SoftGlassThemePreset,
        MinimalThemePreset,
        ClaudeThemePreset,
        ChatGptThemePreset,
        GeminiThemePreset,
        HiTechThemePreset,
    )
}

fun findPresetTheme(id: String): PresetTheme {
    if (id == LegacyCyberpunkThemeId) return HiTechThemePreset
    return PresetThemes.find { it.id == id } ?: SoftGlassThemePreset
}

fun findThemeById(id: String, customThemes: List<CustomTheme>): PresetTheme? {
    PresetThemes.find { it.id == id }?.let { return it }
    val custom = customThemes.find { it.id == id } ?: return null
    return PresetTheme(
        id = custom.id,
        name = { androidx.compose.material3.Text(custom.name) },
        standardLight = custom.generateColorScheme(dark = false),
        standardDark = custom.generateColorScheme(dark = true),
    )
}

