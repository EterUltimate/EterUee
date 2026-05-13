package com.eterultimate.eteruee.data.ai

import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.ToolResult
import com.eterultimate.eteruee.data.ai.mcp.McpManager
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.ai.tools.createSearchTools
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.files.SkillManager
import com.eterultimate.eteruee.data.ai.tools.createSkillTools
import com.eterultimate.eteruee.data.datastore.getCurrentAssistant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.utils.JsonInstant
import kotlinx.serialization.json.JsonElement

class ChatToolExecutor(
    private val settings: Settings,
    private val mcpManager: McpManager,
    private val localTools: LocalTools,
    private val skillManager: SkillManager,
) : ToolExecutor {

    override suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult {
        return try {
            val allTools = buildList<Tool> {
                if (settings.enableWebSearch) {
                    addAll(createSearchTools(settings))
                }
                addAll(localTools.getTools(settings.getCurrentAssistant().localTools))
                val assistant = settings.getCurrentAssistant()
                if (assistant.enabledSkills.isNotEmpty()) {
                    addAll(
                        createSkillTools(
                            enabledSkills = assistant.enabledSkills,
                            allSkills = skillManager.listSkills(),
                            skillManager = skillManager,
                        )
                    )
                }
                mcpManager.getAllAvailableTools().forEach { (serverId, tool) ->
                    add(
                        Tool(
                            name = "mcp__" + tool.name,
                            description = tool.description ?: "",
                            parameters = { tool.inputSchema },
                            needsApproval = tool.needsApproval,
                            execute = {
                                mcpManager.callTool(serverId, tool.name, it.jsonObject)
                            },
                        )
                    )
                }
            }

            val tool = allTools.find { it.name == toolName }
                ?: throw IllegalArgumentException("Tool not found: $toolName")

            val args = JsonInstant.parseToJsonElement(arguments)
            val outputParts = tool.execute(args)
            val resultText = outputParts.joinToString("\n") {
                when (it) {
                    is com.eterultimate.eteruee.ai.ui.UIMessagePart.Text -> it.text
                    else -> JsonInstant.encodeToString(it)
                }
            }

            ToolResult(toolCallId, resultText)
        } catch (e: Exception) {
            ToolResult(toolCallId, "Error: ${e.message}", isError = true)
        }
    }
}
