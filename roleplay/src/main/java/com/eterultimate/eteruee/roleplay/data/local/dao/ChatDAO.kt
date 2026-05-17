package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * 聊天数据访问对象
 */
@Dao
interface ChatDAO {
    /**
     * 获取角色的所有聊天
     */
    @Query("SELECT * FROM rp_chats WHERE characterId = :characterId ORDER BY updatedAt DESC")
    fun getChatsByCharacter(characterId: String): Flow<List<ChatEntity>>
    
    /**
     * 获取群组的所有聊天
     */
    @Query("SELECT * FROM rp_chats WHERE groupId = :groupId ORDER BY updatedAt DESC")
    fun getChatsByGroup(groupId: String): Flow<List<ChatEntity>>
    
    /**
     * 根据ID获取聊天
     */
    @Query("SELECT * FROM rp_chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?
    
    /**
     * 插入或更新聊天
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(entity: ChatEntity)
    
    /**
     * 批量插入聊天
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(entities: List<ChatEntity>)
    
    /**
     * 删除聊天
     */
    @Delete
    suspend fun deleteChat(entity: ChatEntity)
    
    /**
     * 根据ID删除聊天
     */
    @Query("DELETE FROM rp_chats WHERE id = :id")
    suspend fun deleteChatById(id: String)
    
    /**
     * 删除角色的所有聊天
     */
    @Query("DELETE FROM rp_chats WHERE characterId = :characterId")
    suspend fun deleteChatsByCharacter(characterId: String)
    
    /**
     * 获取置顶的聊天
     */
    @Query("SELECT * FROM rp_chats WHERE pinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedChats(): Flow<List<ChatEntity>>
    
    /**
     * 更新聊天消息计数
     */
    @Query("UPDATE rp_chats SET messageCount = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMessageCount(id: String, count: Int, updatedAt: Long)
    
    /**
     * 搜索聊天(按标题)
     */
    @Query("SELECT * FROM rp_chats WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchChats(query: String): List<ChatEntity>
}
