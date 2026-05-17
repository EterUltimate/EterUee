package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import com.eterultimate.eteruee.roleplay.data.local.dao.GroupDAO
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * GroupService 单元测试
 */
class GroupServiceTest {
    
    private lateinit var groupDao: GroupDAO
    private lateinit var groupService: GroupServiceImpl
    
    @Before
    fun setUp() {
        groupDao = mockk(relaxed = true)
        groupService = GroupServiceImpl(
            context = mockk(relaxed = true),
            groupDao = groupDao
        )
    }
    
    @Test
    fun `test create group success`() = runTest {
        // Given
        val name = "Test Group"
        val description = "A test group"
        
        // When
        val result = groupService.createGroup(name, description)
        
        // Then
        assertTrue(result.isSuccess)
        val group = result.getOrNull()
        assertNotNull(group)
        assertEquals(name, group?.name)
        assertEquals(description, group?.description)
    }
    
    @Test
    fun `test delete group success`() = runTest {
        // Given
        val groupId = Uuid.random()
        
        // When
        val result = groupService.deleteGroup(groupId)
        
        // Then
        assertTrue(result.isSuccess)
        verify { groupDao.deleteGroup(groupId.toString()) }
    }
    
    @Test
    fun `test update group success`() = runTest {
        // Given
        val group = Group(
            id = Uuid.random(),
            name = "Original Name",
            description = "Original Description",
            members = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        every { groupDao.getGroupById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity.fromModel(group)
        
        val updatedGroup = group.copy(name = "Updated Name")
        
        // When
        val result = groupService.updateGroup(updatedGroup)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated Name", result.getOrNull()?.name)
    }
    
    @Test
    fun `test add member success`() = runTest {
        // Given
        val groupId = Uuid.random()
        val characterId = Uuid.random()
        val member = GroupMember(
            characterId = characterId,
            active = true,
            priority = 1,
            probability = 1.0f
        )
        
        every { groupDao.getGroupById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity(
                id = groupId.toString(),
                name = "Test Group",
                description = "",
                membersJson = "[]",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        
        // When
        val result = groupService.addMember(groupId, member)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `test remove member success`() = runTest {
        // Given
        val groupId = Uuid.random()
        val characterId = Uuid.random()
        
        every { groupDao.getGroupById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity(
                id = groupId.toString(),
                name = "Test Group",
                description = "",
                membersJson = """[{"characterId":"${characterId}","active":true,"priority":1,"probability":1.0}]""",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        
        // When
        val result = groupService.removeMember(groupId, characterId)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `test toggle member active success`() = runTest {
        // Given
        val groupId = Uuid.random()
        val characterId = Uuid.random()
        
        every { groupDao.getGroupById(any()) } returns 
            com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity(
                id = groupId.toString(),
                name = "Test Group",
                description = "",
                membersJson = """[{"characterId":"${characterId}","active":true,"priority":1,"probability":1.0}]""",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        
        // When
        val result = groupService.toggleMemberActive(groupId, characterId)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `test select next speaker with single member`() {
        // Given
        val characterId = Uuid.random()
        val group = Group(
            id = Uuid.random(),
            name = "Test Group",
            description = "",
            members = listOf(
                GroupMember(
                    characterId = characterId,
                    active = true,
                    priority = 1,
                    probability = 1.0f
                )
            ),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // When
        val speaker = groupService.selectNextSpeaker(group)
        
        // Then
        assertNotNull(speaker)
        assertEquals(characterId, speaker?.characterId)
    }
    
    @Test
    fun `test select next speaker with multiple members`() {
        // Given
        val characterId1 = Uuid.random()
        val characterId2 = Uuid.random()
        val group = Group(
            id = Uuid.random(),
            name = "Test Group",
            description = "",
            members = listOf(
                GroupMember(
                    characterId = characterId1,
                    active = true,
                    priority = 1,
                    probability = 0.7f
                ),
                GroupMember(
                    characterId = characterId2,
                    active = true,
                    priority = 2,
                    probability = 0.3f
                )
            ),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // When
        val speaker = groupService.selectNextSpeaker(group)
        
        // Then
        assertNotNull(speaker)
        // Higher priority should be selected first
        assertEquals(characterId1, speaker?.characterId)
    }
    
    @Test
    fun `test select next speaker with no active members`() {
        // Given
        val group = Group(
            id = Uuid.random(),
            name = "Test Group",
            description = "",
            members = listOf(
                GroupMember(
                    characterId = Uuid.random(),
                    active = false,
                    priority = 1,
                    probability = 1.0f
                )
            ),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // When
        val speaker = groupService.selectNextSpeaker(group)
        
        // Then
        assertNull(speaker)
    }
}
