package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.ChatMetadata
import com.eterultimate.eteruee.roleplay.data.model.MessageNode
import com.eterultimate.eteruee.roleplay.data.local.dao.ChatDAO
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * ChatService 单元测试
 */
class ChatServiceTest {
    
    private lateinit var chatDao: ChatDAO
    private lateinit var chatService: ChatServiceImpl
    
    @Before
    fun setUp() {
        chatDao = mockk(relaxed = true)
        chatService = ChatServiceImpl(
            context = mockk(relaxed = true),
            chatDao = chatDao
        )
    }
    
    @Test
    fun `test create chat success`() = runTest {
        // Given
        val characterId = Uuid.random()
        val title = "Test Chat"
        
        // When
        val result = chatService.createChat(characterId, title = title)
        
        // Then
        assertTrue(result.isSuccess)
        val chat = result.getOrNull()
        assertNotNull(chat)
        assertEquals(title, chat?.title)
        assertEquals(characterId.toString(), chat?.characterId)
    }
    
    @Test
    fun `test delete chat success`() = runTest {
        // Given
        val chatId = Uuid.random()
        
        // When
        val result = chatService.deleteChat(chatId)
        
        // Then
        assertTrue(result.isSuccess)
        verify { chatDao.deleteChat(chatId.toString()) }
    }
    
    @Test
    fun `test update chat title success`() = runTest {
        // Given
        val chatId = Uuid.random()
        val newTitle = "Updated Title"
        
        every { chatDao.getChatById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity(
                id = chatId.toString(),
                characterId = Uuid.random().toString(),
                groupId = null,
                title = newTitle,
                pinned = false,
                messageCount = 0,
                lastMessageAt = Instant.now().toString(),
                createdAt = Instant.now().toString()
            )
        
        // When
        val result = chatService.updateChatTitle(chatId, newTitle)
        
        // Then
        assertTrue(result.isSuccess)
        verify { chatDao.updateChatTitle(chatId.toString(), newTitle) }
    }
    
    @Test
    fun `test toggle pin success`() = runTest {
        // Given
        val chatId = Uuid.random()
        val currentPinned = false
        
        every { chatDao.getChatById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity(
                id = chatId.toString(),
                characterId = Uuid.random().toString(),
                groupId = null,
                title = "Test",
                pinned = currentPinned,
                messageCount = 0,
                lastMessageAt = Instant.now().toString(),
                createdAt = Instant.now().toString()
            )
        
        // When
        val result = chatService.togglePin(chatId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        verify { chatDao.updateChatPinned(chatId.toString(), true) }
    }
    
    @Test
    fun `test get chats by character`() = runTest {
        // Given
        val characterId = Uuid.random()
        val expectedChats = listOf(
            ChatMetadata(
                id = Uuid.random(),
                characterId = characterId,
                groupId = null,
                title = "Chat 1",
                pinned = false,
                messageCount = 5,
                lastMessageAt = Instant.now(),
                createdAt = Instant.now()
            )
        )
        
        every { chatDao.getChatsByCharacter(any()) } returns flowOf(expectedChats.map { 
            com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity.fromModel(it)
        })
        
        // When
        val result = chatService.getChatsByCharacter(characterId)
        
        // Then
        assertNotNull(result)
    }
}
