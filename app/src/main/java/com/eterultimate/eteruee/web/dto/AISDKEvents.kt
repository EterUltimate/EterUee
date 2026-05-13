package com.eterultimate.eteruee.web.dto

import kotlinx.serialization.Serializable

/**
 * AI SDK v5 标准 SSE 事件格式
 * 
 * 参考: https://sdk.vercel.ai/docs/advanced/streaming
 */

/**
 * 文本增量事件 - 流式文本生成的每个片段
 */
@Serializable
data class TextDeltaEvent(
    val textDelta: String
)

/**
 * 工具调用开始事件
 */
@Serializable
data class ToolCallStartEvent(
    val toolCallId: String,
    val toolName: String,
    val args: Map<String, Any>
)

/**
 * 工具调用结果事件
 */
@Serializable
data class ToolCallResultEvent(
    val toolCallId: String,
    val result: String
)

/**
 * 使用量统计事件
 */
@Serializable
data class UsageEvent(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * 完成事件 - 标记生成结束
 */
@Serializable
data class FinishEvent(
    val finishReason: String, // "stop" | "length" | "content_filter" | "tool_calls"
    val usage: UsageEvent? = null
)

/**
 * 错误事件
 */
@Serializable
data class StandardErrorEvent(
    val error: String,
    val code: String? = null
)

/**
 * 元数据事件 - 可选的额外信息
 */
@Serializable
data class MetadataEvent(
    val messageId: String? = null,
    val modelId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
