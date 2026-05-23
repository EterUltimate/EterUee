package com.eterultimate.eteruee.roleplay.domain.subagent

import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.ToolResult
import com.eterultimate.eteruee.ai.subagent.DefaultSubagentToolExecutor
import com.eterultimate.eteruee.ai.subagent.SubagentToolExecutor

/**
 * Roleplay subagent bridge. Roleplay currently has no dedicated tool surface,
 * so this executor provides a safe no-tool implementation and can be extended
 * when character tools are added.
 */
class RoleplaySubagentExecutor {
    fun createToolExecutor(): SubagentToolExecutor {
        return DefaultSubagentToolExecutor(NoopToolExecutor)
    }

    private data object NoopToolExecutor : ToolExecutor {
        override suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult {
            return ToolResult(
                toolCallId = toolCallId,
                result = "Tool execution is not available in roleplay chat: $toolName",
                isError = true
            )
        }
    }
}
