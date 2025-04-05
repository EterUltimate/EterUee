package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.ui.components.MarkdownBlock

@Composable
fun ChatMessage(
    message: UIMessage,
    modifier: Modifier = Modifier
) {
    when(message.role) {
        MessageRole.USER -> {
            Card {
                Column(
                    modifier = modifier.padding(4.dp)
                ) {
                    message.parts.forEach {
                        when (it) {
                            is UIMessagePart.Reasoning -> {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(4.dp)
                                ) {
                                    MarkdownBlock(it.reasoning, Modifier.fillMaxWidth())
                                }
                            }

                            is UIMessagePart.Text -> MarkdownBlock(it.text, Modifier.fillMaxWidth())
                            else -> {
                                // DO NOTHING
                            }
                        }
                    }
                }
            }
        }

        MessageRole.ASSISTANT -> {
            Column(
                modifier = modifier.padding(4.dp)
            ) {
                message.parts.forEach {
                    when (it) {
                        is UIMessagePart.Reasoning -> {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(4.dp)
                            ) {
                                MarkdownBlock(it.reasoning, Modifier.fillMaxWidth())
                            }
                        }

                        is UIMessagePart.Text -> MarkdownBlock(it.text, Modifier.fillMaxWidth())
                        else -> {
                            // DO NOTHING
                        }
                    }
                }
            }
        }

        else -> {}
    }
}