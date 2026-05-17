package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition

/**
 * Prompt 构建服务实现
 */
class PromptBuilderServiceImpl : PromptBuilderService {
    
    override fun buildPrompt(
        systemPrompt: String,
        worldInfoEntries: List<WorldInfoEntry>,
        messages: List<ChatMessage>,
        maxContextLength: Int
    ): String {
        val sb = StringBuilder()
        
        // 1. 添加系统提示词
        if (systemPrompt.isNotBlank()) {
            sb.appendLine(systemPrompt)
            sb.appendLine()
        }
        
        // 2. 按位置分组世界书条目
        val entriesByPosition = groupEntriesByPosition(worldInfoEntries)
        
        // 3. 在系统提示词后注入
        entriesByPosition[InsertionPosition.AFTER_SYSTEM_PROMPT]?.let { entries ->
            if (entries.isNotEmpty()) {
                sb.appendLine("=== World Info ===")
                sb.appendLine(formatEntries(entries))
                sb.appendLine()
            }
        }
        
        // 4. 添加对话历史（可能需要截断）
        val truncatedMessages = truncateMessages(messages, maxContextLength)
        
        // 5. 在最后一条用户消息前注入
        var injectedBeforeLastUser = false
        entriesByPosition[InsertionPosition.BEFORE_LAST_USER_MESSAGE]?.let { entries ->
            if (entries.isNotEmpty()) {
                // 找到最后一条用户消息的位置
                val lastUserIndex = truncatedMessages.indexOfLast { it.role == MessageRole.USER }
                if (lastUserIndex >= 0) {
                    // 先添加最后一条用户消息之前的所有消息
                    for (i in 0 until lastUserIndex) {
                        appendMessage(sb, truncatedMessages[i])
                    }
                    
                    // 注入世界书
                    sb.appendLine("=== World Info ===")
                    sb.appendLine(formatEntries(entries))
                    sb.appendLine()
                    
                    injectedBeforeLastUser = true
                }
            }
        }
        
        // 6. 添加剩余的消息
        if (injectedBeforeLastUser) {
            val lastUserIndex = truncatedMessages.indexOfLast { it.role == MessageRole.USER }
            for (i in lastUserIndex until truncatedMessages.size) {
                appendMessage(sb, truncatedMessages[i])
            }
        } else {
            truncatedMessages.forEach { message ->
                appendMessage(sb, message)
            }
        }
        
        // 7. 在末尾注入
        entriesByPosition[InsertionPosition.AT_END]?.let { entries ->
            if (entries.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== World Info ===")
                sb.appendLine(formatEntries(entries))
            }
        }
        
        return sb.toString()
    }
    
    override fun groupEntriesByPosition(
        entries: List<WorldInfoEntry>
    ): Map<InsertionPosition, List<WorldInfoEntry>> {
        return entries.groupBy { it.position }
    }
    
    override fun formatEntry(entry: WorldInfoEntry): String {
        val keys = entry.getAllKeys().joinToString(", ")
        return "[${keys}]: ${entry.content}"
    }
    
    override fun formatEntries(entries: List<WorldInfoEntry>): String {
        // 按 order 排序
        val sortedEntries = entries.sortedBy { it.order }
        return sortedEntries.joinToString("\n") { formatEntry(it) }
    }
    
    override fun truncateMessages(
        messages: List<ChatMessage>,
        maxContextLength: Int
    ): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()
        
        var totalLength = calculateMessageLength(messages)
        
        // 如果总长度未超过限制，返回所有消息
        if (totalLength <= maxContextLength) {
            return messages
        }
        
        // 从前往后移除消息，直到符合长度限制
        // 但至少保留最后一条消息（通常是当前用户输入）
        val result = messages.toMutableList()
        var index = 0
        
        while (totalLength > maxContextLength && result.size > 1 && index < result.size - 1) {
            val removedMessage = result.removeAt(index)
            totalLength -= removedMessage.content.length + 20 // 估算角色标签长度
        }
        
        return result
    }
    
    override fun calculateMessageLength(messages: List<ChatMessage>): Int {
        return messages.sumOf { message ->
            // 内容长度 + 角色标签（如 "User: " 或 "Assistant: "）的估算长度
            message.content.length + 20
        }
    }
    
    /**
     * 格式化单条消息
     */
    private fun appendMessage(sb: StringBuilder, message: ChatMessage) {
        val roleLabel = when (message.role) {
            MessageRole.USER -> "User"
            MessageRole.ASSISTANT -> "Assistant"
            MessageRole.SYSTEM -> "System"
            MessageRole.TOOL -> "Tool"
        }
        
        sb.appendLine("$roleLabel: ${message.content}")
        sb.appendLine()
    }
}
