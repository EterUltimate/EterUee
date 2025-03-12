package me.rerere.rikkahub.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import icu.ketal.katexmath.KaTeXMath

@Composable
fun MathInlineText(latex: String, modifier: Modifier) {
    KaTeXMath(
        latex = latex,
        style = LocalTextStyle.current.merge(
            color = LocalContentColor.current
        ),
        modifier = modifier,
    )
}
