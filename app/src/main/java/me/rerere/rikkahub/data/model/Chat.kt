package me.rerere.rikkahub.data.model

data class TextGenerationRequest(
    val model: String,
    val messages: List<UIMessage>,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val presencePenalty: Float? = null,
    val frequencyPenalty: Float? = null,
    val additionalProps: Map<String, Any>? = null,
)

enum class UIMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}

data class UIMessage(
    val role: UIMessageRole,
    val content: List<UIMessageContentPart>
)

enum class UIMessageContentType {
    TEXT,
    IMAGE,
    REASONING,
}

sealed class UIMessageContentPart(
    val type: UIMessageContentType
) {
    data class Text(
        val text: String
    ) : UIMessageContentPart(UIMessageContentType.TEXT)

    data class Image(
        val imageUrl: String
    ) : UIMessageContentPart(UIMessageContentType.IMAGE)

    data class Reasoning(
        val reasoning: String
    ) : UIMessageContentPart(UIMessageContentType.REASONING)
}