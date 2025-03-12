package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rerere.highlight.HighlightText
import me.rerere.rikkahub.ui.theme.AtomOneDarkPalette
import me.rerere.rikkahub.ui.theme.AtomOneLightPalette
import me.rerere.rikkahub.ui.theme.LocalDarkMode

@Composable
fun HighlightCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val darkMode = LocalDarkMode.current
    val colorPalette = if (darkMode) AtomOneDarkPalette else AtomOneLightPalette
    val scrollState = rememberScrollState()

    SelectionContainer {
        HighlightText(
            code = code,
            language = language,
            modifier = modifier
                .horizontalScroll(scrollState)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            colors = colorPalette,
            overflow = TextOverflow.Visible,
            softWrap = false,
        )
    }
}