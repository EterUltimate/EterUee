package com.eterultimate.eteruee.ai.sdk

import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.subagent.SubagentExecutor
import com.eterultimate.eteruee.ai.subagent.SubagentTextChunk
import com.eterultimate.eteruee.ai.subagent.SubagentToolExecutor
import com.eterultimate.eteruee.ai.ui.UIMessage
import kotlinx.coroutines.flow.Flow

/**
 * Subagent 模式的 AI SDK 扩展
 *
 * 提供 streamTextWithSubagent 方法，实现：
 * 1. 主模型分析需求并生成 Plan
 * 2. Subagent 执行工具调用
 * 3. 主模型基于工具结果生成最终回复
 */
fun AISDK.streamTextWithSubagent(
    request: StreamTextRequest,
    toolExecutor: SubagentToolExecutor,
    systemPrompt: String = SubagentExecutor.DEFAULT_PLAN_SYSTEM_PROMPT
): Flow<SubagentTextChunk> {
    val subagentExecutor = SubagentExecutor(
        aiSDK = this@streamTextWithSubagent,
        toolExecutor = toolExecutor
    )

    return subagentExecutor.execute(
        model = request.model,
        messages = request.messages,
        tools = request.tools,
        temperature = request.temperature,
        topP = request.topP,
        maxTokens = request.maxTokens,
        systemPrompt = systemPrompt
    )
}

/**
 * Subagent 请求配置
 */
data class SubagentRequest(
    val model: Model,
    val messages: List<UIMessage>,
    val tools: List<Tool> = emptyList(),
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String = SubagentExecutor.DEFAULT_PLAN_SYSTEM_PROMPT
)

/**
 * Subagent 流式响应块（兼容标准 TextChunk）
 * 用于将 SubagentTextChunk 转换为标准 TextChunk 的适配
 */
fun SubagentTextChunk.toTextChunk(): TextChunk? {
    return when (this) {
        is SubagentTextChunk.TextDelta -> TextChunk.TextDelta(this.text)
        is SubagentTextChunk.Usage -> TextChunk.Usage(this.tokenUsage)
        is SubagentTextChunk.Finish -> TextChunk.Finish
        is SubagentTextChunk.Error -> null // 错误需要单独处理
        else -> null // 其他事件类型不转换为 TextChunk
    }
}

/**
 * 检查 SubagentTextChunk 是否包含可显示文本
 */
fun SubagentTextChunk.hasDisplayText(): Boolean {
    return when (this) {
        is SubagentTextChunk.TextDelta -> text.isNotBlank()
        is SubagentTextChunk.Status -> true
        is SubagentTextChunk.PlanGenerating -> true
        is SubagentTextChunk.StepExecuting -> true
        is SubagentTextChunk.StepCompleted -> true
        is SubagentTextChunk.Error -> true
        else -> false
    }
}

/**
 * 获取 SubagentTextChunk 的显示文本
 */
fun SubagentTextChunk.getDisplayText(): String {
    return when (this) {
        is SubagentTextChunk.TextDelta -> text
        is SubagentTextChunk.Status -> "▸ $status"
        is SubagentTextChunk.PlanGenerating -> "[计划] $status"
        is SubagentTextChunk.StepExecuting -> "[$toolName] 执行中..."
        is SubagentTextChunk.StepCompleted -> if (result.isError) "[${result.stepId}] 失败" else "[${result.stepId}] 完成"
        is SubagentTextChunk.Error -> "[错误] $error"
        is SubagentTextChunk.PlanGenerated -> "[计划] ${plan.planText}"
        is SubagentTextChunk.PlanExecuted -> result.summary
        is SubagentTextChunk.Usage -> ""
        SubagentTextChunk.Finish -> ""
    }
}
