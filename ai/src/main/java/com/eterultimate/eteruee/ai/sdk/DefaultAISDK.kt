package com.eterultimate.eteruee.ai.sdk

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.core.TokenUsage
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.provider.ProviderSetting
import com.eterultimate.eteruee.ai.provider.TextGenerationParams
import com.eterultimate.eteruee.ai.ui.MessageChunk
import com.eterultimate.eteruee.ai.ui.UIMessage

private const val TAG = "DefaultAISDK"

/**
 * AISDK 默认实现
 * 封装现有的 Provider 接口,提供统一的 API
 */
class DefaultAISDK(
    private val providerManager: ProviderManager,
    private val providerSetting: ProviderSetting
) : AISDK {

    override suspend fun generateText(request: GenerateTextRequest): GenerateTextResult {
        Log.d(TAG, "generateText: model=${request.model.modelId}, messages=${request.messages.size}")

        try {
            val params = TextGenerationParams(
                model = request.model,
                temperature = request.temperature,
                topP = request.topP,
                maxTokens = request.maxTokens,
                tools = request.tools,
                customHeaders = request.customHeaders,
                customBody = request.customBody
            )

            val provider = providerManager.getProviderByType(providerSetting)
            val result = provider.generateText(
                providerSetting,
                request.messages,
                params
            )

            // 从 MessageChunk 提取文本
            val text = result.choices.firstOrNull()?.message?.parts
                ?.filterIsInstance<com.eterultimate.eteruee.ai.ui.UIMessagePart.Text>()
                ?.joinToString("") { it.text } ?: ""

            val finishReason = when (result.choices.firstOrNull()?.finishReason) {
                "stop" -> FinishReason.STOP
                "length" -> FinishReason.LENGTH
                "tool_calls" -> FinishReason.TOOL_CALLS
                else -> FinishReason.OTHER
            }

            // 构建完整的 UIMessage
            val message = UIMessage(
                role = com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT,
                parts = result.choices.firstOrNull()?.message?.parts ?: emptyList(),
                usage = result.usage
            )

            return GenerateTextResult(
                text = text,
                usage = result.usage,
                finishReason = finishReason,
                message = message
            )
        } catch (e: Exception) {
            Log.e(TAG, "generateText error", e)
            throw AISDKException("Failed to generate text: ${e.message}", e)
        }
    }

    override fun streamText(request: StreamTextRequest): Flow<TextChunk> {
        Log.d(TAG, "streamText: model=${request.model.modelId}, messages=${request.messages.size}")

        val params = TextGenerationParams(
            model = request.model,
            temperature = request.temperature,
            topP = request.topP,
            maxTokens = request.maxTokens,
            tools = request.tools,
            customHeaders = request.customHeaders,
            customBody = request.customBody
        )

        // streamText 是 suspend 函数,需要在协程中调用
        return kotlinx.coroutines.flow.flow {
            val provider = providerManager.getProviderByType(providerSetting)
            val flow = provider.streamText(
                providerSetting,
                request.messages,
                params
            )
            flow.collect { chunk ->
                emit(chunk)
            }
        }
            .map { chunk -> chunk.toTextChunk() }
            .catch { e ->
                Log.e(TAG, "streamText error", e)
                throw AISDKException("Stream failed: ${e.message}", e)
            }
    }

    override suspend fun generateObject(request: GenerateObjectRequest): JsonObject {
        // TODO: 实现结构化对象生成
        // 当前版本暂不支持,可以后续通过 function calling 或 JSON mode 实现
        throw UnsupportedOperationException("generateObject is not yet implemented")
    }

}

/**
 * 将 MessageChunk 转换为 TextChunk
 */
internal fun MessageChunk.toTextChunk(): TextChunk {
    // 检查是否有 usage 信息
    usage?.let {
        return TextChunk.Usage(it)
    }

    // 检查是否有 finish reason
    if (choices.any { it.finishReason != null }) {
        return TextChunk.Finish
    }

    // 提取文本增量
    val textDeltas = choices.flatMap { choice ->
        choice.delta?.parts?.filterIsInstance<com.eterultimate.eteruee.ai.ui.UIMessagePart.Text>() ?: emptyList()
    }

    if (textDeltas.isNotEmpty()) {
        return TextChunk.TextDelta(textDeltas.joinToString("") { it.text })
    }

    // 提取工具调用
    val toolCalls = choices.flatMap { choice ->
        choice.delta?.parts?.filterIsInstance<com.eterultimate.eteruee.ai.ui.UIMessagePart.Tool>() ?: emptyList()
    }

    if (toolCalls.isNotEmpty()) {
        val toolCall = toolCalls.first()
        return TextChunk.ToolCall(
            toolCallId = toolCall.toolCallId,
            toolName = toolCall.toolName,
            arguments = toolCall.input
        )
    }

    // 如果没有内容,返回空文本块
    return TextChunk.TextDelta("")
}

/**
 * AI SDK 异常
 */
class AISDKException(message: String, cause: Throwable? = null) : Exception(message, cause)
