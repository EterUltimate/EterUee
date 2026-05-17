package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition

/**
 * Prompt 构建服务
 * 负责组装系统提示词、世界书条目和对话历史
 */
interface PromptBuilderService {
    
    /**
     * 构建完整的 Prompt
     * 
     * @param systemPrompt 系统提示词
     * @param worldInfoEntries 需要注入的世界书条目
     * @param messages 对话历史消息列表
     * @param maxContextLength 最大上下文长度（字符数）
     * @return 构建好的完整 Prompt
     */
    fun buildPrompt(
        systemPrompt: String,
        worldInfoEntries: List<WorldInfoEntry>,
        messages: List<ChatMessage>,
        maxContextLength: Int = 4096
    ): String
    
    /**
     * 根据插入位置分组世界书条目
     */
    fun groupEntriesByPosition(entries: List<WorldInfoEntry>): Map<InsertionPosition, List<WorldInfoEntry>>
    
    /**
     * 格式化单个世界书条目为文本
     */
    fun formatEntry(entry: WorldInfoEntry): String
    
    /**
     * 格式化世界书条目列表
     */
    fun formatEntries(entries: List<WorldInfoEntry>): String
    
    /**
     * 截断消息列表以适应上下文长度限制
     */
    fun truncateMessages(messages: List<ChatMessage>, maxContextLength: Int): List<ChatMessage>
    
    /**
     * 计算消息列表的总字符数
     */
    fun calculateMessageLength(messages: List<ChatMessage>): Int
}
