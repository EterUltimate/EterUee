package com.eterultimate.eteruee.ai.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart

/**
 * 工具执行器接口
 */
interface ToolExecutor {
    /**
     * 执行工具调用
     */
    suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult
}

/**
 * 工具执行结果
 */
data class ToolResult(
    val toolCallId: String,
    val result: String,
    val isError: Boolean = false
)

/**
 * 支持多轮工具调用的流式文本生成
 */
fun AISDK.streamTextWithTools(
    request: StreamTextRequest,
    executor: ToolExecutor
): Flow<TextChunk> = flow {
    val currentMessages = request.messages.toMutableList()
    var continueGenerating = true

    while (continueGenerating) {
        continueGenerating = false
        val toolCalls = mutableListOf<TextChunk.ToolCall>()

        streamText(request.copy(messages = currentMessages)).collect { chunk ->
            when (chunk) {
                is TextChunk.ToolCall -> {
                    toolCalls.add(chunk)
                    emit(chunk)
                }
                is TextChunk.Finish -> {
                    if (toolCalls.isEmpty()) {
                        emit(chunk)
                    }
                }
                else -> emit(chunk)
            }
        }

        if (toolCalls.isNotEmpty()) {
            // 1. 构建 Assistant 消息包含工具调用
            val assistantMessage = UIMessage(
                role = com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT,
                parts = toolCalls.map {
                    UIMessagePart.Tool(
                        toolCallId = it.toolCallId,
                        toolName = it.toolName,
                        input = it.arguments,
                        output = emptyList()
                    )
                }
            )
            currentMessages.add(assistantMessage)

            // 2. 执行所有工具调用并构建 TOOL 角色消息 (在 EterUee 中, TOOL 角色也是一种消息)
            // 注意: EterUee 的架构可能期望 ToolResult 被合并到之前的 Assistant 消息中,
            // 但为了遵循标准的工具调用流程, 我们先按角色分开。

            val toolResults = toolCalls.map { call ->
                executor.execute(call.toolCallId, call.toolName, call.arguments)
            }

            val toolMessage = UIMessage(
                role = com.eterultimate.eteruee.ai.core.MessageRole.TOOL,
                parts = toolResults.map { result ->
                    UIMessagePart.Tool(
                        toolCallId = result.toolCallId,
                        toolName = "",
                        input = "",
                        output = listOf(UIMessagePart.Text(result.result))
                    )
                }
            )
            currentMessages.add(toolMessage)

            // 3. 继续生成
            continueGenerating = true
        }
    }
}
