package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.ChatDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity
import com.eterultimate.eteruee.roleplay.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 聊天服务实现
 */
class ChatServiceImpl(
    private val context: Context,
    private val chatDao: ChatDAO,
    private val fileStorage: RolePlayFileStorage,
    private val aiSDK: AISDK
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
                    activeBranchId = chatId,
                    rootNodes = listOf(chatId),
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
                fileStorage.deleteChatBranchFiles(chatFile, chatId)

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

            val chatFile = activeChatFile(chat)

            val messages = fileStorage.loadMessagesWindowed(chatFile, offset, limit)

            messages.map { it.toMessageNode() }
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

                val chatFile = activeChatFile(chat)
                fileStorage.appendMessageToFile(chatFile, message)

                persistChat(chat.copy(messageCount = fileStorage.getChatLineCount(chatFile), updatedAt = Instant.now()))

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

                val chatFile = activeChatFile(chat)
                fileStorage.appendMessageToFile(chatFile, message)

                persistChat(chat.copy(messageCount = fileStorage.getChatLineCount(chatFile), updatedAt = Instant.now()))

                Result.success(message)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun addSwipeAlternative(chatId: Uuid, messageIndex: Int, content: String): Result<Unit> {
        return updateMessageAtIndex(chatId, messageIndex) { message ->
            val alternatives = (listOf(message.content) + message.swipeAlternatives)
                .filter { it != content }
            message.copy(
                content = content,
                timestamp = Instant.now(),
                swipeAlternatives = alternatives
            )
        }
    }

    override suspend fun nextSwipe(chatId: Uuid, messageIndex: Int): Result<Unit> {
        return updateMessageAtIndex(chatId, messageIndex) { message ->
            val next = message.swipeAlternatives.firstOrNull() ?: return@updateMessageAtIndex message
            message.copy(
                content = next,
                timestamp = Instant.now(),
                swipeAlternatives = message.swipeAlternatives.drop(1) + message.content
            )
        }
    }

    override suspend fun previousSwipe(chatId: Uuid, messageIndex: Int): Result<Unit> {
        return updateMessageAtIndex(chatId, messageIndex) { message ->
            val previous = message.swipeAlternatives.lastOrNull() ?: return@updateMessageAtIndex message
            message.copy(
                content = previous,
                timestamp = Instant.now(),
                swipeAlternatives = listOf(message.content) + message.swipeAlternatives.dropLast(1)
            )
        }
    }

    override suspend fun deleteMessageNode(chatId: Uuid, messageIndex: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                val chatFile = activeChatFile(chat)
                val messages = fileStorage.loadMessagesFromJsonl(chatFile).toMutableList()
                if (messageIndex !in messages.indices) {
                    return@withContext Result.failure(Exception("Message index out of range"))
                }
                messages.removeAt(messageIndex)
                fileStorage.saveMessagesToJsonl(chatFile, messages)
                persistChat(chat.copy(messageCount = messages.size, updatedAt = Instant.now()))
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun editMessage(chatId: Uuid, messageIndex: Int, content: String): Result<Unit> {
        return updateMessageAtIndex(chatId, messageIndex) { message ->
            message.copy(content = content, timestamp = Instant.now())
        }
    }

    override suspend fun getMessageCount(chatId: Uuid): Int {
        return withContext(Dispatchers.IO) {
            val chat = getChatById(chatId) ?: return@withContext 0

            val chatFile = activeChatFile(chat)

            fileStorage.getChatLineCount(chatFile)
        }
    }

    override suspend fun deleteMessageById(chatId: Uuid, messageId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))

                val chatFile = activeChatFile(chat)

                // 读取所有消息
                val messages = fileStorage.loadMessagesFromJsonl(chatFile)
                if (messages.none { it.id == messageId }) {
                    return@withContext Result.failure(Exception("Message not found"))
                }

                // 过滤掉要删除的消息
                val filteredMessages = messages.filter { it.id != messageId }

                // 重写文件
                fileStorage.saveMessagesToJsonl(chatFile, filteredMessages)

                persistChat(chat.copy(messageCount = filteredMessages.size, updatedAt = Instant.now()))

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

                val chatFile = activeChatFile(chat)

                // 清空文件（写入空列表）
                fileStorage.saveMessagesToJsonl(chatFile, emptyList())

                persistChat(chat.copy(messageCount = 0, updatedAt = Instant.now()))

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

                val sourceFile = activeChatFile(chat)
                val messages = fileStorage.loadMessagesFromJsonl(sourceFile)

                if (fromMessageIndex < 0 || fromMessageIndex >= messages.size) {
                    return@withContext Result.failure(Exception("Invalid message index"))
                }

                val newBranchId = Uuid.random()
                val branchFile = branchChatFile(chat, newBranchId)
                val branchMessages = messages.take(fromMessageIndex + 1)
                fileStorage.saveMessagesToJsonl(branchFile, branchMessages)

                val updatedRootNodes = (branchIds(chat) + newBranchId).distinct()
                val updatedChat = chat.copy(
                    rootNodes = updatedRootNodes,
                    activeBranchId = newBranchId,
                    messageCount = branchMessages.size,
                    updatedAt = Instant.now()
                )

                persistChat(updatedChat)

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

                if (!branchIds(chat).contains(nodeId)) {
                    return@withContext Result.failure(Exception("Branch not found"))
                }

                // 更新 activeBranchId
                val branchFile = branchChatFile(chat, nodeId)
                val updatedChat = chat.copy(
                    activeBranchId = nodeId,
                    messageCount = fileStorage.getChatLineCount(branchFile),
                    updatedAt = Instant.now()
                )

                persistChat(updatedChat)

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

                val branches = branchIds(chat)
                if (branches.size <= 1) {
                    return@withContext Result.failure(Exception("Cannot delete the last branch"))
                }
                if (nodeId == chat.chatId) {
                    return@withContext Result.failure(Exception("Cannot delete the default branch"))
                }

                // 从 rootNodes 中移除
                val updatedRootNodes = branches.filter { it != nodeId }
                val nextActiveBranch = if (chat.activeBranchId == nodeId) {
                    updatedRootNodes.firstOrNull() ?: chat.chatId
                } else {
                    chat.activeBranchId ?: chat.chatId
                }
                val nextFile = branchChatFile(chat, nextActiveBranch)
                val updatedChat = chat.copy(
                    rootNodes = updatedRootNodes,
                    activeBranchId = nextActiveBranch,
                    messageCount = fileStorage.getChatLineCount(nextFile),
                    updatedAt = Instant.now()
                )

                branchChatFile(chat, nodeId).delete()
                persistChat(updatedChat)

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

            branchIds(chat).map { nodeId ->
                val branchFile = branchChatFile(chat, nodeId)
                val messages = fileStorage.loadMessagesFromJsonl(branchFile)
                MessageNode(
                    id = nodeId,
                    messages = messages.takeLast(1),
                    selectedIndex = 0,
                    branchLabel = if (nodeId == chat.chatId) {
                        "Main"
                    } else {
                        "Branch ${nodeId.toString().take(8)}"
                    }
                )
            }
        }
    }

    // ==================== 消息编辑 ====================

    override suspend fun editMessageContent(chatId: Uuid, messageId: Uuid, newContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))

                val chatFile = activeChatFile(chat)

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
        return flow {
            try {
                val chat = getChatById(chatId) ?: run {
                    emit(ChatGenerationEvent.Error(Exception("Chat not found")))
                    return@flow
                }
                val chatFile = activeChatFile(chat)
                val messages = fileStorage.loadMessagesFromJsonl(chatFile)
                val targetIndex = messages.indexOfFirst { it.id == messageId }
                if (targetIndex < 0) {
                    emit(ChatGenerationEvent.Error(Exception("Message not found")))
                    return@flow
                }

                val keepCount = if (messages[targetIndex].role == MessageRole.ASSISTANT) {
                    targetIndex
                } else {
                    targetIndex + 1
                }
                val retainedMessages = messages.take(keepCount)
                fileStorage.saveMessagesToJsonl(chatFile, retainedMessages)
                persistChat(chat.copy(messageCount = retainedMessages.size, updatedAt = Instant.now()))

                generateResponse(chatId, providerId, modelId, systemPrompt, temperature, maxTokens).collect { event ->
                    emit(event)
                }
            } catch (e: Exception) {
                emit(ChatGenerationEvent.Error(e))
            }
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
        return flow {
            try {
                val chat = getChatById(chatId) ?: run {
                    emit(ChatGenerationEvent.Error(Exception("Chat not found")))
                    return@flow
                }
                val model = requestModel(modelId)
                val messages = buildUiMessages(systemPrompt, loadAllMessages(chat))
                val request = StreamTextRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    maxTokens = maxTokens
                )
                val fullMessage = StringBuilder()
                aiSDK.streamText(request).collect { chunk ->
                    when (chunk) {
                        is TextChunk.TextDelta -> {
                            fullMessage.append(chunk.text)
                            emit(ChatGenerationEvent.Streaming(chunk.text))
                        }

                        is TextChunk.Finish -> {
                            val saved = appendAssistantMessage(chatId, fullMessage.toString()).getOrThrow()
                            emit(ChatGenerationEvent.Complete(saved))
                        }

                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                emit(ChatGenerationEvent.Error(e))
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 加载聊天的所有消息
     */
    private suspend fun updateMessageAtIndex(
        chatId: Uuid,
        messageIndex: Int,
        transform: (ChatMessage) -> ChatMessage
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = getChatById(chatId) ?: return@withContext Result.failure(Exception("Chat not found"))
                val chatFile = activeChatFile(chat)
                val messages = fileStorage.loadMessagesFromJsonl(chatFile)
                if (messageIndex !in messages.indices) {
                    return@withContext Result.failure(Exception("Message index out of range"))
                }
                val updatedMessages = messages.mapIndexed { index, message ->
                    if (index == messageIndex) transform(message) else message
                }
                fileStorage.saveMessagesToJsonl(chatFile, updatedMessages)
                persistChat(chat.copy(messageCount = updatedMessages.size, updatedAt = Instant.now()))
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun ChatMessage.toMessageNode(): MessageNode {
        val alternatives = swipeAlternatives.map { alternative ->
            copy(content = alternative, swipeAlternatives = emptyList())
        }
        return MessageNode(
            id = id,
            messages = listOf(this) + alternatives,
            selectedIndex = 0
        )
    }

    private fun requestModel(modelId: String): Model {
        val trimmedModelId = modelId.trim()
        val uuid = runCatching { Uuid.parse(trimmedModelId) }.getOrElse { Uuid.random() }
        return Model(
            id = uuid,
            modelId = trimmedModelId,
            displayName = trimmedModelId
        )
    }

    private fun buildUiMessages(systemPrompt: String, messages: List<ChatMessage>): List<UIMessage> {
        return buildList {
            systemPrompt.trim().takeIf { it.isNotEmpty() }?.let { prompt ->
                add(UIMessage.system(prompt))
            }
            messages.forEach { message ->
                add(
                    UIMessage(
                        id = message.id,
                        role = message.role,
                        parts = listOf(UIMessagePart.Text(message.content))
                    )
                )
            }
        }
    }

    private fun branchIds(chat: ChatMetadata): List<Uuid> {
        return (chat.rootNodes.ifEmpty { listOf(chat.chatId) })
            .let { ids -> if (chat.chatId in ids) ids else listOf(chat.chatId) + ids }
            .distinct()
    }

    private fun activeBranchId(chat: ChatMetadata): Uuid {
        val active = chat.activeBranchId ?: chat.chatId
        return if (active in branchIds(chat)) active else chat.chatId
    }

    private fun activeChatFile(chat: ChatMetadata): File {
        return branchChatFile(chat, activeBranchId(chat))
    }

    private fun branchChatFile(chat: ChatMetadata, branchId: Uuid): File {
        return if (branchId == chat.chatId) {
            if (chat.groupId != null) {
                fileStorage.getGroupChatFile(chat.groupId, chat.chatId)
            } else {
                fileStorage.getChatFile(chat.characterId, chat.chatId)
            }
        } else {
            if (chat.groupId != null) {
                fileStorage.getGroupChatBranchFile(chat.groupId, chat.chatId, branchId)
            } else {
                fileStorage.getChatBranchFile(chat.characterId, chat.chatId, branchId)
            }
        }
    }

    private suspend fun persistChat(chat: ChatMetadata) {
        chatDao.insertChat(ChatEntity.fromModel(chat))
    }

    /**
     * 加载聊天的所有消息
     */
    private suspend fun loadAllMessages(chat: ChatMetadata): List<ChatMessage> {
        return fileStorage.loadMessagesFromJsonl(activeChatFile(chat))
    }
}
