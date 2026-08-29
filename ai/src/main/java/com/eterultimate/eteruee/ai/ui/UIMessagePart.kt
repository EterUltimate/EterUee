package com.eterultimate.eteruee.ai.ui

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.util.json
import kotlin.time.Clock
import kotlin.time.Instant
import com.eterultimate.eteruee.ai.subagent.PlanStep
import com.eterultimate.eteruee.ai.subagent.PlanStepResult

@Serializable
sealed class ToolApprovalState {
    @Serializable
    @SerialName("auto")
    data object Auto : ToolApprovalState()

    @Serializable
    @SerialName("pending")
    data object Pending : ToolApprovalState()

    @Serializable
    @SerialName("approved")
    data object Approved : ToolApprovalState()

    @Serializable
    @SerialName("denied")
    data class Denied(val reason: String = "") : ToolApprovalState()

    @Serializable
    @SerialName("answered")
    data class Answered(val answer: String) : ToolApprovalState()
}

@Serializable
sealed class UIMessagePart {
    abstract val metadata: JsonObject?

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("image")
    data class Image(
        val url: String,
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("video")
    data class Video(
        val url: String,
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("audio")
    data class Audio(
        val url: String,
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("document")
    data class Document(
        val url: String,
        val fileName: String,
        val mime: String = "text/*",
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        val reasoning: String,
        val createdAt: Instant = Clock.System.now(),
        val finishedAt: Instant? = Clock.System.now(),
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Deprecated("Deprecated")
    @Serializable
    @SerialName("search")
    data object Search : UIMessagePart() {
        override var metadata: JsonObject? = null
    }

    @Deprecated("Use UIMessagePart.Tool instead")
    @Serializable
    @SerialName("tool_call")
    data class ToolCall(
        val toolCallId: String,
        val toolName: String,
        val arguments: String,
        val approvalState: ToolApprovalState = ToolApprovalState.Auto,
        override var metadata: JsonObject? = null
    ) : UIMessagePart() {
        fun merge(other: ToolCall): ToolCall {
            return ToolCall(
                toolCallId = toolCallId,
                toolName = toolName + other.toolName,
                arguments = arguments + other.arguments,
                approvalState = approvalState,
                metadata = if (other.metadata != null) other.metadata else metadata,
            )
        }
    }

    @Deprecated("Use UIMessagePart.Tool instead")
    @Serializable
    @SerialName("tool_result")
    data class ToolResult(
        val toolCallId: String,
        val toolName: String,
        val content: JsonElement,
        val arguments: JsonElement,
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    @SerialName("subagent_plan")
    data class SubagentPlan(
        val planId: String = "",
        val planText: String,
        val reasoning: String = "",
        val steps: List<PlanStep> = emptyList(),
        val isExecuting: Boolean = false,
        val executionResults: List<PlanStepResult> = emptyList(),
        override var metadata: JsonObject? = null
    ) : UIMessagePart()

    @Serializable
    data class PlanStep(
        val stepId: String,
        val toolName: String,
        val description: String,
        val arguments: String,
        val status: StepStatus = StepStatus.PENDING,
        val dependsOn: List<String> = emptyList()
    )

    @Serializable
    enum class StepStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @Serializable
    data class PlanStepResult(
        val stepId: String,
        val result: String,
        val isError: Boolean = false
    )

    @Serializable
    @SerialName("tool")
    data class Tool(
        val toolCallId: String,
        val toolName: String,
        val input: String,
        val output: List<UIMessagePart> = emptyList(),
        val approvalState: ToolApprovalState = ToolApprovalState.Auto,
        override var metadata: JsonObject? = null
    ) : UIMessagePart() {
        /** Whether the tool has been executed (has output) */
        val isExecuted: Boolean get() = output.isNotEmpty()

        /** Whether the tool is pending user approval */
        val isPending: Boolean get() = approvalState is ToolApprovalState.Pending

        /** Whether generation can resume and handle this tool immediately */
        val canResumeExecution: Boolean get() = !isExecuted && approvalState.canResumeToolExecution()

        /** Parse input string as JsonElement */
        fun inputAsJson(): JsonElement = runCatching {
            json.parseToJsonElement(input.ifBlank { "{}" })
        }.getOrElse { JsonObject(emptyMap()) }

        fun merge(other: Tool): Tool {
            return Tool(
                toolCallId = toolCallId,
                toolName = toolName + other.toolName,
                input = input + other.input,
                output = output + other.output,
                approvalState = approvalState,
                metadata = if (other.metadata != null) other.metadata else metadata,
            )
        }
    }
}

fun ToolApprovalState.canResumeToolExecution(): Boolean {
    return when (this) {
        ToolApprovalState.Approved -> true
        is ToolApprovalState.Denied -> true
        is ToolApprovalState.Answered -> true
        ToolApprovalState.Auto,
        ToolApprovalState.Pending,
            -> false
    }
}
