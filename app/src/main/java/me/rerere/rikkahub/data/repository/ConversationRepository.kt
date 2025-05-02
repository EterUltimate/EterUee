package me.rerere.rikkahub.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.deleteAllChatFiles
import me.rerere.rikkahub.utils.deleteChatFiles
import java.time.Instant
import kotlin.uuid.Uuid

class ConversationRepository(
    private val context: Context,
    private val conversationDAO: ConversationDAO,
) {
    fun getAllConversations(): Flow<List<Conversation>> = conversationDAO
        .getAll()
        .map { flow ->
            flow.map { entity ->
                Conversation(
                    id = Uuid.parse(entity.id),
                    assistantId = Uuid.parse(entity.assistantId),
                    title = entity.title,
                    messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                    tokenUsage = entity.tokenUsage,
                    createAt = Instant.ofEpochMilli(entity.createAt),
                    updateAt = Instant.ofEpochMilli(entity.updateAt),
                )
            }
        }

    fun getConversationsOfAssistant(assistantId: Uuid): Flow<List<Conversation>> {
        return conversationDAO
            .getConversationsOfAssistant(assistantId.toString())
            .map { flow ->
                flow.map { entity ->
                    Conversation(
                        id = Uuid.parse(entity.id),
                        assistantId = Uuid.parse(entity.assistantId),
                        title = entity.title,
                        messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                        tokenUsage = entity.tokenUsage,
                        createAt = Instant.ofEpochMilli(entity.createAt),
                        updateAt = Instant.ofEpochMilli(entity.updateAt),
                    )
                }
            }
    }

    fun searchConversations(titleKeyword: String): Flow<List<Conversation>> {
        return conversationDAO
            .searchConversations(titleKeyword)
            .map { flow ->
                flow.map { entity ->
                    Conversation(
                        id = Uuid.parse(entity.id),
                        assistantId = Uuid.parse(entity.assistantId),
                        title = entity.title,
                        messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                        tokenUsage = entity.tokenUsage,
                        createAt = Instant.ofEpochMilli(entity.createAt),
                        updateAt = Instant.ofEpochMilli(entity.updateAt),
                    )
                }
            }
    }

    suspend fun getConversationById(uuid: Uuid): Conversation? {
        val entity = conversationDAO.getConversationById(uuid.toString())
        return if (entity != null) {
            Conversation(
                id = Uuid.parse(entity.id),
                title = entity.title,
                messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                tokenUsage = entity.tokenUsage,
                createAt = Instant.ofEpochMilli(entity.createAt),
                updateAt = Instant.ofEpochMilli(entity.updateAt),
                assistantId = Uuid.parse(entity.assistantId)
            )
        } else null
    }

    suspend fun insertConversation(conversation: Conversation) {
        conversationDAO.insert(
            ConversationEntity(
                id = conversation.id.toString(),
                title = conversation.title,
                messages = JsonInstant.encodeToString(conversation.messages),
                createAt = conversation.createAt.toEpochMilli(),
                updateAt = conversation.updateAt.toEpochMilli(),
                tokenUsage = conversation.tokenUsage,
                assistantId = conversation.assistantId.toString()
            )
        )
    }

    suspend fun updateConversation(conversation: Conversation) {
        conversationDAO.update(
            ConversationEntity(
                id = conversation.id.toString(),
                title = conversation.title,
                messages = JsonInstant.encodeToString(conversation.messages),
                createAt = conversation.createAt.toEpochMilli(),
                updateAt = conversation.updateAt.toEpochMilli(),
                tokenUsage = conversation.tokenUsage,
                assistantId = conversation.assistantId.toString()
            )
        )
    }

    suspend fun deleteConversation(conversation: Conversation) {
        conversationDAO.delete(
            ConversationEntity(
                id = conversation.id.toString(),
                title = conversation.title,
                messages = JsonInstant.encodeToString(conversation.messages),
                createAt = conversation.createAt.toEpochMilli(),
                updateAt = conversation.updateAt.toEpochMilli(),
                tokenUsage = conversation.tokenUsage,
                assistantId = conversation.assistantId.toString()
            )
        )
        context.deleteChatFiles(conversation.files)
    }

    suspend fun deleteAllConversations() {
        conversationDAO.deleteAll()
        context.deleteAllChatFiles()
    }
}