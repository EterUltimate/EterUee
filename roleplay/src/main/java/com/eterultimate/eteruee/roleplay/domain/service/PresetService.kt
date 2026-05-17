package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import kotlinx.coroutines.flow.Flow

/**
 * 预设服务接口
 */
interface PresetService {
    
    /**
     * 获取所有预设(Flow)
     */
    fun getAllPresets(): Flow<List<Preset>>
    
    /**
     * 获取所有预设(列表)
     */
    suspend fun getAllPresetsList(): List<Preset>
    
    /**
     * 根据ID获取预设
     */
    suspend fun getPresetById(id: kotlin.uuid.Uuid): Preset?
    
    /**
     * 根据类型获取预设
     */
    fun getPresetsByType(type: PresetType): Flow<List<Preset>>
    
    /**
     * 根据类型获取预设(列表)
     */
    suspend fun getPresetsByTypeList(type: PresetType): List<Preset>
    
    /**
     * 保存预设(新增或更新)
     */
    suspend fun savePreset(preset: Preset): Result<Preset>
    
    /**
     * 删除预设
     */
    suspend fun deletePreset(id: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 搜索预设
     */
    suspend fun searchPresets(query: String): List<Preset>
    
    /**
     * 获取预设数量
     */
    suspend fun getPresetCount(): Int
    
    /**
     * 获取指定类型的预设数量
     */
    suspend fun getPresetCountByType(type: PresetType): Int
    
    /**
     * 批量导入预设
     */
    suspend fun importPresets(presets: List<Preset>): Result<Int>
    
    /**
     * 导出所有预设为JSON
     */
    suspend fun exportAllPresets(): Result<String>
}
