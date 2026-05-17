package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.local.dao.WorldInfoDAO
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * WorldInfoService 单元测试
 */
class WorldInfoServiceTest {
    
    private lateinit var worldInfoDao: WorldInfoDAO
    private lateinit var worldInfoService: WorldInfoServiceImpl
    
    @Before
    fun setUp() {
        worldInfoDao = mockk(relaxed = true)
        worldInfoService = WorldInfoServiceImpl(
            context = mockk(relaxed = true),
            worldInfoDao = worldInfoDao
        )
    }
    
    @Test
    fun `test create world info success`() = runTest {
        // Given
        val name = "Test World"
        val description = "A test world"
        
        // When
        val result = worldInfoService.createWorldInfo(name, description)
        
        // Then
        assertTrue(result.isSuccess)
        val worldInfo = result.getOrNull()
        assertNotNull(worldInfo)
        assertEquals(name, worldInfo?.name)
        assertEquals(description, worldInfo?.description)
    }
    
    @Test
    fun `test delete world info success`() = runTest {
        // Given
        val worldInfoId = Uuid.random()
        
        // When
        val result = worldInfoService.deleteWorldInfo(worldInfoId)
        
        // Then
        assertTrue(result.isSuccess)
        verify { worldInfoDao.deleteWorldInfo(worldInfoId.toString()) }
    }
    
    @Test
    fun `test update world info success`() = runTest {
        // Given
        val worldInfo = WorldInfo(
            id = Uuid.random(),
            name = "Original Name",
            description = "Original Description",
            entries = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        every { worldInfoDao.getWorldInfoById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.WorldInfoEntity.fromModel(worldInfo)
        
        val updatedWorldInfo = worldInfo.copy(name = "Updated Name")
        
        // When
        val result = worldInfoService.updateWorldInfo(updatedWorldInfo)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated Name", result.getOrNull()?.name)
    }
    
    @Test
    fun `test add entry success`() = runTest {
        // Given
        val worldInfoId = Uuid.random()
        val entry = WorldInfoEntry(
            id = Uuid.random(),
            key = "test_key",
            content = "Test content",
            enabled = true,
            order = 0
        )
        
        every { worldInfoDao.getWorldInfoById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.WorldInfoEntity(
                id = worldInfoId.toString(),
                name = "Test World",
                description = "",
                entriesJson = "[]",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        
        // When
        val result = worldInfoService.addEntry(worldInfoId, entry)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `test is entry matched with keyword`() {
        // Given
        val entry = WorldInfoEntry(
            id = Uuid.random(),
            key = "magic|spell",
            content = "Magic content",
            enabled = true,
            order = 0
        )
        val text = "The wizard cast a magic spell"
        
        // When
        val matched = worldInfoService.isEntryMatched(entry, text)
        
        // Then
        assertTrue(matched)
    }
    
    @Test
    fun `test is entry matched with no match`() {
        // Given
        val entry = WorldInfoEntry(
            id = Uuid.random(),
            key = "dragon",
            content = "Dragon content",
            enabled = true,
            order = 0
        )
        val text = "The wizard cast a spell"
        
        // When
        val matched = worldInfoService.isEntryMatched(entry, text)
        
        // Then
        assertFalse(matched)
    }
    
    @Test
    fun `test match entries filters correctly`() {
        // Given
        val entries = listOf(
            WorldInfoEntry(
                id = Uuid.random(),
                key = "magic",
                content = "Magic content",
                enabled = true,
                order = 0
            ),
            WorldInfoEntry(
                id = Uuid.random(),
                key = "dragon",
                content = "Dragon content",
                enabled = true,
                order = 1
            )
        )
        val text = "The wizard used magic"
        
        // When
        val matched = worldInfoService.matchEntries(entries, text)
        
        // Then
        assertEquals(1, matched.size)
        assertEquals("magic", matched[0].key)
    }
}
