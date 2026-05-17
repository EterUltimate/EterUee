package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 聊天服务接口
 */
interface ChatService {
    /**
     * 获取角色的所有聊天
     */
    fun getChatsByCharacter(characterId: kotlin.uuid.Uuid): Flow<List<ChatMetadata>>
    
    /**
     * 获取群组的所有聊天
     */
    fun getChatsByGroup(groupId: kotlin.uuid.Uuid): Flow<List<ChatMetadata>>
    
    /**
     * 根据ID获取聊天元数据
     */
    suspend fun getChatById(chatId: kotlin.uuid.Uuid): ChatMetadata?
    
    /**
     * 创建新聊天
     */
    suspend fun createChat(
        characterId: kotlin.uuid.Uuid,
        groupId: kotlin.uuid.Uuid? = null,
        title: String = ""
    ): Result<ChatMetadata>
    
    /**
     * 删除聊天
     */
    suspend fun deleteChat(chatId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 更新聊天标题
     */
    suspend fun updateChatTitle(chatId: kotlin.uuid.Uuid, title: String): Result<Unit>
    
    /**
     * 切换置顶状态
     */
    suspend fun togglePin(chatId: kotlin.uuid.Uuid): Result<Boolean>
    
    // ==================== 消息管理 ====================
    
    /**
     * 加载聊天消息(窗口加载)
     */
    suspend fun loadMessages(chatId: kotlin.uuid.Uuid, offset: Int = 0, limit: Int = 50): List<MessageNode>
    
    /**
     * 追加用户消息
     */
    suspend fun appendUserMessage(chatId: kotlin.uuid.Uuid, content: String): Result<ChatMessage>
    
    /**
     * 追加AI消息
     */
    suspend fun appendAssistantMessage(chatId: kotlin.uuid.Uuid, content: String): Result<ChatMessage>
    
    /**
     * 添加备选回复(滑动)
     */
    suspend fun addSwipeAlternative(chatId: kotlin.uuid.Uuid, messageIndex: Int, content: String): Result<Unit>
    
    /**
     * 切换到下一个备选
     */
    suspend fun nextSwipe(chatId: kotlin.uuid.Uuid, messageIndex: Int): Result<Unit>
    
    /**
     * 切换到上一个备选
     */
    suspend fun previousSwipe(chatId: kotlin.uuid.Uuid, messageIndex: Int): Result<Unit>
    
    /**
     * 删除消息节点
     */
    suspend fun deleteMessageNode(chatId: kotlin.uuid.Uuid, messageIndex: Int): Result<Unit>
    
    /**
     * 编辑消息内容
     */
    suspend fun editMessage(chatId: kotlin.uuid.Uuid, messageIndex: Int, content: String): Result<Unit>
    
    /**
     * 删除消息(通过ID)
     */
    suspend fun deleteMessageById(chatId: kotlin.uuid.Uuid, messageId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 清空所有消息
     */
    suspend fun clearAllMessages(chatId: kotlin.uuid.Uuid): Result<Unit>
    
    // ==================== 分支管理 ====================
    
    /**
     * 从指定消息创建新分支
     */
    suspend fun createBranch(chatId: kotlin.uuid.Uuid, fromMessageIndex: Int): Result<kotlin.uuid.Uuid>
    
    /**
     * 切换到指定分支
     */
    suspend fun switchToBranch(chatId: kotlin.uuid.Uuid, nodeId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 删除分支
     */
    suspend fun deleteBranch(chatId: kotlin.uuid.Uuid, nodeId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 获取所有分支节点
     */
    suspend fun getBranches(chatId: kotlin.uuid.Uuid): List<MessageNode>
    
    // ==================== 消息编辑 ====================
    
    /**
     * 编辑消息内容
     */
    suspend fun editMessageContent(chatId: kotlin.uuid.Uuid, messageId: kotlin.uuid.Uuid, newContent: String): Result<Unit>
    
    /**
     * 重新生成消息
     */
    suspend fun regenerateMessage(
        chatId: kotlin.uuid.Uuid,
        messageId: kotlin.uuid.Uuid,
        providerId: String,
        modelId: String,
        systemPrompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<ChatGenerationEvent>
    
    /**
     * 获取消息总数
     */
    suspend fun getMessageCount(chatId: kotlin.uuid.Uuid): Int
    
    // ==================== AI生成 ====================
    
    /**
     * 流式生成AI回复
     */
    suspend fun generateResponse(
        chatId: kotlin.uuid.Uuid,
        providerId: String,
        modelId: String,
        systemPrompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<ChatGenerationEvent>
}
