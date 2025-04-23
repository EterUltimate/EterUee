package me.rerere.ai.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.rerere.ai.core.MessageRole
import me.rerere.search.SearchResult
import kotlin.uuid.Uuid

// 公共消息抽象, 具体的Provider实现会转换为API接口需要的DTO
@Serializable
data class UIMessage(
    val id: Uuid = Uuid.random(),
    val role: MessageRole,
    val parts: List<UIMessagePart>,
    val annotations: List<UIMessageAnnotation> = emptyList(),
) {
    private fun appendChunk(chunk: MessageChunk): UIMessage {
        val choice = chunk.choices[0]
        return choice.delta?.let { delta ->
            // Handle Parts
            val newParts = delta.parts.fold(parts) { acc, deltaPart ->
                when (deltaPart) {
                    is UIMessagePart.Text -> {
                        val existingTextPart =
                            acc.find { it is UIMessagePart.Text } as? UIMessagePart.Text
                        if (existingTextPart != null) {
                            acc.map { part ->
                                if (part is UIMessagePart.Text) {
                                    UIMessagePart.Text(existingTextPart.text + deltaPart.text)
                                } else part
                            }
                        } else {
                            acc + UIMessagePart.Text(deltaPart.text)
                        }
                    }

                    is UIMessagePart.Reasoning -> {
                        val existingReasoningPart =
                            acc.find { it is UIMessagePart.Reasoning } as? UIMessagePart.Reasoning
                        if (existingReasoningPart != null) {
                            acc.map { part ->
                                if (part is UIMessagePart.Reasoning) {
                                    UIMessagePart.Reasoning(existingReasoningPart.reasoning + deltaPart.reasoning)
                                } else part
                            }
                        } else {
                            acc + UIMessagePart.Reasoning(deltaPart.reasoning)
                        }
                    }

                    is UIMessagePart.Search -> {
                        val existingSearchPart =
                            acc.find { it is UIMessagePart.Search } as? UIMessagePart.Search
                        if (existingSearchPart != null) {
                            acc.map { part ->
                                if (part is UIMessagePart.Search) {
                                    UIMessagePart.Search(
                                        existingSearchPart.search.copy(
                                            items = existingSearchPart.search.items + deltaPart.search.items
                                        )
                                    )
                                } else part
                            }
                        } else {
                            acc + UIMessagePart.Search(deltaPart.search)
                        }
                    }

                    is UIMessagePart.ToolCall -> {
                        if(deltaPart.toolCallId.isBlank()) {
                            val lastToolCall = acc.lastOrNull { it is UIMessagePart.ToolCall } as? UIMessagePart.ToolCall
                            if(lastToolCall == null || lastToolCall.toolCallId.isBlank()) {
                                acc + UIMessagePart.ToolCall(
                                    toolCallId = deltaPart.toolCallId,
                                    toolName = deltaPart.toolName,
                                    arguments = deltaPart.arguments
                                )
                            } else {
                                acc.map { part ->
                                    if (part == lastToolCall && part is UIMessagePart.ToolCall) {
                                        part.merge(deltaPart)
                                    } else part
                                }
                            }
                        } else {
                            // insert or update
                            val existsPart = acc.find {
                                it is UIMessagePart.ToolCall && it.toolCallId == deltaPart.toolCallId
                            } as? UIMessagePart.ToolCall
                            if (existsPart == null) {
                                // insert
                                acc + UIMessagePart.ToolCall(
                                    toolCallId = deltaPart.toolCallId,
                                    toolName = deltaPart.toolName,
                                    arguments = deltaPart.arguments
                                )
                            } else {
                                // update
                                acc.map { part ->
                                    if (part is UIMessagePart.ToolCall && part.toolCallId == deltaPart.toolCallId) {
                                        part.merge(deltaPart)
                                    } else part
                                }
                            }
                        }
                    }

                    else -> {
                        println("delta part append not supported: $deltaPart")
                        acc
                    }
                }
            }
            // Handle annotations
            val newAnnotations = delta.annotations.ifEmpty {
                annotations
            }
            copy(
                parts = newParts,
                annotations = newAnnotations,
            )
        } ?: this
    }

    fun summaryAsText(): String {
        return "[${role.name}]: " + parts.joinToString(separator = "\n") { part ->
            when (part) {
                is UIMessagePart.Text -> part.text
                else -> ""
            }
        }
    }

    fun toText() = parts.joinToString(separator = "\n") { part ->
        when (part) {
            is UIMessagePart.Text -> part.text
            else -> ""
        }
    }

    fun getToolCalls() = parts.filterIsInstance<UIMessagePart.ToolCall>()
    fun getToolResults() = parts.filterIsInstance<UIMessagePart.ToolResult>()

    fun isValidToUpload() = parts.any {
        it !is UIMessagePart.Search && it !is UIMessagePart.Reasoning
    }

    inline fun <reified P : UIMessagePart> hasPart(): Boolean {
        return parts.any {
            it is P
        }
    }

    operator fun plus(chunk: MessageChunk): UIMessage {
        return this.appendChunk(chunk)
    }

    companion object {
        fun system(prompt: String) = UIMessage(
            role = MessageRole.SYSTEM,
            parts = listOf(UIMessagePart.Text(prompt))
        )

        fun user(prompt: String) = UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text(prompt))
        )
    }
}

fun List<UIMessage>.handleMessageChunk(chunk: MessageChunk): List<UIMessage> {
    require(this.isNotEmpty()) {
        "messages must not be empty"
    }
    val choice = chunk.choices[0]
    val message = choice.delta ?: choice.message ?: throw Exception("delta/message is null")
    if (this.last().role != message.role) {
        return this + message
    } else {
        val last = this.last() + chunk
        return this.dropLast(1) + last
    }
}

fun List<UIMessagePart>.isEmptyMessage(): Boolean {
    if (this.isEmpty()) return true
    return this.all { message ->
        when (message) {
            is UIMessagePart.Text -> message.text.isBlank()
            is UIMessagePart.Image -> message.url.isBlank()
            else -> false
        }
    }
}

fun List<UIMessagePart>.searchTextContent(): String {
    return buildString {
        for (part in this@searchTextContent) {
            when (part) {
                is UIMessagePart.Search -> {
                    part.search.items.forEachIndexed { index, item ->
                        append("<search_item>\n")
                        append("<index>${index + 1}</index>\n")
                        append("<title>${item.title}</title>\n")
                        append("<content>${item.text}</content>\n")
                        append("<url>${item.url}</url>\n")
                        append("</search_item>\n")
                    }
                }

                else -> {}
            }
        }
    }
}

@Serializable
sealed class UIMessagePart {
    @Serializable
    data class Text(val text: String) : UIMessagePart()

    @Serializable
    data class Image(val url: String) : UIMessagePart()

    @Serializable
    data class Reasoning(val reasoning: String) : UIMessagePart()

    @Serializable
    data class Search(val search: SearchResult) : UIMessagePart()

    @Serializable
    data class ToolCall(
        val toolCallId: String,
        val toolName: String,
        val arguments: String,
    ) : UIMessagePart() {
        fun merge(other: ToolCall): ToolCall {
            return ToolCall(
                toolCallId = toolCallId + other.toolCallId,
                toolName = toolName + other.toolName,
                arguments = arguments + other.arguments
            )
        }
    }

    @Serializable
    data class ToolResult(
        val toolCallId: String,
        val toolName: String,
        val content: JsonElement,
    ): UIMessagePart()
}

@Serializable
sealed class UIMessageAnnotation {
    @Serializable
    @SerialName("url_citation")
    data class UrlCitation(
        val title: String,
        val url: String
    ) : UIMessageAnnotation()
}

@Serializable
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>
)

@Serializable
data class UIMessageChoice(
    val index: Int,
    val delta: UIMessage?,
    val message: UIMessage?,
    val finishReason: String?
)