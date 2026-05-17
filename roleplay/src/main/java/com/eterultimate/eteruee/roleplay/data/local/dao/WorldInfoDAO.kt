package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.WorldInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 世界书数据访问对象
 */
@Dao
interface WorldInfoDAO {
    /**
     * 获取所有世界书
     */
    @Query("SELECT * FROM rp_world_infos ORDER BY updatedAt DESC")
    fun getAllWorldInfos(): Flow<List<WorldInfoEntity>>
    
    /**
     * 根据ID获取世界书
     */
    @Query("SELECT * FROM rp_world_infos WHERE id = :id")
    suspend fun getWorldInfoById(id: String): WorldInfoEntity?
    
    /**
     * 插入或更新世界书
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldInfo(entity: WorldInfoEntity)
    
    /**
     * 删除世界书
     */
    @Delete
    suspend fun deleteWorldInfo(entity: WorldInfoEntity)
    
    /**
     * 根据ID删除世界书
     */
    @Query("DELETE FROM rp_world_infos WHERE id = :id")
    suspend fun deleteWorldInfoById(id: String)
    
    /**
     * 搜索世界书(按名称)
     */
    @Query("SELECT * FROM rp_world_infos WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchWorldInfos(query: String): List<WorldInfoEntity>
}
