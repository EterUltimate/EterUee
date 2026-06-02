package com.eterultimate.eteruee.roleplay.domain.service

import android.net.Uri
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import kotlinx.coroutines.flow.Flow

/**
 * 世界书服务接口
 */
interface WorldInfoService {
    /**
     * 获取所有世界书
     */
    fun getAllWorldInfos(): Flow<List<WorldInfo>>
    
    /**
     * 根据ID获取世界书
     */
    suspend fun getWorldInfoById(worldInfoId: kotlin.uuid.Uuid): WorldInfo?
    
    /**
     * 创建世界书
     */
    suspend fun createWorldInfo(name: String, description: String = ""): Result<WorldInfo>

    /**
     * 保存完整世界书(创建或更新)
     */
    suspend fun saveWorldInfo(worldInfo: WorldInfo): Result<WorldInfo>

    /**
     * 导入 Tavern/SillyTavern 世界书 JSON
     */
    suspend fun importWorldInfo(uri: Uri): Result<WorldInfo>

    suspend fun importWorldInfo(jsonString: String, fallbackName: String = "Imported Lorebook"): Result<WorldInfo>
    
    /**
     * 更新世界书
     */
    suspend fun updateWorldInfo(worldInfo: WorldInfo): Result<WorldInfo>
    
    /**
     * 删除世界书
     */
    suspend fun deleteWorldInfo(worldInfoId: kotlin.uuid.Uuid): Result<Unit>
    
    // ==================== 条目管理 ====================
    
    /**
     * 添加条目
     */
    suspend fun addEntry(worldInfoId: kotlin.uuid.Uuid, entry: WorldInfoEntry): Result<WorldInfo>
    
    /**
     * 更新条目
     */
    suspend fun updateEntry(worldInfoId: kotlin.uuid.Uuid, entry: WorldInfoEntry): Result<WorldInfo>
    
    /**
     * 删除条目
     */
    suspend fun deleteEntry(worldInfoId: kotlin.uuid.Uuid, entryId: kotlin.uuid.Uuid): Result<WorldInfo>
    
    /**
     * 切换条目启用状态
     */
    suspend fun toggleEntryEnabled(worldInfoId: kotlin.uuid.Uuid, entryId: kotlin.uuid.Uuid): Result<WorldInfo>
    
    // ==================== 关键词匹配 ====================
    
    /**
     * 扫描消息并返回匹配的条目
     */
    suspend fun scanAndMatchEntries(
        worldInfoId: kotlin.uuid.Uuid,
        recentMessages: List<String>,
        scanDepth: Int = 4
    ): List<WorldInfoEntry>
    
    /**
     * 检查单个条目是否匹配
     */
    fun isEntryMatched(entry: WorldInfoEntry, text: String): Boolean
    
    /**
     * 批量检查条目匹配
     */
    fun matchEntries(entries: List<WorldInfoEntry>, text: String): List<WorldInfoEntry>
}
