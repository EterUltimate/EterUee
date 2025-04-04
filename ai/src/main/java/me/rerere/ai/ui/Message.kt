package me.rerere.ai.ui

import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import kotlin.uuid.Uuid

// 公共消息抽象, 具体的Provider实现会转换为API接口需要的DTO
@Serializable
data class UIMessage(
    val modelId: Uuid,
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
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>,
)

@Serializable
data class UIMessageChoice(
    val index: Int,
    val delta: UIMessageContent?,
    val message: UIMessageContent?,
    val finishReason: String?
)