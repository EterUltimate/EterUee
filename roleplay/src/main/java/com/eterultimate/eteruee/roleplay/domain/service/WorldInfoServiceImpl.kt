package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.WorldInfoDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.WorldInfoEntity
import com.eterultimate.eteruee.roleplay.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 世界书服务实现
 */
class WorldInfoServiceImpl(
    private val context: Context,
    private val worldInfoDao: WorldInfoDAO,
    private val fileStorage: RolePlayFileStorage
) : WorldInfoService {
    
    override fun getAllWorldInfos(): Flow<List<WorldInfo>> {
        return worldInfoDao.getAllWorldInfos().map { entities ->
            entities.map { WorldInfoEntity.toModel(it) }
        }
    }
    
    override suspend fun getWorldInfoById(worldInfoId: kotlin.uuid.Uuid): WorldInfo? {
        return withContext(Dispatchers.IO) {
            val entity = worldInfoDao.getWorldInfoById(worldInfoId.toString())
            entity?.let { WorldInfoEntity.toModel(it) }
        }
    }
    
    override suspend fun createWorldInfo(name: String, description: String): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val worldInfo = WorldInfo(
                    name = name,
                    description = description,
                    entries = emptyList(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                
                // 保存到数据库
                val entity = WorldInfoEntity.fromModel(worldInfo)
                worldInfoDao.insertWorldInfo(entity)
                
                // 保存JSON文件
                fileStorage.saveWorldInfoJson(worldInfo)
                
                Result.success(worldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateWorldInfo(worldInfo: WorldInfo): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedWorldInfo = worldInfo.copy(updatedAt = Instant.now())
                
                // 更新数据库
                val entity = WorldInfoEntity.fromModel(updatedWorldInfo)
                worldInfoDao.insertWorldInfo(entity)
                
                // 更新JSON文件
                fileStorage.saveWorldInfoJson(updatedWorldInfo)
                
                Result.success(updatedWorldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteWorldInfo(worldInfoId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 删除数据库记录
                worldInfoDao.deleteWorldInfoById(worldInfoId.toString())
                
                // 删除JSON文件
                fileStorage.deleteWorldInfoFile(worldInfoId)
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 条目管理 ====================
    
    override suspend fun addEntry(worldInfoId: kotlin.uuid.Uuid, entry: WorldInfoEntry): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val worldInfo = getWorldInfoById(worldInfoId) 
                    ?: return@withContext Result.failure(Exception("WorldInfo not found"))
                
                val updatedEntries = worldInfo.entries + entry
                val updatedWorldInfo = worldInfo.copy(entries = updatedEntries)
                
                updateWorldInfo(updatedWorldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateEntry(worldInfoId: kotlin.uuid.Uuid, entry: WorldInfoEntry): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val worldInfo = getWorldInfoById(worldInfoId) 
                    ?: return@withContext Result.failure(Exception("WorldInfo not found"))
                
                val updatedEntries = worldInfo.entries.map { existing ->
                    if (existing.id == entry.id) entry else existing
                }
                
                val updatedWorldInfo = worldInfo.copy(entries = updatedEntries)
                updateWorldInfo(updatedWorldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteEntry(worldInfoId: kotlin.uuid.Uuid, entryId: kotlin.uuid.Uuid): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val worldInfo = getWorldInfoById(worldInfoId) 
                    ?: return@withContext Result.failure(Exception("WorldInfo not found"))
                
                val updatedEntries = worldInfo.entries.filter { it.id != entryId }
                val updatedWorldInfo = worldInfo.copy(entries = updatedEntries)
                
                updateWorldInfo(updatedWorldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun toggleEntryEnabled(worldInfoId: kotlin.uuid.Uuid, entryId: kotlin.uuid.Uuid): Result<WorldInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val worldInfo = getWorldInfoById(worldInfoId) 
                    ?: return@withContext Result.failure(Exception("WorldInfo not found"))
                
                val updatedEntries = worldInfo.entries.map { entry ->
                    if (entry.id == entryId) {
                        entry.copy(enabled = !entry.enabled)
                    } else {
                        entry
                    }
                }
                
                val updatedWorldInfo = worldInfo.copy(entries = updatedEntries)
                updateWorldInfo(updatedWorldInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== 关键词匹配 ====================
    
    override suspend fun scanAndMatchEntries(
        worldInfoId: kotlin.uuid.Uuid,
        recentMessages: List<String>,
        scanDepth: Int
    ): List<WorldInfoEntry> {
        return withContext(Dispatchers.IO) {
            val worldInfo = getWorldInfoById(worldInfoId) ?: return@withContext emptyList()
            
            // 只扫描最近的N条消息
            val messagesToScan = recentMessages.takeLast(scanDepth)
            val combinedText = messagesToScan.joinToString(" ")
            
            // 过滤启用的条目
            val enabledEntries = worldInfo.entries.filter { it.enabled }
            
            // 分离常量条目和条件条目
            val constantEntries = enabledEntries.filter { it.constant }
            val conditionalEntries = enabledEntries.filter { !it.constant }
            
            // 匹配条件条目
            val matchedConditional = matchEntries(conditionalEntries, combinedText)
            
            // 合并结果并按order排序
            (constantEntries + matchedConditional).sortedBy { it.order }
        }
    }
    
    override fun isEntryMatched(entry: WorldInfoEntry, text: String): Boolean {
        if (!entry.enabled) return false
        
        val keys = entry.getAllKeys()
        if (keys.isEmpty()) return false
        
        val lowerText = text.lowercase()
        
        return when (entry.probability) {
            0f -> false
            1f -> checkKeysMatch(keys, lowerText, entry)
            else -> {
                // 概率触发
                if (Math.random() > entry.probability) {
                    false
                } else {
                    checkKeysMatch(keys, lowerText, entry)
                }
            }
        }
    }
    
    override fun matchEntries(entries: List<WorldInfoEntry>, text: String): List<WorldInfoEntry> {
        return entries.filter { entry ->
            isEntryMatched(entry, text)
        }
    }
    
    /**
     * 检查关键词是否匹配(支持AND/OR逻辑)
     */
    private fun checkKeysMatch(keys: List<String>, text: String, entry: WorldInfoEntry): Boolean {
        if (keys.isEmpty()) return false
        
        return when (entry.keys.size) {
            0 -> {
                // 只有主关键词
                text.contains(entry.key.lowercase())
            }
            else -> {
                // 有多个关键词,根据逻辑判断
                when (SelectiveLogic.OR) { // TODO: 从worldInfo获取selectiveLogic
                    SelectiveLogic.AND -> keys.all { key -> text.contains(key.lowercase()) }
                    SelectiveLogic.OR -> keys.any { key -> text.contains(key.lowercase()) }
                }
            }
        }
    }
}
