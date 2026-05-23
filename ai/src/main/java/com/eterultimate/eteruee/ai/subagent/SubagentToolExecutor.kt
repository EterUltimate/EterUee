package com.eterultimate.eteruee.ai.subagent

import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.ToolResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 支持 Plan 模式的工具执行器
 * 可以按依赖关系串行或并行执行工具调用
 */
interface SubagentToolExecutor : ToolExecutor {

    /**
     * 批量执行计划步骤
     * 根据依赖关系自动决定串行或并行执行
     *
     * @param steps 计划步骤列表
     * @param onEvent 执行事件回调
     * @return 步骤执行结果列表
     */
    suspend fun executeSteps(
        steps: List<PlanStep>,
        onEvent: suspend (SubagentEvent) -> Unit = {}
    ): List<PlanStepResult>
}

/**
 * SubagentToolExecutor 的默认实现
 */
class DefaultSubagentToolExecutor(
    private val toolExecutor: ToolExecutor
) : SubagentToolExecutor {

    override suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult {
        return toolExecutor.execute(toolCallId, toolName, arguments)
    }

    override suspend fun executeSteps(
        steps: List<PlanStep>,
        onEvent: suspend (SubagentEvent) -> Unit
    ): List<PlanStepResult> = coroutineScope {
        val results = mutableMapOf<String, PlanStepResult>()
        val completedSteps = mutableSetOf<String>()

        // 按依赖关系分批执行
        var remainingSteps = steps.toMutableList()

        while (remainingSteps.isNotEmpty()) {
            // 找出当前可以执行的步骤（没有依赖或依赖已完成）
            val executableSteps = remainingSteps.filter { step ->
                step.dependsOn.isEmpty() || step.dependsOn.all { it in completedSteps }
            }

            if (executableSteps.isEmpty()) {
                // 存在循环依赖或无法执行的步骤
                val stuckSteps = remainingSteps.map { step ->
                    PlanStepResult(
                        stepId = step.stepId,
                        result = "Error: Dependencies cannot be satisfied. Missing: ${step.dependsOn.filter { it !in completedSteps }}",
                        isError = true
                    )
                }
                stuckSteps.forEach { results[it.stepId] = it }
                break
            }

            // 并行执行当前可执行的步骤
            val deferredResults = executableSteps.map { step ->
                async {
                    // 通知步骤开始
                    onEvent(SubagentEvent.StepStarted(step.stepId, step.toolName))

                    val result = try {
                        val toolResult = toolExecutor.execute(
                            toolCallId = step.stepId,
                            toolName = step.toolName,
                            arguments = step.arguments
                        )
                        PlanStepResult(
                            stepId = step.stepId,
                            result = toolResult.result,
                            isError = toolResult.isError
                        )
                    } catch (e: Exception) {
                        PlanStepResult(
                            stepId = step.stepId,
                            result = "Error: ${e.message}",
                            isError = true
                        )
                    }

                    // 通知步骤完成
                    onEvent(SubagentEvent.StepCompleted(step.stepId, result))
                    result
                }
            }

            // 等待所有当前批次完成
            val batchResults = deferredResults.awaitAll()
            batchResults.forEach { result ->
                results[result.stepId] = result
                completedSteps.add(result.stepId)
            }

            // 移除已完成的步骤
            remainingSteps.removeAll { it.stepId in completedSteps }
        }

        // 按原始顺序返回结果
        steps.map { step ->
            results[step.stepId]
                ?: PlanStepResult(
                    stepId = step.stepId,
                    result = "Error: Step was not executed",
                    isError = true
                )
        }
    }
}

/**
 * 流式执行计划步骤
 */
fun SubagentToolExecutor.executeStepsFlow(
    steps: List<PlanStep>
): Flow<SubagentEvent> = flow {
    executeSteps(steps) { event ->
        emit(event)
    }
}
