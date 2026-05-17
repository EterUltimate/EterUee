package com.eterultimate.eteruee.roleplay.domain.extension

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 扩展管理器
 * 负责加载、注册和管理所有扩展
 */
interface ExtensionManager {
    
    /**
     * 所有已注册的扩展
     */
    val extensions: StateFlow<List<Extension>>
    
    /**
     * 所有可用的命令（按名称索引）
     */
    val commands: StateFlow<Map<String, SlashCommand>>
    
    /**
     * 注册扩展
     */
    fun registerExtension(extension: Extension)
    
    /**
     * 注销扩展
     */
    fun unregisterExtension(extensionId: String)
    
    /**
     * 启用扩展
     */
    fun enableExtension(extensionId: String)
    
    /**
     * 禁用扩展
     */
    fun disableExtension(extensionId: String)
    
    /**
     * 执行命令
     * 
     * @param commandName 命令名称（不含 /）
     * @param args 命令参数
     * @param context 执行上下文
     * @return 执行结果
     */
    suspend fun executeCommand(
        commandName: String,
        args: Map<String, String>,
        context: ExtensionContext
    ): ExtensionResult
    
    /**
     * 获取命令帮助信息
     */
    fun getCommandHelp(commandName: String): String?
    
    /**
     * 获取所有命令的帮助列表
     */
    fun getAllCommandsHelp(): List<String>
    
    /**
     * 解析用户输入的 Slash Command
     * 
     * @param userInput 用户输入（如 "/help token"）
     * @return 解析结果，如果不是命令则返回 null
     */
    fun parseSlashCommand(userInput: String): ParsedCommand?
}

/**
 * 解析后的命令
 */
data class ParsedCommand(
    val commandName: String,           // 命令名（不含 /）
    val arguments: Map<String, String> // 参数字典
)

/**
 * 扩展管理器实现
 */
class ExtensionManagerImpl : ExtensionManager {
    
    private val _extensions = MutableStateFlow<List<Extension>>(emptyList())
    override val extensions: StateFlow<List<Extension>> = _extensions.asStateFlow()
    
    private val _commands = MutableStateFlow<Map<String, SlashCommand>>(emptyMap())
    override val commands: StateFlow<Map<String, SlashCommand>> = _commands.asStateFlow()
    
    private val extensionMap = mutableMapOf<String, Extension>()
    private val commandMap = mutableMapOf<String, Pair<Extension, SlashCommand>>()
    
    override fun registerExtension(extension: Extension) {
        if (extensionMap.containsKey(extension.id)) {
            throw IllegalArgumentException("Extension with id '${extension.id}' already registered")
        }
        
        extensionMap[extension.id] = extension
        
        // 注册该扩展的所有命令
        extension.getCommands().forEach { command ->
            val fullCommandName = "${extension.id}.${command.name}"
            commandMap[fullCommandName] = extension to command
            commandMap[command.name] = extension to command  // 也注册短名称
        }
        
        // 更新状态流
        _extensions.value = extensionMap.values.toList()
        _commands.value = commandMap.mapValues { it.value.second }.toMap()
    }
    
    override fun unregisterExtension(extensionId: String) {
        val extension = extensionMap.remove(extensionId)
            ?: throw IllegalArgumentException("Extension '$extensionId' not found")
        
        // 移除该扩展的所有命令
        val commandsToRemove = commandMap.filter { 
            it.value.first.id == extensionId 
        }.keys
        commandsToRemove.forEach { commandMap.remove(it) }
        
        // 更新状态流
        _extensions.value = extensionMap.values.toList()
        _commands.value = commandMap.mapValues { it.value.second }.toMap()
    }
    
    override fun enableExtension(extensionId: String) {
        val extension = extensionMap[extensionId]
            ?: throw IllegalArgumentException("Extension '$extensionId' not found")
        
        extension.enabled = true
        _extensions.value = extensionMap.values.toList()
    }
    
    override fun disableExtension(extensionId: String) {
        val extension = extensionMap[extensionId]
            ?: throw IllegalArgumentException("Extension '$extensionId' not found")
        
        extension.enabled = false
        _extensions.value = extensionMap.values.toList()
    }
    
    override suspend fun executeCommand(
        commandName: String,
        args: Map<String, String>,
        context: ExtensionContext
    ): ExtensionResult {
        val (extension, command) = commandMap[commandName]
            ?: return ExtensionResult.Error(
                errorMessage = "Unknown command: /$commandName",
                errorCode = "COMMAND_NOT_FOUND"
            )
        
        if (!extension.enabled) {
            return ExtensionResult.Error(
                errorMessage = "Extension '${extension.name}' is disabled",
                errorCode = "EXTENSION_DISABLED"
            )
        }
        
        return try {
            extension.executeCommand(command.name, args, context)
        } catch (e: Exception) {
            ExtensionResult.Error(
                errorMessage = "Error executing command: ${e.message}",
                errorCode = "EXECUTION_ERROR"
            )
        }
    }
    
    override fun getCommandHelp(commandName: String): String? {
        val (_, command) = commandMap[commandName] ?: return null
        
        val sb = StringBuilder()
        sb.appendLine("**/${command.name}** - ${command.description}")
        
        if (command.usage.isNotBlank()) {
            sb.appendLine("Usage: `${command.usage}`")
        }
        
        if (command.parameters.isNotEmpty()) {
            sb.appendLine("\nParameters:")
            command.parameters.forEach { param ->
                val required = if (param.required) "*" else ""
                sb.appendLine("  `$required${param.name}`: ${param.description} (${param.type})")
            }
        }
        
        return sb.toString()
    }
    
    override fun getAllCommandsHelp(): List<String> {
        return commandMap.values
            .distinctBy { it.second.name }
            .map { (_, command) ->
                "/${command.name} - ${command.description}"
            }
            .sorted()
    }
    
    override fun parseSlashCommand(userInput: String): ParsedCommand? {
        if (!userInput.startsWith("/")) {
            return null
        }
        
        val input = userInput.trim().substring(1) // 去掉开头的 /
        if (input.isBlank()) {
            return null
        }
        
        val parts = input.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.isEmpty()) {
            return null
        }
        
        val commandName = parts[0]
        val arguments = mutableMapOf<String, String>()
        
        // 解析参数（简单实现：支持 key=value 格式）
        for (i in 1 until parts.size) {
            val part = parts[i]
            if (part.contains("=")) {
                val (key, value) = part.split("=", limit = 2)
                arguments[key] = value
            } else {
                // 无名参数，使用索引作为键
                arguments["arg${i - 1}"] = part
            }
        }
        
        return ParsedCommand(commandName, arguments)
    }
}
