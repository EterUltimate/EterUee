package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import android.net.Uri
import com.eterultimate.eteruee.roleplay.data.local.dao.PresetDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.PresetEntity
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.data.tavern.TavernPresetCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 预设服务实现
 */
class PresetServiceImpl(
    private val context: Context,
    private val presetDao: PresetDAO
) : PresetService {
    
    override fun getAllPresets(): Flow<List<Preset>> {
        return presetDao.getAllPresets().map { entities ->
            entities.map { PresetEntity.toModel(it) }
        }
    }
    
    override suspend fun getAllPresetsList(): List<Preset> {
        return withContext(Dispatchers.IO) {
            presetDao.getAllPresetsList().map { PresetEntity.toModel(it) }
        }
    }
    
    override suspend fun getPresetById(id: kotlin.uuid.Uuid): Preset? {
        return withContext(Dispatchers.IO) {
            presetDao.getPresetById(id.toString())?.let { PresetEntity.toModel(it) }
        }
    }
    
    override fun getPresetsByType(type: PresetType): Flow<List<Preset>> {
        return presetDao.getPresetsByType(type.name).map { entities ->
            entities.map { PresetEntity.toModel(it) }
        }
    }
    
    override suspend fun getPresetsByTypeList(type: PresetType): List<Preset> {
        return withContext(Dispatchers.IO) {
            presetDao.getPresetsByTypeList(type.name).map { PresetEntity.toModel(it) }
        }
    }
    
    override suspend fun savePreset(preset: Preset): Result<Preset> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = PresetEntity.fromModel(preset)
                presetDao.insertPreset(entity)
                Result.success(preset)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deletePreset(id: kotlin.uuid.Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                presetDao.deletePresetById(id.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun searchPresets(query: String): List<Preset> {
        return withContext(Dispatchers.IO) {
            presetDao.searchPresets(query).map { PresetEntity.toModel(it) }
        }
    }
    
    override suspend fun getPresetCount(): Int {
        return withContext(Dispatchers.IO) {
            presetDao.getPresetCount()
        }
    }
    
    override suspend fun getPresetCountByType(type: PresetType): Int {
        return withContext(Dispatchers.IO) {
            presetDao.getPresetCountByType(type.name)
        }
    }
    
    override suspend fun importPresets(presets: List<Preset>): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = presets.map { PresetEntity.fromModel(it) }
                presetDao.insertPresets(entities)
                Result.success(presets.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun importPreset(uri: Uri): Result<Preset> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("Cannot read file"))
                val fallbackName = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                    ?: "Imported Preset"
                importPresetData(jsonString, fallbackName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun importPreset(jsonString: String, fallbackName: String): Result<Preset> {
        return withContext(Dispatchers.IO) {
            try {
                importPresetData(jsonString, fallbackName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportAllPresets(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val presets = presetDao.getAllPresetsList().map { PresetEntity.toModel(it) }
                val json = kotlinx.serialization.json.Json.encodeToString(presets)
                Result.success(json)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun importPresetData(jsonString: String, fallbackName: String): Result<Preset> {
        val preset = TavernPresetCodec.decode(jsonString, fallbackName)
        presetDao.insertPreset(PresetEntity.fromModel(preset))
        return Result.success(preset)
    }
}
