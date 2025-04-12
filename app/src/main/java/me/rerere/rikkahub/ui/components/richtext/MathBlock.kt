package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import icu.ketal.katexmath.KaTeXMath

@Composable
fun MathInline(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    KaTeXMath(
        latex = latex,
        style = LocalTextStyle.current.merge(
            color = LocalContentColor.current,
            fontSize = fontSize.takeOrElse { LocalTextStyle.current.fontSize },
        ),
        modifier = modifier,
    )
}

@Composable
fun MathBlock(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    Box(
        modifier = modifier.padding(8.dp)
    ) {
        KaTeXMath(
            latex = latex,
            style = LocalTextStyle.current.merge(
                color = LocalContentColor.current,
                fontSize = fontSize.takeOrElse { MaterialTheme.typography.bodyLarge.fontSize },
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .horizontalScroll(
                    rememberScrollState()
                ),
        )
    }
}