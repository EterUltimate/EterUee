package com.eterultimate.eteruee.roleplay.domain.extension.builtin

import com.eterultimate.eteruee.roleplay.domain.extension.*

/**
 * 基础命令扩展
 * 提供常用的 Slash Commands
 */
class BasicCommandsExtension : Extension {
    
    override val id = "basic"
    override val name = "Basic Commands"
    override val description = "基础命令集合，包括帮助、令牌计数等实用功能"
    override val version = "1.0.0"
    override val author = "EterUee Team"
    override var enabled = true
    
    private val commands = listOf(
        SlashCommand(
            name = "help",
            description = "显示帮助信息或特定命令的用法",
            usage = "/help [command]",
            parameters = listOf(
                CommandParameter(
                    name = "command",
                    description = "要查询的命令名称",
                    required = false
                )
            )
        ),
        SlashCommand(
            name = "token",
            description = "显示当前对话的令牌使用情况",
            usage = "/token"
        ),
        SlashCommand(
            name = "clear",
            description = "清空聊天历史",
            usage = "/clear"
        ),
        SlashCommand(
            name = "info",
            description = "显示当前聊天/角色/群组的信息",
            usage = "/info"
        ),
        SlashCommand(
            name = "export",
            description = "导出当前聊天记录",
            usage = "/export [format]",
            parameters = listOf(
                CommandParameter(
                    name = "format",
                    description = "导出格式（json/txt/html）",
                    required = false,
                    type = ParameterType.STRING
                )
            )
        )
    )
    
    override fun getCommands(): List<SlashCommand> = commands
    
    override suspend fun executeCommand(
        commandName: String,
        args: Map<String, String>,
        context: ExtensionContext
    ): ExtensionResult {
        return when (commandName) {
            "help" -> handleHelp(args, context)
            "token" -> handleToken(context)
            "clear" -> handleClear(context)
            "info" -> handleInfo(context)
            "export" -> handleExport(args, context)
            else -> ExtensionResult.Error(
                errorMessage = "Unknown command: $commandName",
                errorCode = "UNKNOWN_COMMAND"
            )
        }
    }
    
    override suspend fun initialize() {
        // 初始化逻辑（如果需要）
    }
    
    override suspend fun destroy() {
        // 清理逻辑（如果需要）
    }
    
    private fun handleHelp(args: Map<String, String>, context: ExtensionContext): ExtensionResult {
        val commandName = args["command"] ?: args["arg0"]
        
        return if (commandName != null) {
            // 显示特定命令的帮助
            ExtensionResult.Success(
                message = "TODO: 需要注入 ExtensionManager 来查询命令帮助"
            )
        } else {
            // 显示所有命令列表
            ExtensionResult.Success(
                message = """
                    **可用命令：**
                    
                    `/help [command]` - 显示帮助信息
                    `/token` - 显示令牌使用情况
                    `/clear` - 清空聊天历史
                    `/info` - 显示聊天信息
                    `/export [format]` - 导出聊天记录
                    
                    使用 `/help <command>` 查看特定命令的详细用法。
                """.trimIndent()
            )
        }
    }
    
    private fun handleToken(context: ExtensionContext): ExtensionResult {
        return ExtensionResult.Success(
            message = """
                **令牌统计：**
                
                TODO: 需要注入 TokenService
                
                - 总令牌数：计算中...
                - 用户消息：计算中...
                - AI回复：计算中...
            """.trimIndent()
        )
    }
    
    private fun handleClear(context: ExtensionContext): ExtensionResult {
        return ExtensionResult.Success(
            message = "TODO: 需要注入 ChatService 来清空聊天历史"
        )
    }
    
    private fun handleInfo(context: ExtensionContext): ExtensionResult {
        val sb = StringBuilder()
        sb.appendLine("**聊天信息：**")
        sb.appendLine()
        
        context.chatId?.let {
            sb.appendLine("- 聊天ID: `$it`")
        }
        
        context.characterId?.let {
            sb.appendLine("- 角色ID: `$it`")
        }
        
        context.groupId?.let {
            sb.appendLine("- 群组ID: `$it`")
        }
        
        sb.appendLine("- 消息历史: ${context.messageHistory.size} 条")
        
        return ExtensionResult.Success(message = sb.toString())
    }
    
    private fun handleExport(args: Map<String, String>, context: ExtensionContext): ExtensionResult {
        val format = args["format"] ?: args["arg0"] ?: "json"
        
        return ExtensionResult.Success(
            message = """
                **导出聊天：**
                
                格式：$format
                
                TODO: 需要注入 BackupService
                
                文件将保存到：Downloads/EterUee/Exports/
            """.trimIndent()
        )
    }
}
