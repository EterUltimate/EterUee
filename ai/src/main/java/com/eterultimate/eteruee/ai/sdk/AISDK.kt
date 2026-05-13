package com.eterultimate.eteruee.ai.sdk

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.core.TokenUsage
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.CustomHeader
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.ui.UIMessage

/**
 * AI SDK 统一接口
 * 借鉴 Vercel AI SDK v5 的设计理念,提供统一的文本生成、流式处理等能力
 */
interface AISDK {
    /**
     * 生成文本(非流式)
     */
    suspend fun generateText(request: GenerateTextRequest): GenerateTextResult

    /**
     * 流式生成文本
     */
    fun streamText(request: StreamTextRequest): Flow<TextChunk>

    /**
     * 生成结构化对象(可选功能)
     */
    suspend fun generateObject(request: GenerateObjectRequest): JsonObject
}

/**
 * 文本生成请求
 */
@Serializable
data class GenerateTextRequest(
    val model: Model,
    val messages: List<UIMessage>,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    @Transient
    val tools: List<Tool> = emptyList(),
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBody: List<com.eterultimate.eteruee.ai.provider.CustomBody> = emptyList()
)

/**
 * 文本生成结果
 */
@Serializable
data class GenerateTextResult(
    val text: String,
    val usage: TokenUsage?,
    val finishReason: FinishReason?,
    val message: UIMessage
)

/**
 * 流式文本生成请求
 */
@Serializable
data class StreamTextRequest(
    val model: Model,
    val messages: List<UIMessage>,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    @Transient
    val tools: List<Tool> = emptyList(),
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBody: List<com.eterultimate.eteruee.ai.provider.CustomBody> = emptyList()
)

/**
 * 文本块(流式响应单元)
 */
@Serializable
sealed interface TextChunk {
    @Serializable
    data class TextDelta(val text: String) : TextChunk
    @Serializable
    data class ToolCall(val toolCallId: String, val toolName: String, val arguments: String) : TextChunk
    @Serializable
    data class Usage(val tokenUsage: TokenUsage) : TextChunk
    @Serializable
    object Finish : TextChunk
}

/**
 * 完成原因
 */
@Serializable
enum class FinishReason {
    STOP,           // 自然停止
    LENGTH,         // 达到最大长度
    TOOL_CALLS,     // 工具调用
    ERROR,          // 错误
    OTHER           // 其他
}

/**
 * 结构化对象生成请求
 */
@Serializable
data class GenerateObjectRequest(
    val model: Model,
    val messages: List<UIMessage>,
    val schema: JsonObject,
    val temperature: Float? = null
)
