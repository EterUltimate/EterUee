package com.eterultimate.eteruee.ai.subagent

import android.util.Log
import com.eterultimate.eteruee.ai.core.TokenUsage
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.GenerateTextRequest
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "SubagentExecutor"

/**
 * Coordinates the planner model, tool execution, and final model response.
 */
class SubagentExecutor(
    private val aiSDK: AISDK,
    private val toolExecutor: SubagentToolExecutor,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    fun execute(
        model: Model,
        messages: List<UIMessage>,
        tools: List<Tool> = emptyList(),
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null,
        systemPrompt: String = DEFAULT_PLAN_SYSTEM_PROMPT
    ): Flow<SubagentTextChunk> = flow {
        try {
            emit(SubagentTextChunk.PlanGenerating("正在分析需求并制定执行计划..."))

            val plan = generatePlan(
                model = model,
                messages = messages,
                tools = tools,
                systemPrompt = systemPrompt,
                topP = topP
            )
            emit(SubagentTextChunk.PlanGenerated(plan))

            if (plan.directAnswer || plan.steps.isEmpty()) {
                emit(SubagentTextChunk.Status("无需工具调用，直接生成回复..."))
                generateFinalResponse(
                    model = model,
                    messages = messages,
                    planResult = null,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens
                ).collect { emit(it) }
                return@flow
            }

            emit(SubagentTextChunk.Status("开始执行工具调用..."))
            var stepResults: List<PlanStepResult> = emptyList()
            coroutineScope {
                val events = Channel<SubagentEvent>(Channel.UNLIMITED)
                val worker = launch {
                    stepResults = toolExecutor.executeSteps(plan.steps) { event ->
                        events.send(event)
                    }
                    events.close()
                }

                for (event in events) {
                    when (event) {
                        is SubagentEvent.StepStarted -> {
                            emit(SubagentTextChunk.StepExecuting(event.stepId, event.toolName))
                        }

                        is SubagentEvent.StepCompleted -> {
                            emit(SubagentTextChunk.StepCompleted(event.stepId, event.result))
                        }

                        else -> Unit
                    }
                }

                worker.join()
            }

            val planResult = SubagentPlanResult(
                planId = plan.planId,
                stepResults = stepResults,
                summary = buildResultSummary(stepResults)
            )
            emit(SubagentTextChunk.PlanExecuted(planResult))

            emit(SubagentTextChunk.Status("正在生成最终回复..."))
            generateFinalResponse(
                model = model,
                messages = messages,
                planResult = planResult,
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens
            ).collect { emit(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Subagent execution failed", e)
            emit(SubagentTextChunk.Error("Subagent 执行失败: ${e.message}"))
        }
    }

    private suspend fun generatePlan(
        model: Model,
        messages: List<UIMessage>,
        tools: List<Tool>,
        systemPrompt: String,
        topP: Float?
    ): SubagentPlan {
        if (tools.isEmpty()) {
            return SubagentPlan(
                planId = "plan_${System.currentTimeMillis()}",
                planText = "当前没有可用工具，将直接回复。",
                steps = emptyList(),
                directAnswer = true
            )
        }

        val planSystemPrompt = buildString {
            append(systemPrompt)
            appendLine()
            appendLine()
            appendLine("可用工具列表:")
            tools.forEach { tool ->
                append("- ")
                append(tool.name)
                append(": ")
                appendLine(tool.description)
            }
            appendLine()
            append(PLAN_FORMAT_INSTRUCTION)
        }

        val result = aiSDK.generateText(
            GenerateTextRequest(
                model = model,
                messages = listOf(UIMessage.system(planSystemPrompt)) + messages,
                temperature = 0.2f,
                topP = topP,
                maxTokens = 4096,
                tools = emptyList()
            )
        )

        return parsePlan(result.text)
    }

    private fun parsePlan(planText: String): SubagentPlan {
        return try {
            val jsonText = extractJsonFromMarkdown(planText) ?: planText.trim().takeIf { it.startsWith("{") }
            if (jsonText == null) {
                return SubagentPlan(
                    planId = "plan_${System.currentTimeMillis()}",
                    planText = planText,
                    steps = emptyList(),
                    directAnswer = true
                )
            }

            val jsonObj = json.parseToJsonElement(jsonText).jsonObject
            val steps = jsonObj["steps"]?.jsonArray?.mapIndexedNotNull { index, element ->
                val obj = element.jsonObject
                val toolName = obj["tool_name"]?.jsonPrimitive?.content.orEmpty()
                if (toolName.isBlank()) {
                    null
                } else {
                    PlanStep(
                        stepId = obj["step_id"]?.jsonPrimitive?.content ?: "step_$index",
                        toolName = toolName,
                        description = obj["description"]?.jsonPrimitive?.content.orEmpty(),
                        arguments = normalizeArguments(obj["arguments"]),
                        dependsOn = obj["depends_on"]?.jsonArray?.map {
                            it.jsonPrimitive.content
                        } ?: emptyList()
                    )
                }
            } ?: emptyList()

            SubagentPlan(
                planId = jsonObj["plan_id"]?.jsonPrimitive?.content ?: "plan_${System.currentTimeMillis()}",
                planText = jsonObj["plan_text"]?.jsonPrimitive?.content ?: planText,
                steps = steps,
                reasoning = jsonObj["reasoning"]?.jsonPrimitive?.content.orEmpty(),
                directAnswer = steps.isEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse plan", e)
            SubagentPlan(
                planId = "plan_${System.currentTimeMillis()}",
                planText = planText,
                steps = emptyList(),
                directAnswer = true
            )
        }
    }

    private fun normalizeArguments(element: kotlinx.serialization.json.JsonElement?): String {
        if (element == null) return "{}"
        return runCatching {
            element.jsonPrimitive.content
        }.getOrElse {
            if (element is JsonObject) element.toString() else "{}"
        }.ifBlank { "{}" }
    }

    private fun generateFinalResponse(
        model: Model,
        messages: List<UIMessage>,
        planResult: SubagentPlanResult?,
        temperature: Float?,
        topP: Float?,
        maxTokens: Int?
    ): Flow<SubagentTextChunk> = flow {
        val contextMessages = buildList {
            addAll(messages)
            if (planResult != null) {
                add(UIMessage.system(buildToolResultsPrompt(planResult)))
            }
        }

        aiSDK.streamText(
            StreamTextRequest(
                model = model,
                messages = contextMessages,
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens,
                tools = emptyList()
            )
        ).collect { chunk ->
            when (chunk) {
                is TextChunk.TextDelta -> emit(SubagentTextChunk.TextDelta(chunk.text))
                is TextChunk.Usage -> emit(SubagentTextChunk.Usage(chunk.tokenUsage))
                is TextChunk.Finish -> emit(SubagentTextChunk.Finish)
                is TextChunk.ToolCall -> Unit
            }
        }
    }

    private fun buildToolResultsPrompt(planResult: SubagentPlanResult): String {
        return buildString {
            appendLine("以下是 Subagent 已执行的工具结果。请基于这些结果回答用户，不要虚构未返回的信息。")
            appendLine(planResult.summary)
            planResult.stepResults.forEach { result ->
                appendLine()
                appendLine("步骤 ${result.stepId}${if (result.isError) " (失败)" else ""}:")
                appendLine(result.result)
            }
        }
    }

    private fun extractJsonFromMarkdown(text: String): String? {
        val jsonPattern = "```(?:json)?\\s*([\\s\\S]*?)```".toRegex()
        return jsonPattern.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun buildResultSummary(results: List<PlanStepResult>): String {
        val successCount = results.count { !it.isError }
        val failCount = results.count { it.isError }
        return "执行完成: $successCount 成功, $failCount 失败"
    }

    companion object {
        const val DEFAULT_PLAN_SYSTEM_PROMPT = """
你是一个规划器，负责分析用户请求并决定是否需要调用工具。
如果需要工具，请只输出 JSON，不要输出 Markdown 解释。
如果不需要工具，请直接输出一句自然语言说明，系统会进入普通回复流程。

JSON 格式:
{
  "plan_id": "plan_xxx",
  "plan_text": "简要描述执行计划",
  "reasoning": "简要说明为什么需要这些步骤",
  "steps": [
    {
      "step_id": "step_1",
      "tool_name": "工具名称",
      "description": "步骤描述",
      "arguments": "{\"参数\": \"值\"}",
      "depends_on": []
    }
  ]
}
"""

        const val PLAN_FORMAT_INSTRUCTION = """
规则:
- tool_name 必须严格匹配可用工具列表中的名称。
- arguments 必须是合法 JSON 字符串；没有参数时使用 "{}"。
- depends_on 使用前置步骤 ID；没有依赖时使用空数组。
- 可以并行执行的步骤不要互相依赖。
- 不需要工具时不要输出 JSON。
"""
    }
}

sealed interface SubagentTextChunk {
    data class PlanGenerating(val status: String) : SubagentTextChunk
    data class PlanGenerated(val plan: SubagentPlan) : SubagentTextChunk
    data class Status(val status: String) : SubagentTextChunk
    data class StepExecuting(val stepId: String, val toolName: String) : SubagentTextChunk
    data class StepCompleted(val stepId: String, val result: PlanStepResult) : SubagentTextChunk
    data class PlanExecuted(val result: SubagentPlanResult) : SubagentTextChunk
    data class TextDelta(val text: String) : SubagentTextChunk
    data class Usage(val tokenUsage: TokenUsage) : SubagentTextChunk
    data class Error(val error: String) : SubagentTextChunk
    data object Finish : SubagentTextChunk
}
