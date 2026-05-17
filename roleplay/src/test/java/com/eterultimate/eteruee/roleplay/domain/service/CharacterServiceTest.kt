package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.CharacterDAO
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * CharacterService 单元测试
 */
class CharacterServiceTest {
    
    private lateinit var characterDao: CharacterDAO
    private lateinit var fileStorage: RolePlayFileStorage
    private lateinit var characterService: CharacterServiceImpl
    
    @Before
    fun setUp() {
        characterDao = mockk(relaxed = true)
        fileStorage = mockk(relaxed = true)
        characterService = CharacterServiceImpl(
            context = mockk(relaxed = true),
            characterDao = characterDao,
            fileStorage = fileStorage
        )
    }
    
    @Test
    fun `test create character success`() = runTest {
        // Given
        val name = "Test Character"
        val description = "A test character"
        
        // When
        val result = characterService.createCharacter(name, description)
        
        // Then
        assertTrue(result.isSuccess)
        val character = result.getOrNull()
        assertNotNull(character)
        assertEquals(name, character?.name)
        assertEquals(description, character?.description)
    }
    
    @Test
    fun `test update character success`() = runTest {
        // Given
        val originalCharacter = Character(
            id = Uuid.random(),
            name = "Original Name",
            description = "Original Description",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        every { characterDao.getCharacterById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.CharacterEntity.fromModel(originalCharacter)
        
        val updatedCharacter = originalCharacter.copy(name = "Updated Name")
        
        // When
        val result = characterService.updateCharacter(updatedCharacter)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated Name", result.getOrNull()?.name)
    }
    
    @Test
    fun `test delete character success`() = runTest {
        // Given
        val characterId = Uuid.random()
        
        // When
        val result = characterService.deleteCharacter(characterId)
        
        // Then
        assertTrue(result.isSuccess)
        verify { characterDao.deleteCharacter(characterId.toString()) }
    }
    
    @Test
    fun `test get character by id not found`() = runTest {
        // Given
        val characterId = Uuid.random()
        every { characterDao.getCharacterById(any()) } returns null
        
        // When
        val result = characterService.getCharacterById(characterId)
        
        // Then
        assertNull(result)
    }
}
