package me.rerere.rikkahub.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.theme.extendColors

@Composable
fun ChatMessage(
    message: UIMessage,
    modifier: Modifier = Modifier
) {

    when (message.role) {
        MessageRole.USER -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Card {
                    Column(
                        modifier = modifier
                            .widthIn(max = 400.dp)
                            .padding(4.dp)
                    ) {
                        MessagePartsBlock(message.parts)
                    }
                }
            }
        }

        MessageRole.ASSISTANT -> {
            Column(
                modifier = modifier.padding(4.dp)
            ) {
                MessagePartsBlock(message.parts)
            }
        }

        else -> {}
    }
}

@Composable
fun MessagePartsBlock(
    parts: List<UIMessagePart>,
    modifier: Modifier = Modifier
) {
    var expandReasoning by remember {
        mutableStateOf(true)
    }
    parts.forEach {
        when (it) {
            is UIMessagePart.Reasoning -> {
                val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Row(
                        modifier = Modifier
                            .clickable { expandReasoning = !expandReasoning }
                            .padding(4.dp)
                    ) {
                        Text("深度思考")
                        Icon(
                            if (expandReasoning) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            null
                        )
                    }
                }
                AnimatedVisibility(expandReasoning) {
                    Box(
                        modifier = Modifier
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = contentColor.copy(alpha = 0.2f),
                                    size = Size(width = 10f, height = size.height),
                                )
                            }
                            .padding(start = 4.dp)
                            .padding(4.dp)
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                            MarkdownBlock(it.reasoning)
                        }
                    }
                }
            }

            is UIMessagePart.Text -> MarkdownBlock(it.text)

            else -> {
                // DO NOTHING
            }
        }
    }
}
