package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.ChatDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity
import com.eterultimate.eteruee.roleplay.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 聊天服务实现
 */
class ChatServiceImpl(
    private val context: Context,
    private val chatDao: ChatDAO,
    private val fileStorage: RolePlayFileStorage
) : ChatService {
    
    override fun getChatsByCharacter(characterId: Uuid): Flow<List<ChatMetadata>> {
        return chatDao.getChatsByCharacter(characterId.toString()).map { entities ->
            entities.map { ChatEntity.toModel(it) }
        }
    }
    
    override fun getChatsByGroup(groupId: Uuid): Flow<List<ChatMetadata>> {
        return chatDao.getChatsByGroup(groupId.toString()).map { entities ->
            entities.map { ChatEntity.toModel(it) }
        }
    }
    
    override suspend fun getChatById(chatId: Uuid): ChatMetadata? {
        return withContext(Dispatchers.IO) {
            val entity = chatDao.getChatById(chatId.toString())
            entity?.let { ChatEntity.toModel(it) }
        }
    }
    
    override suspend fun createChat(
        characterId: Uuid,
        groupId: Uuid?,
        title: String
    ): Result<ChatMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val chatId = Uuid.random()
                val now = Instant.now()
                
                val metadata = ChatMetadata(
                    chatId = chatId,
                    characterId = characterId,
                    groupId = groupId,
                    title = title.ifBlank { "New Chat" },
                    messageCount = 0,
                    pinned = false,
                    createdAt = now,
                    updatedAt = now
                )
                
                // 保存到数据库
                val entity = ChatEntity.fromModel(metadata)
                chatDao.insertChat(entity)
                
                // 创建JSONL文件
                val chatFile = if (groupId != null) {
                    fileStorage.getGroupChatFile(groupId, chatId)
                } else {
                    fileStorage.getChatFile(characterId, chatId)
                }
                chatFile.createNewFile()
                
                Result.success(metadata)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteChat(chatId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                // 删除数据库记录
                chatDao.deleteChatById(chatId.toString())
                
                // 删除JSONL文件
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                fileStorage.deleteChatFile(chatFile)
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateChatTitle(chatId: Uuid, title: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                val updated = chat.copy(title = title, updatedAt = Instant.now())
                
                chatDao.insertChat(ChatEntity.fromModel(updated))
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun togglePin(chatId: Uuid): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                val newPinned = !chat.pinned
                val updated = chat.copy(pinned = newPinned, updatedAt = Instant.now())
                
                chatDao.insertChat(ChatEntity.fromModel(updated))
                Result.success(newPinned)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 消息管理 ====================
    
    override suspend fun loadMessages(chatId: Uuid, offset: Int, limit: Int): List<MessageNode> {
        return withContext(Dispatchers.IO) {
            val chat = getChatById(chatId) ?: return@withContext emptyList()
            
            val chatFile = if (chat.groupId != null) {
                fileStorage.getGroupChatFile(chat.groupId, chatId)
            } else {
                fileStorage.getChatFile(chat.characterId, chatId)
            }
            
            val messages = fileStorage.loadMessagesWindowed(chatFile, offset, limit)
            
            // 将ChatMessage转换为MessageNode(每个节点只有一个消息)
            messages.map { msg ->
                MessageNode(
                    id = msg.id,
                    messages = listOf(msg),
                    selectedIndex = 0
                )
            }
        }
    }
    
    override suspend fun appendUserMessage(chatId: Uuid, content: String): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                val message = ChatMessage(
                    id = Uuid.random(),
                    role = MessageRole.USER,
                    content = content,
                    timestamp = Instant.now()
                )
                
                // 追加到JSONL文件
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                fileStorage.appendMessageToFile(chatFile, message)
                
                // 更新消息计数
                val newCount = chat.messageCount + 1
                chatDao.updateMessageCount(chatId.toString(), newCount, Instant.now().toEpochMilli())
                
                Result.success(message)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun appendAssistantMessage(chatId: Uuid, content: String): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                val message = ChatMessage(
                    id = Uuid.random(),
                    role = MessageRole.ASSISTANT,
                    content = content,
                    timestamp = Instant.now()
                )
                
                // 追加到JSONL文件
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                fileStorage.appendMessageToFile(chatFile, message)
                
                // 更新消息计数
                val newCount = chat.messageCount + 1
                chatDao.updateMessageCount(chatId.toString(), newCount, Instant.now().toEpochMilli())
                
                Result.success(message)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun addSwipeAlternative(chatId: Uuid, messageIndex: Int, content: String): Result<Unit> {
        // TODO: 需要重新设计JSONL格式以支持MessageNode的备选消息
        // 当前简化实现:直接返回错误
        return Result.failure(Exception("Swipe alternatives not yet implemented"))
    }
    
    override suspend fun nextSwipe(chatId: Uuid, messageIndex: Int): Result<Unit> {
        return Result.failure(Exception("Swipe not yet implemented"))
    }
    
    override suspend fun previousSwipe(chatId: Uuid, messageIndex: Int): Result<Unit> {
        return Result.failure(Exception("Swipe not yet implemented"))
    }
    
    override suspend fun deleteMessageNode(chatId: Uuid, messageIndex: Int): Result<Unit> {
        // TODO: 需要实现消息删除逻辑(重写JSONL文件)
        return Result.failure(Exception("Message deletion not yet implemented"))
    }
    
    override suspend fun editMessage(chatId: Uuid, messageIndex: Int, content: String): Result<Unit> {
        // TODO: 需要实现消息编辑逻辑(重写JSONL文件)
        return Result.failure(Exception("Message editing not yet implemented"))
    }
    
    override suspend fun getMessageCount(chatId: Uuid): Int {
        return withContext(Dispatchers.IO) {
            val chat = getChatById(chatId) ?: return@withContext 0
            
            val chatFile = if (chat.groupId != null) {
                fileStorage.getGroupChatFile(chat.groupId, chatId)
            } else {
                fileStorage.getChatFile(chat.characterId, chatId)
            }
            
            fileStorage.getChatLineCount(chatFile)
        }
    }
    
    override suspend fun deleteMessageById(chatId: Uuid, messageId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                
                // 读取所有消息
                val messages = fileStorage.loadMessagesFromJsonl(chatFile)
                
                // 过滤掉要删除的消息
                val filteredMessages = messages.filter { it.id != messageId }
                
                // 重写文件
                fileStorage.saveMessagesToJsonl(chatFile, filteredMessages)
                
                // 更新消息计数
                val newCount = filteredMessages.size
                chatDao.updateMessageCount(chatId.toString(), newCount, Instant.now().toEpochMilli())
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun clearAllMessages(chatId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                
                // 清空文件（写入空列表）
                fileStorage.saveMessagesToJsonl(chatFile, emptyList())
                
                // 更新消息计数
                chatDao.updateMessageCount(chatId.toString(), 0, Instant.now().toEpochMilli())
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 分支管理 ====================
    
    override suspend fun createBranch(chatId: Uuid, fromMessageIndex: Int): Result<Uuid> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                // 加载所有消息
                val allMessages = loadAllMessages(chat)
                
                if (fromMessageIndex < 0 || fromMessageIndex >= allMessages.size) {
                    return@withContext Result.failure(Exception("Invalid message index"))
                }
                
                // 创建新分支节点ID
                val newBranchId = Uuid.random()
                
                // 复制从指定消息开始的所有后续消息到新分支
                val branchMessages = allMessages.subList(fromMessageIndex, allMessages.size)
                
                // TODO: 将分支消息保存到单独的文件或标记
                // 这里简化处理，实际应该创建分支文件
                
                // 更新 ChatMetadata 的 rootNodes
                val updatedRootNodes = chat.rootNodes + newBranchId
                val updatedChat = chat.copy(
                    rootNodes = updatedRootNodes,
                    updatedAt = Instant.now()
                )
                
                chatDao.insertChat(ChatEntity.fromModel(updatedChat))
                
                Result.success(newBranchId)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun switchToBranch(chatId: Uuid, nodeId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                // 验证分支是否存在
                if (!chat.rootNodes.contains(nodeId)) {
                    return@withContext Result.failure(Exception("Branch not found"))
                }
                
                // 更新 activeBranchId
                val updatedChat = chat.copy(
                    activeBranchId = nodeId,
                    updatedAt = Instant.now()
                )
                
                chatDao.insertChat(ChatEntity.fromModel(updatedChat))
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteBranch(chatId: Uuid, nodeId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                // 从 rootNodes 中移除
                val updatedRootNodes = chat.rootNodes.filter { it != nodeId }
                val updatedChat = chat.copy(
                    rootNodes = updatedRootNodes,
                    activeBranchId = if (chat.activeBranchId == nodeId) null else chat.activeBranchId,
                    updatedAt = Instant.now()
                )
                
                chatDao.insertChat(ChatEntity.fromModel(updatedChat))
                
                // TODO: 删除分支对应的消息文件
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getBranches(chatId: Uuid): List<MessageNode> {
        return withContext(Dispatchers.IO) {
            val chat = getChatById(chatId) ?: return@withContext emptyList()
            
            // 返回所有根节点作为分支列表
            chat.rootNodes.map { nodeId ->
                MessageNode(
                    id = nodeId,
                    messages = emptyList(),  // TODO: 加载分支消息
                    selectedIndex = 0,
                    branchLabel = "Branch ${nodeId.toString().take(8)}"
                )
            }
        }
    }
    
    // ==================== 消息编辑 ====================
    
    override suspend fun editMessageContent(chatId: Uuid, messageId: Uuid, newContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                
                val chatFile = if (chat.groupId != null) {
                    fileStorage.getGroupChatFile(chat.groupId, chatId)
                } else {
                    fileStorage.getChatFile(chat.characterId, chatId)
                }
                
                // 加载所有消息
                val messages = fileStorage.loadMessagesFromJsonl(chatFile)
                
                // 找到并更新消息
                val updatedMessages = messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(content = newContent, timestamp = Instant.now())
                    } else {
                        msg
                    }
                }
                
                // 检查是否找到消息
                if (updatedMessages.size == messages.size && !messages.any { it.id == messageId }) {
                    return@withContext Result.failure(Exception("Message not found"))
                }
                
                // 保存更新后的消息
                fileStorage.saveMessagesToJsonl(chatFile, updatedMessages)
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun regenerateMessage(
        chatId: Uuid,
        messageId: Uuid,
        providerId: String,
        modelId: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<ChatGenerationEvent> {
        // TODO: 实现消息重新生成
        // 1. 删除原消息
        // 2. 基于之前的上下文重新生成
        // 3. 流式返回新消息
        
        return kotlinx.coroutines.flow.flow {
            emit(ChatGenerationEvent.Error(Exception("Regeneration not yet implemented")))
        }
    }
    
    // ==================== AI生成 ====================
    
    override suspend fun generateResponse(
        chatId: Uuid,
        providerId: String,
        modelId: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<ChatGenerationEvent> {
        // TODO: 集成AI SDK进行流式生成
        // 这里需要:
        // 1. 加载历史消息
        // 2. 构建对话上下文
        // 3. 调用AI SDK
        // 4. 流式返回结果
        
        return kotlinx.coroutines.flow.flow {
            emit(ChatGenerationEvent.Error(Exception("AI generation not yet implemented")))
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 加载聊天的所有消息
     */
    private suspend fun loadAllMessages(chat: ChatMetadata): List<ChatMessage> {
        val chatFile = if (chat.groupId != null) {
            fileStorage.getGroupChatFile(chat.groupId, chat.chatId)
        } else {
            fileStorage.getChatFile(chat.characterId, chat.chatId)
        }
        
        return fileStorage.loadMessagesFromJsonl(chatFile)
    }
}
