package com.eterultimate.eteruee.roleplay.domain.extension

import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 扩展接口
 * 允许用户通过 Slash Commands 与聊天系统交互
 */
interface Extension {
    /**
     * 扩展的唯一标识符
     */
    val id: String
    
    /**
     * 扩展名称（显示给用户）
     */
    val name: String
    
    /**
     * 扩展描述
     */
    val description: String
    
    /**
     * 版本号
     */
    val version: String
    
    /**
     * 作者
     */
    val author: String
    
    /**
     * 是否启用
     */
    var enabled: Boolean
    
    /**
     * 获取所有支持的命令
     */
    fun getCommands(): List<SlashCommand>
    
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
     * 初始化扩展
     */
    suspend fun initialize()
    
    /**
     * 销毁扩展
     */
    suspend fun destroy()
}

/**
 * Slash 命令定义
 */
@Serializable
data class SlashCommand(
    val name: String,              // 命令名称（如 "help"）
    val description: String,       // 命令描述
    val usage: String = "",        // 用法说明（如 "/help [command]"）
    val parameters: List<CommandParameter> = emptyList(),  // 参数列表
    val requiresArgs: Boolean = false  // 是否需要参数
)

/**
 * 命令参数定义
 */
@Serializable
data class CommandParameter(
    val name: String,              // 参数名
    val description: String,       // 参数描述
    val required: Boolean = false, // 是否必需
    val type: ParameterType = ParameterType.STRING  // 参数类型
)

/**
 * 参数类型
 */
@Serializable
enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    USER,
    CHARACTER,
    CHANNEL
}

/**
 * 扩展执行上下文
 */
data class ExtensionContext(
    val chatId: kotlin.uuid.Uuid?,           // 当前聊天ID
    val characterId: kotlin.uuid.Uuid?,      // 当前角色ID
    val groupId: kotlin.uuid.Uuid?,          // 当前群组ID
    val userId: String,                      // 用户ID
    val messageHistory: List<String> = emptyList(),  // 消息历史
    val metadata: Map<String, Any> = emptyMap()  // 元数据
)

/**
 * 扩展执行结果
 */
sealed class ExtensionResult {
    /**
     * 成功结果
     */
    data class Success(
        val message: String,
        val data: Map<String, Any> = emptyMap()
    ) : ExtensionResult()
    
    /**
     * 失败结果
     */
    data class Error(
        val errorMessage: String,
        val errorCode: String? = null
    ) : ExtensionResult()
    
    /**
     * 无响应（静默执行）
     */
    object Silent : ExtensionResult()
}

/**
 * 扩展元数据
 */
@Serializable
data class ExtensionMetadata(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    @kotlinx.serialization.Contextual val createdAt: Instant = Instant.now(),
    @kotlinx.serialization.Contextual val updatedAt: Instant = Instant.now(),
    val enabled: Boolean = true,
    val commandCount: Int = 0
)
