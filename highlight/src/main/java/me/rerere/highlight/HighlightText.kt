package me.rerere.highlight

import android.annotation.SuppressLint
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.util.fastForEach

val LocalHighlighter = compositionLocalOf<Highlighter> { error("No Highlighter provided") }

@Composable
fun HighlightText(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val highlighter = LocalHighlighter.current
    var tokens: List<HighlightToken> by remember {  mutableStateOf(emptyList()) }

    LaunchedEffect(code, language) {
        tokens =
            highlighter.highlight(code, language).getOrNull() ?: listOf(HighlightToken.Plain(code))
        println("tokens = $tokens")
    }

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            tokens.fastForEach { token ->
                when (token) {
                    is HighlightToken.Plain -> {
                        append(token.content)
                    }

                    is HighlightToken.Token -> {
                        withStyle(getStyleForTokenType(token.type)) {
                            append(token.content)
                        }
                    }
                }
            }
        }
    )
}

// 根据token类型返回对应的文本样式
@Composable
private fun getStyleForTokenType(type: String): SpanStyle {
    return when (type) {
        "keyword" -> SpanStyle(color = MaterialTheme.colorScheme.primary)
        "string" -> SpanStyle(color = Color(0xFF6A8759)) // 绿色
        "number" -> SpanStyle(color = Color(0xFF6897BB)) // 蓝色
        "comment" -> SpanStyle(color = Color(0xFF808080), fontStyle = FontStyle.Italic) // 灰色斜体
        "function" -> SpanStyle(color = Color(0xFFFFC66D)) // 黄色
        "operator" -> SpanStyle(color = Color(0xFFCC7832)) // 橙色
        "punctuation" -> SpanStyle(color = Color(0xFFCC7832)) // 橙色
        "class-name", "property" -> SpanStyle(color = Color(0xFFCB772F)) // 棕色
        "boolean" -> SpanStyle(color = Color(0xFF6897BB)) // 蓝色
        "variable" -> SpanStyle(color = MaterialTheme.colorScheme.onBackground)
        "tag" -> SpanStyle(color = Color(0xFFE8BF6A)) // 黄色
        "attr-name" -> SpanStyle(color = Color(0xFFBABABA)) // 浅灰色
        "attr-value" -> SpanStyle(color = Color(0xFF6A8759)) // 绿色
        else -> SpanStyle(color = MaterialTheme.colorScheme.onBackground) // 默认颜色
    }
}
