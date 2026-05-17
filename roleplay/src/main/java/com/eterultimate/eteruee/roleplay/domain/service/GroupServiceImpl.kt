package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.ChatDAO
import com.eterultimate.eteruee.roleplay.data.local.dao.GroupDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity
import com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity
import com.eterultimate.eteruee.roleplay.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 群组服务实现
 */
class GroupServiceImpl(
    private val context: Context,
    private val groupDao: GroupDAO,
    private val chatDao: ChatDAO,
    private val fileStorage: RolePlayFileStorage
) : GroupService {
    
    override fun getAllGroups(): Flow<List<Group>> {
        return groupDao.getAllGroups().map { entities ->
            entities.map { GroupEntity.toModel(it) }
        }
    }
    
    override suspend fun getGroupById(groupId: kotlin.uuid.Uuid): Group? {
        return withContext(Dispatchers.IO) {
            val entity = groupDao.getGroupById(groupId.toString())
            entity?.let { GroupEntity.toModel(it) }
        }
    }
    
    override suspend fun createGroup(name: String, description: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = Group(
                    name = name,
                    description = description,
                    members = emptyList(),
                    activeMembers = emptySet(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                
                // 保存到数据库
                val entity = GroupEntity.fromModel(group)
                groupDao.insertGroup(entity)
                
                // 保存JSON文件
                fileStorage.saveGroupJson(group)
                
                Result.success(group)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateGroup(group: Group): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedGroup = group.copy(updatedAt = Instant.now())
                
                // 更新数据库
                val entity = GroupEntity.fromModel(updatedGroup)
                groupDao.insertGroup(entity)
                
                // 更新JSON文件
                fileStorage.saveGroupJson(updatedGroup)
                
                Result.success(updatedGroup)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteGroup(groupId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 删除数据库记录
                groupDao.deleteGroupById(groupId.toString())
                
                // 删除JSON文件
                fileStorage.deleteGroupDir(groupId)
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 成员管理 ====================
    
    override suspend fun addMember(groupId: kotlin.uuid.Uuid, member: GroupMember): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = getGroupById(groupId) 
                    ?: return@withContext Result.failure(Exception("Group not found"))
                
                // 检查是否已存在
                if (group.members.any { it.characterId == member.characterId }) {
                    return@withContext Result.failure(Exception("Member already exists"))
                }
                
                val updatedMembers = group.members + member
                val updatedGroup = group.copy(members = updatedMembers)
                
                updateGroup(updatedGroup)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateMember(
        groupId: kotlin.uuid.Uuid,
        characterId: kotlin.uuid.Uuid,
        member: GroupMember
    ): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = getGroupById(groupId) 
                    ?: return@withContext Result.failure(Exception("Group not found"))
                
                val updatedMembers = group.members.map { existing ->
                    if (existing.characterId == characterId) member else existing
                }
                
                val updatedGroup = group.copy(members = updatedMembers)
                updateGroup(updatedGroup)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun removeMember(groupId: kotlin.uuid.Uuid, characterId: kotlin.uuid.Uuid): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = getGroupById(groupId) 
                    ?: return@withContext Result.failure(Exception("Group not found"))
                
                val updatedMembers = group.members.filter { it.characterId != characterId }
                val updatedActiveMembers = group.activeMembers - characterId
                
                val updatedGroup = group.copy(
                    members = updatedMembers,
                    activeMembers = updatedActiveMembers
                )
                
                updateGroup(updatedGroup)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun toggleMemberActive(groupId: kotlin.uuid.Uuid, characterId: kotlin.uuid.Uuid): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = getGroupById(groupId) 
                    ?: return@withContext Result.failure(Exception("Group not found"))
                
                val updatedActiveMembers = if (group.activeMembers.contains(characterId)) {
                    group.activeMembers - characterId
                } else {
                    group.activeMembers + characterId
                }
                
                val updatedGroup = group.copy(activeMembers = updatedActiveMembers)
                updateGroup(updatedGroup)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 聊天管理 ====================
    
    override suspend fun createGroupChat(groupId: kotlin.uuid.Uuid, title: String): Result<kotlin.uuid.Uuid> {
        return withContext(Dispatchers.IO) {
            try {
                val group = getGroupById(groupId) 
                    ?: return@withContext Result.failure(Exception("Group not found"))
                
                val chatId = Uuid.random()
                val chat = ChatMetadata(
                    chatId = chatId,
                    characterId = Uuid.NIL,  // 群组聊天没有单一角色，使用 NIL
                    groupId = groupId,
                    title = title.ifBlank { group.name },
                    messageCount = 0,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                
                // 保存到数据库
                val entity = ChatEntity.fromModel(chat)
                chatDao.insertChat(entity)
                
                // 创建JSONL文件
                val chatFile = fileStorage.getGroupChatFile(groupId, chatId)
                chatFile.createNewFile()
                
                Result.success(chatId)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getGroupChats(groupId: Uuid): List<ChatMetadata> {
        return withContext(Dispatchers.IO) {
            // chatDao.getChatsByGroup 返回 Flow<List<ChatEntity>>
            // 我们需要在 suspend 函数中获取一次值
            var result: List<ChatMetadata> = emptyList()
            chatDao.getChatsByGroup(groupId.toString()).collect { entities ->
                result = entities.map { ChatEntity.toModel(it) }
                return@collect // 只取第一次发射的值
            }
            result
        }
    }
    
    override fun selectNextSpeaker(group: Group): GroupMember? {
        // 只从活跃成员中选择
        val activeMembers = group.members.filter { it.characterId in group.activeMembers }
        
        if (activeMembers.isEmpty()) {
            return null
        }
        
        // 过滤出强制响应的成员
        val forcedMembers = activeMembers.filter { it.forcedResponse }
        if (forcedMembers.isNotEmpty()) {
            // 从强制响应成员中按优先级选择
            return forcedMembers.maxByOrNull { it.priority }
        }
        
        // 按概率选择
        val candidates = activeMembers.filter { Math.random() <= it.responseProbability }
        
        if (candidates.isEmpty()) {
            // 如果没有候选者,选择优先级最高的
            return activeMembers.maxByOrNull { it.priority }
        }
        
        // 从候选者中按优先级选择
        return candidates.maxByOrNull { it.priority }
    }
}
