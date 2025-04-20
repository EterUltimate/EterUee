package me.rerere.ai.ui.transformers

import me.rerere.ai.provider.Model
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import java.time.LocalDateTime
import java.util.Locale
import java.util.TimeZone

object PlaceholderTransformer : MessageTransformer {
    val Placeholders = listOf(
        "{cur_date}",
        "{cur_time}",
        "{model_id}",
        "{model_name}",
        "{locale}",
        "{timezone}"
    )

    override fun transform(messages: List<UIMessage>, model: Model): List<UIMessage> {
        return messages.map {
            it.copy(
                parts = it.parts.map { part ->
                    if (part is UIMessagePart.Text) {
                        part.copy(
                            text = part.text
                                .replace("{cur_date}", LocalDateTime.now().toString())
                                .replace("{cur_time}", LocalDateTime.now().toString())
                                .replace("{model_id}", model.modelId)
                                .replace("{model_name}", model.displayName)
                                .replace("{locale}", Locale.getDefault().displayName)
                                .replace("{timezone}", TimeZone.getDefault().displayName)
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }
}