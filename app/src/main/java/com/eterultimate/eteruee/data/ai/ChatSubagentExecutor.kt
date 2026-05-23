package com.eterultimate.eteruee.data.ai

import com.eterultimate.eteruee.ai.subagent.DefaultSubagentToolExecutor
import com.eterultimate.eteruee.ai.subagent.SubagentToolExecutor
import com.eterultimate.eteruee.data.ai.mcp.McpManager
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.ai.tools.createSearchTools
import com.eterultimate.eteruee.data.ai.tools.createSkillTools
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.datastore.getCurrentAssistant
import com.eterultimate.eteruee.data.files.SkillManager

/**
 * App 模块的 Subagent 工具执行器
 *
 * 复用现有的 ChatToolExecutor，包装为 SubagentToolExecutor
 * 集成 MCP、skill、local tools、web search
 */
class ChatSubagentExecutor(
    private val mcpManager: McpManager,
    private val localTools: LocalTools,
    private val skillManager: SkillManager
) {
    /**
     * 创建 SubagentToolExecutor 实例
     *
     * @param settings 当前设置
     * @return SubagentToolExecutor
     */
    fun createToolExecutor(
        settings: Settings
    ): SubagentToolExecutor {
        // 创建标准的 ChatToolExecutor
        val chatToolExecutor = ChatToolExecutor(
            settings = settings,
            mcpManager = mcpManager,
            localTools = localTools,
            skillManager = skillManager
        )

        // 包装为支持批量执行的 SubagentToolExecutor
        return DefaultSubagentToolExecutor(chatToolExecutor)
    }

    companion object {
        /**
         * 创建 ChatSubagentExecutor 的工厂方法
         */
        fun create(
            mcpManager: McpManager,
            localTools: LocalTools,
            skillManager: SkillManager
        ): ChatSubagentExecutor {
            return ChatSubagentExecutor(
                mcpManager = mcpManager,
                localTools = localTools,
                skillManager = skillManager
            )
        }
    }
}
