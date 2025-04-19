package me.rerere.ai.ui.transformers

import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import java.time.LocalDateTime

object PlaceholderTransformer : MessageTransformer {
    override fun transform(messages: List<UIMessage>): List<UIMessage> {
        return messages.map {
            it.copy(
                parts = it.parts.map { part ->
                    if(part is UIMessagePart.Text) {
                        part.copy(
                            text = part.text
                                .replace("{cur_date}", LocalDateTime.now().toString())
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }
}