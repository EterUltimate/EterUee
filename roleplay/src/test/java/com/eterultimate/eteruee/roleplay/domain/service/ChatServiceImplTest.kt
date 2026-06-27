package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.GenerateObjectRequest
import com.eterultimate.eteruee.ai.sdk.GenerateTextRequest
import com.eterultimate.eteruee.ai.sdk.GenerateTextResult
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.ChatDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity
import com.eterultimate.eteruee.roleplay.data.model.ChatGenerationEvent
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatServiceImplTest {
    @Test
    fun generateResponsePersistsPartialAssistantMessageWhenStreamFails() = runTest {
        val filesDir = Files.createTempDirectory("eteruee-roleplay-test").toFile()
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns filesDir

            val chatDao = InMemoryChatDao()
            val service = ChatServiceImpl(
                context = context,
                chatDao = chatDao,
                fileStorage = RolePlayFileStorage(context),
                aiSDK = failingAfterDeltaSdk(),
            )
            val chat = service.createChat(
                characterId = Uuid.random(),
                groupId = null,
                title = "Partial stream",
            ).getOrThrow()
            service.appendUserMessage(chat.chatId, "hello").getOrThrow()

            val events = service.generateResponse(
                chatId = chat.chatId,
                providerId = "unused",
                modelId = "gpt-test",
                systemPrompt = "You are helpful.",
                temperature = 0.7f,
                maxTokens = 128,
            ).toList()

            val complete = events.filterIsInstance<ChatGenerationEvent.Complete>().single()
            assertEquals("partial reply", complete.fullMessage.content)
            assertTrue(events.last() is ChatGenerationEvent.Error)

            val saved = service.exportMessages(chat.chatId).getOrThrow()
            assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT), saved.map { it.role })
            assertEquals("partial reply", saved.last().content)
        } finally {
            filesDir.deleteRecursively()
        }
    }

    private fun failingAfterDeltaSdk(): AISDK = object : AISDK {
        override suspend fun generateText(request: GenerateTextRequest): GenerateTextResult {
            error("Not used")
        }

        override fun streamText(request: StreamTextRequest): Flow<TextChunk> = flow {
            emit(TextChunk.TextDelta("partial "))
            emit(TextChunk.TextDelta("reply"))
            throw IOException("stream interrupted")
        }

        override suspend fun generateObject(request: GenerateObjectRequest): JsonObject {
            error("Not used")
        }
    }
}

private class InMemoryChatDao : ChatDAO {
    private val chats = MutableStateFlow<List<ChatEntity>>(emptyList())

    override fun getChatsByCharacter(characterId: String): Flow<List<ChatEntity>> =
        chats.map { list -> list.filter { it.characterId == characterId }.sortedByDescending { it.updatedAt } }

    override fun getChatsByGroup(groupId: String): Flow<List<ChatEntity>> =
        chats.map { list -> list.filter { it.groupId == groupId }.sortedByDescending { it.updatedAt } }

    override suspend fun getChatById(id: String): ChatEntity? =
        chats.value.firstOrNull { it.id == id }

    override suspend fun insertChat(entity: ChatEntity) {
        chats.value = chats.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun insertChats(entities: List<ChatEntity>) {
        entities.forEach { insertChat(it) }
    }

    override suspend fun deleteChat(entity: ChatEntity) {
        deleteChatById(entity.id)
    }

    override suspend fun deleteChatById(id: String) {
        chats.value = chats.value.filterNot { it.id == id }
    }

    override suspend fun deleteChatsByCharacter(characterId: String) {
        chats.value = chats.value.filterNot { it.characterId == characterId }
    }

    override fun getPinnedChats(): Flow<List<ChatEntity>> =
        chats.map { list -> list.filter { it.pinned }.sortedByDescending { it.updatedAt } }

    override suspend fun updateMessageCount(id: String, count: Int, updatedAt: Long) {
        getChatById(id)?.let { entity ->
            insertChat(entity.copy(messageCount = count, updatedAt = updatedAt))
        }
    }

    override suspend fun searchChats(query: String): List<ChatEntity> =
        chats.value.filter { it.title.contains(query, ignoreCase = true) }.sortedByDescending { it.updatedAt }
}
