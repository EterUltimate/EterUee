package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * 群组数据访问对象
 */
@Dao
interface GroupDAO {
    /**
     * 获取所有群组
     */
    @Query("SELECT * FROM rp_groups ORDER BY updatedAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    /**
     * 根据ID获取群组
     */
    @Query("SELECT * FROM rp_groups WHERE id = :id")
    suspend fun getGroupById(id: String): GroupEntity?
    
    /**
     * 插入或更新群组
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(entity: GroupEntity)
    
    /**
     * 删除群组
     */
    @Delete
    suspend fun deleteGroup(entity: GroupEntity)
    
    /**
     * 根据ID删除群组
     */
    @Query("DELETE FROM rp_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)
    
    /**
     * 搜索群组(按名称)
     */
    @Query("SELECT * FROM rp_groups WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchGroups(query: String): List<GroupEntity>
}
