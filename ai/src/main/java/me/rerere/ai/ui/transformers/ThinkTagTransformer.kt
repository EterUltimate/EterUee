package me.rerere.ai.ui.transformers

import android.content.Context
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

private val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)

object ThinkTagTransformer : MessageTransformer {
    override fun transform(
        context: Context,
        messages: List<UIMessage>,
        model: Model
    ): List<UIMessage> {
        return messages.map { message ->
            if(message.role == MessageRole.ASSISTANT && message.hasPart<UIMessagePart.Text>()) {
                message.copy(
                    parts = message.parts.flatMap { part ->
                        if(part is UIMessagePart.Text && part.text.startsWith("<think>")) {
                            // 提取 <think> 中的内容，并替换为空字串
                            val stripped = part.text.replace(THINKING_REGEX, "")
                            val reasoning = THINKING_REGEX.find(part.text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                            listOf(
                                UIMessagePart.Reasoning(reasoning),
                                part.copy(text = stripped),
                            )
                        } else {
                            listOf(part)
                        }
                    }
                )
            } else {
                message
            }
        }
    }
}