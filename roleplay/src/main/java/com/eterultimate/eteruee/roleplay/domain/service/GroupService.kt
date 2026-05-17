package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import kotlinx.coroutines.flow.Flow

/**
 * 群组服务接口
 */
interface GroupService {
    /**
     * 获取所有群组
     */
    fun getAllGroups(): Flow<List<Group>>
    
    /**
     * 根据ID获取群组
     */
    suspend fun getGroupById(groupId: kotlin.uuid.Uuid): Group?
    
    /**
     * 创建群组
     */
    suspend fun createGroup(name: String, description: String = ""): Result<Group>
    
    /**
     * 更新群组
     */
    suspend fun updateGroup(group: Group): Result<Group>
    
    /**
     * 删除群组
     */
    suspend fun deleteGroup(groupId: kotlin.uuid.Uuid): Result<Unit>
    
    // ==================== 成员管理 ====================
    
    /**
     * 添加成员
     */
    suspend fun addMember(groupId: kotlin.uuid.Uuid, member: GroupMember): Result<Group>
    
    /**
     * 更新成员
     */
    suspend fun updateMember(groupId: kotlin.uuid.Uuid, characterId: kotlin.uuid.Uuid, member: GroupMember): Result<Group>
    
    /**
     * 删除成员
     */
    suspend fun removeMember(groupId: kotlin.uuid.Uuid, characterId: kotlin.uuid.Uuid): Result<Group>
    
    /**
     * 切换成员激活状态
     */
    suspend fun toggleMemberActive(groupId: kotlin.uuid.Uuid, characterId: kotlin.uuid.Uuid): Result<Group>
    
    // ==================== 聊天管理 ====================
    
    /**
     * 创建群组聊天
     */
    suspend fun createGroupChat(groupId: kotlin.uuid.Uuid, title: String = ""): Result<kotlin.uuid.Uuid>
    
    /**
     * 获取群组的聊天列表
     */
    suspend fun getGroupChats(groupId: kotlin.uuid.Uuid): List<com.eterultimate.eteruee.roleplay.data.model.ChatMetadata>
    
    /**
     * 决定下一个发言的角色(基于优先级和概率)
     */
    fun selectNextSpeaker(group: Group): GroupMember?
}
