package me.rerere.ai.ui

import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole

@Serializable
data class UIMessage(
    val role: MessageRole,
    val content: List<UIMessageContent>
)

@Serializable
sealed class UIMessageContent {
    @Serializable
    data class Text(val text: String) : UIMessageContent()

    @Serializable
    data class Image(val url: String) : UIMessageContent()

    @Serializable
    data class Reasoning(val reasoning: String) : UIMessageContent()
}

@Serializable
data class UIMessageChoice(
    val index: Int,
    val delta: UIMessageContent?,
    val message: UIMessageContent?,
    val finishReason: String?
)