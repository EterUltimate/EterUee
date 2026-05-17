package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 备份服务实现
 */
class BackupServiceImpl(
    private val chatService: ChatService
) : BackupService {
    
    override suspend fun exportChatAsJson(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取聊天记录
                val messageNodes = chatService.loadMessages(chatId)
                
                // 提取所有消息（从 MessageNode 中）
                val messages = messageNodes.flatMap { node -> node.messages }
                
                // 构建导出数据
                val exportData = ChatExportData(
                    characterName = characterName,
                    chatId = chatId.toString(),
                    exportTime = Instant.now().toString(),
                    messages = messages.map { msg ->
                        MessageExportData(
                            role = msg.role.toString(),
                            content = msg.content,
                            timestamp = msg.timestamp.toString()
                        )
                    }
                )
                
                // 序列化为 JSON
                val json = Json { prettyPrint = true }
                val jsonString = json.encodeToString(exportData)
                
                // 保存到文件
                val file = createExportFile(context, characterName, BackupService.ExportFormat.JSON)
                file.writeText(jsonString)
                
                Result.success(file)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportChatAsTxt(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取聊天记录
                val messageNodes = chatService.loadMessages(chatId)
                
                // 提取所有消息
                val messages = messageNodes.flatMap { node -> node.messages }
                
                // 构建文本内容
                val sb = StringBuilder()
                sb.appendLine("=== $characterName 聊天记录 ===")
                sb.appendLine("导出时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
                sb.appendLine("消息数量: ${messages.size}")
                sb.appendLine()
                sb.appendLine("-".repeat(50))
                sb.appendLine()
                
                messages.forEachIndexed { index, msg ->
                    val role = when (msg.role.toString()) {
                        "USER" -> "你"
                        "ASSISTANT" -> characterName
                        else -> msg.role.toString()
                    }
                    
                    sb.appendLine("[$role] ${formatTimestamp(msg.timestamp)}")
                    sb.appendLine(msg.content)
                    sb.appendLine()
                    if (index < messages.size - 1) {
                        sb.appendLine("-".repeat(30))
                        sb.appendLine()
                    }
                }
                
                // 保存到文件
                val file = createExportFile(context, characterName, BackupService.ExportFormat.TXT)
                file.writeText(sb.toString())
                
                Result.success(file)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportChatAsHtml(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取聊天记录
                val messageNodes = chatService.loadMessages(chatId)
                
                // 提取所有消息
                val messages = messageNodes.flatMap { node -> node.messages }
                
                // 构建 HTML 内容
                val sb = StringBuilder()
                sb.appendLine("<!DOCTYPE html>")
                sb.appendLine("<html lang=\"zh-CN\">")
                sb.appendLine("<head>")
                sb.appendLine("  <meta charset=\"UTF-8\">")
                sb.appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                sb.appendLine("  <title>$characterName - 聊天记录</title>")
                sb.appendLine("  <style>")
                sb.appendLine("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; background: #f5f5f5; }")
                sb.appendLine("    .header { text-align: center; margin-bottom: 30px; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
                sb.appendLine("    .message { margin-bottom: 20px; padding: 15px; border-radius: 8px; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
                sb.appendLine("    .user-message { border-left: 4px solid #2196F3; }")
                sb.appendLine("    .assistant-message { border-left: 4px solid #4CAF50; }")
                sb.appendLine("    .role { font-weight: bold; margin-bottom: 5px; color: #333; }")
                sb.appendLine("    .timestamp { font-size: 12px; color: #999; margin-bottom: 10px; }")
                sb.appendLine("    .content { line-height: 1.6; color: #555; white-space: pre-wrap; }")
                sb.appendLine("  </style>")
                sb.appendLine("</head>")
                sb.appendLine("<body>")
                sb.appendLine("  <div class=\"header\">")
                sb.appendLine("    <h1>$characterName</h1>")
                sb.appendLine("    <p>导出时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</p>")
                sb.appendLine("    <p>消息数量: ${messages.size}</p>")
                sb.appendLine("  </div>")
                
                messages.forEach { msg ->
                    val isUser = msg.role.toString() == "USER"
                    val role = if (isUser) "你" else characterName
                    val messageClass = if (isUser) "user-message" else "assistant-message"
                    
                    sb.appendLine("  <div class=\"message $messageClass\">")
                    sb.appendLine("    <div class=\"role\">$role</div>")
                    sb.appendLine("    <div class=\"timestamp\">${formatTimestamp(msg.timestamp)}</div>")
                    sb.appendLine("    <div class=\"content\">${escapeHtml(msg.content)}</div>")
                    sb.appendLine("  </div>")
                }
                
                sb.appendLine("</body>")
                sb.appendLine("</html>")
                
                // 保存到文件
                val file = createExportFile(context, characterName, BackupService.ExportFormat.HTML)
                file.writeText(sb.toString())
                
                Result.success(file)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportCharacterData(
        context: Context,
        characterId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File> {
        // TODO: 实现角色数据导出（包含所有聊天）
        return Result.failure(NotImplementedError("角色数据导出暂未实现"))
    }
    
    override fun getExportFileName(characterName: String, format: BackupService.ExportFormat): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val extension = when (format) {
            BackupService.ExportFormat.JSON -> "json"
            BackupService.ExportFormat.TXT -> "txt"
            BackupService.ExportFormat.HTML -> "html"
        }
        return "${characterName}_chat_${timestamp}.$extension"
    }
    
    /**
     * 创建导出文件
     */
    private fun createExportFile(context: Context, characterName: String, format: BackupService.ExportFormat): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloadsDir, "EterUee/Exports")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        val fileName = getExportFileName(characterName, format)
        return File(folder, fileName)
    }
    
    /**
     * 格式化时间戳
     */
    private fun formatTimestamp(instant: Instant): String {
        return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
    
    /**
     * HTML 转义
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    
    /**
     * 聊天导出数据模型
     */
    @kotlinx.serialization.Serializable
    data class ChatExportData(
        val characterName: String,
        val chatId: String,
        val exportTime: String,
        val messages: List<MessageExportData>
    )
    
    /**
     * 消息导出数据模型
     */
    @kotlinx.serialization.Serializable
    data class MessageExportData(
        val role: String,
        val content: String,
        val timestamp: String
    )
}
