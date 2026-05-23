package com.eterultimate.eteruee.ui.theme

import androidx.compose.ui.graphics.Color
import com.eterultimate.eteruee.highlight.HighlightTextColorPalette

// High-contrast code highlight colors for dark surfaces.
val AtomOneDarkPalette = HighlightTextColorPalette(
    keyword = Color(0xFFFF0000),     // Red
    string = Color(0xFF0000FF),      // Blue
    number = Color(0xFF0000FF),      // Blue
    comment = Color(0xFF666666),     // Gray
    function = Color(0xFF00FFFF),    // Cyan
    operator = Color(0xFFFF00FF),    // Magenta
    punctuation = Color(0xFFCCCCCC), // Light Gray
    className = Color(0xFFFFFF00),   // Yellow
    property = Color(0xFFFF6600),    // Orange
    boolean = Color(0xFF0000FF),     // Blue
    variable = Color(0xFFFF6600),    // Orange
    tag = Color(0xFFFF0000),         // Red
    attrName = Color(0xFF0000FF),    // Blue
    attrValue = Color(0xFFFFFF00),   // Yellow
    fallback = Color(0xFFFFFFFF)     // White
)

// Reuse the high-contrast palette as a fallback for light surfaces.
val AtomOneLightPalette = AtomOneDarkPalette
