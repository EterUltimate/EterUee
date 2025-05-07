package me.rerere.ai.ui.transformers

import android.content.Context
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

object MessageTimeTransformer : MessageTransformer {
    override fun transform(
        context: Context,
        messages: List<UIMessage>,
        model: Model
    ): List<UIMessage> {
        return messages.map { message ->
            if(message.role == MessageRole.USER) {
                message.copy(
                    parts = message.parts.map { part ->
                        if(part is UIMessagePart.Text) {
                            part.copy(
                                text = part.text + "\n(send at ${message.createdAt.format(LocalDateTime.Formats.ISO)})"
                            )
                        } else {
                            part
                        }
                    }
                )
            } else {
                message
            }
        }
    }
}