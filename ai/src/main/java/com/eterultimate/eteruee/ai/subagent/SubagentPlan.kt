package com.eterultimate.eteruee.ai.subagent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Subagent 执行计划
 * 由主模型生成，包含需要执行的工具调用步骤
 */
@Serializable
data class SubagentPlan(
    val planId: String,
    val planText: String,
    val steps: List<PlanStep>,
    val reasoning: String = "",
    val directAnswer: Boolean = false
)

/**
 * 计划执行步骤
 */
@Serializable
data class PlanStep(
    val stepId: String,
    val toolName: String,
    val description: String,
    val arguments: String,
    val status: StepStatus = StepStatus.PENDING,
    val dependsOn: List<String> = emptyList()
)

/**
 * 步骤执行状态
 */
@Serializable
enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

/**
 * 步骤执行结果
 */
@Serializable
data class PlanStepResult(
    val stepId: String,
    val result: String,
    val isError: Boolean = false
)

/**
 * 计划执行结果
 */
@Serializable
data class SubagentPlanResult(
    val planId: String,
    val stepResults: List<PlanStepResult>,
    val summary: String = ""
)

/**
 * Subagent 执行事件
 * 用于流式通知 UI 执行进度
 */
sealed interface SubagentEvent {
    data class PlanGenerated(val plan: SubagentPlan) : SubagentEvent
    data class StepStarted(val stepId: String, val toolName: String) : SubagentEvent
    data class StepCompleted(val stepId: String, val result: PlanStepResult) : SubagentEvent
    data class PlanCompleted(val results: SubagentPlanResult) : SubagentEvent
    data class PlanFailed(val error: String) : SubagentEvent
}
