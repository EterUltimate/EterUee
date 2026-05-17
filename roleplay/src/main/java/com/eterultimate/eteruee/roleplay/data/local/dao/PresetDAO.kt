package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 预设数据访问对象
 */
@Dao
interface PresetDAO {
    
    /**
     * 插入预设
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)
    
    /**
     * 批量插入预设
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)
    
    /**
     * 更新预设
     */
    @Update
    suspend fun updatePreset(preset: PresetEntity)
    
    /**
     * 删除预设
     */
    @Delete
    suspend fun deletePreset(preset: PresetEntity)
    
    /**
     * 根据ID删除预设
     */
    @Query("DELETE FROM rp_presets WHERE id = :id")
    suspend fun deletePresetById(id: String)
    
    /**
     * 根据ID获取预设
     */
    @Query("SELECT * FROM rp_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?
    
    /**
     * 获取所有预设(Flow)
     */
    @Query("SELECT * FROM rp_presets ORDER BY updatedAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>
    
    /**
     * 获取所有预设(挂起函数)
     */
    @Query("SELECT * FROM rp_presets ORDER BY updatedAt DESC")
    suspend fun getAllPresetsList(): List<PresetEntity>
    
    /**
     * 根据类型获取预设
     */
    @Query("SELECT * FROM rp_presets WHERE type = :type ORDER BY updatedAt DESC")
    fun getPresetsByType(type: String): Flow<List<PresetEntity>>
    
    /**
     * 根据类型获取预设(挂起函数)
     */
    @Query("SELECT * FROM rp_presets WHERE type = :type ORDER BY updatedAt DESC")
    suspend fun getPresetsByTypeList(type: String): List<PresetEntity>
    
    /**
     * 搜索预设
     */
    @Query("SELECT * FROM rp_presets WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchPresets(query: String): List<PresetEntity>
    
    /**
     * 获取预设数量
     */
    @Query("SELECT COUNT(*) FROM rp_presets")
    suspend fun getPresetCount(): Int
    
    /**
     * 获取指定类型的预设数量
     */
    @Query("SELECT COUNT(*) FROM rp_presets WHERE type = :type")
    suspend fun getPresetCountByType(type: String): Int
}
