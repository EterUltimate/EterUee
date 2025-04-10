package me.rerere.rikkahub.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.utils.JsonInstant
import java.time.Instant
import kotlin.uuid.Uuid

class ConversationRepository(
    private val conversationDAO: ConversationDAO
) {
    fun getAllConversations() = conversationDAO
        .getAll()
        .map { flow ->
            flow.map { entity ->
                Conversation(
                    id = Uuid.parse(entity.id),
                    title = entity.title,
                    messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                    createAt = Instant.ofEpochMilli(entity.createAt),
                    updateAt = Instant.ofEpochMilli(entity.updateAt),
                )
            }
        }

    fun searchConversations(titleKeyword: String): Flow<List<Conversation>> {
        return conversationDAO
            .searchConversations(titleKeyword)
            .map { flow ->
                flow.map { entity ->
                    Conversation(
                        id = Uuid.parse(entity.id),
                        title = entity.title,
                        messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                        createAt = Instant.ofEpochMilli(entity.createAt),
                        updateAt = Instant.ofEpochMilli(entity.updateAt),
                    )
                }
            }
    }

    fun getConversationFlowById(uuid: Uuid) = conversationDAO
        .getConversationFlowById(uuid.toString())
        .map { entity ->
            if(entity != null) Conversation(
                id = Uuid.parse(entity.id),
                title = entity.title,
                messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                createAt = Instant.ofEpochMilli(entity.createAt),
                updateAt = Instant.ofEpochMilli(entity.updateAt),
            ) else Conversation.ofId(uuid)
        }

    suspend fun getConversationById(uuid: Uuid): Conversation? {
        val entity = conversationDAO.getConversationById(uuid.toString())
        return if (entity != null) {
            Conversation(
                id = Uuid.parse(entity.id),
                title = entity.title,
                messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                createAt = Instant.ofEpochMilli(entity.createAt),
                updateAt = Instant.ofEpochMilli(entity.updateAt),
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
            )
        )
    }
}