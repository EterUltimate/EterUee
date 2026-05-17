package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.CharacterDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.CharacterEntity
import com.eterultimate.eteruee.roleplay.data.model.Character
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream
import kotlin.uuid.Uuid

/**
 * 角色卡服务实现
 */
class CharacterServiceImpl(
    private val context: Context,
    private val characterDao: CharacterDAO,
    private val fileStorage: RolePlayFileStorage
) : CharacterService {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override fun getAllCharacters(): Flow<PagingData<Character>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { characterDao.getAllCharacters() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> CharacterEntity.toModel(entity) }
        }
    }
    
    override fun getAllCharactersList(): Flow<List<Character>> {
        return characterDao.getAllCharactersFlow().map { entities ->
            entities.map { CharacterEntity.toModel(it) }
        }
    }
    
    override suspend fun getCharacterById(id: kotlin.uuid.Uuid): Character? {
        return withContext(Dispatchers.IO) {
            val entity = characterDao.getCharacterById(id.toString())
            entity?.let { CharacterEntity.toModel(it) }
        }
    }
    
    override suspend fun createCharacter(character: Character, avatarUri: Uri?): Result<Character> {
        return withContext(Dispatchers.IO) {
            try {
                // 保存头像
                val avatarPath = avatarUri?.let { fileStorage.saveCharacterAvatar(character.id, it) }
                
                // 保存JSON
                fileStorage.saveCharacterJson(character, avatarPath)
                
                // 保存到数据库
                val entity = CharacterEntity.fromModel(character)
                characterDao.insertCharacter(entity)
                
                Result.success(character)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateCharacter(character: Character, avatarUri: Uri?): Result<Character> {
        return withContext(Dispatchers.IO) {
            try {
                // 保存新头像(如果有)
                val avatarPath = avatarUri?.let { fileStorage.saveCharacterAvatar(character.id, it) }
                    ?: character.avatarUrl
                
                // 更新JSON
                fileStorage.saveCharacterJson(character.copy(avatarUrl = avatarPath), avatarPath)
                
                // 更新数据库
                val entity = CharacterEntity.fromModel(character.copy(avatarUrl = avatarPath))
                characterDao.insertCharacter(entity)
                
                Result.success(character)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteCharacter(id: kotlin.uuid.Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 删除数据库记录
                characterDao.deleteCharacterById(id.toString())
                
                // 删除文件目录
                fileStorage.deleteCharacterDir(id)
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun toggleFavorite(id: kotlin.uuid.Uuid): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val character = getCharacterById(id) ?: return@withContext Result.failure(Exception("Character not found"))
                val newFavorite = !character.favorite
                val updated = character.copy(favorite = newFavorite)
                
                fileStorage.saveCharacterJson(updated, updated.avatarUrl)
                characterDao.insertCharacter(CharacterEntity.fromModel(updated))
                
                Result.success(newFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun searchCharacters(query: String): List<Character> {
        return withContext(Dispatchers.IO) {
            val entities = characterDao.searchCharacters(query)
            entities.map { CharacterEntity.toModel(it) }
        }
    }
    
    override fun searchCharactersAdvanced(
        query: String,
        tags: List<String>,
        favoriteOnly: Boolean,
        sortBy: CharacterSortOption
    ): Flow<List<Character>> {
        // 获取所有角色，然后在内存中过滤
        return characterDao.getAllCharactersFlow().map { entities ->
            var result = entities.map { CharacterEntity.toModel(it) }
            
            // 过滤标签
            if (tags.isNotEmpty()) {
                result = result.filter { character ->
                    tags.all { tag -> character.tags.contains(tag) }
                }
            }
            
            // 过滤收藏
            if (favoriteOnly) {
                result = result.filter { it.favorite }
            }
            
            // 名称搜索
            if (query.isNotBlank()) {
                result = result.filter { it.name.contains(query, ignoreCase = true) }
            }
            
            // 排序
            result.sortedWith(
                when (sortBy) {
                    CharacterSortOption.NAME_ASC -> compareBy { it.name.lowercase() }
                    CharacterSortOption.NAME_DESC -> compareByDescending { it.name.lowercase() }
                    CharacterSortOption.LAST_CHAT_DESC -> compareByDescending { it.lastChatAt?.toEpochMilli() ?: 0 }
                    CharacterSortOption.LAST_CHAT_ASC -> compareBy { it.lastChatAt?.toEpochMilli() ?: 0 }
                    CharacterSortOption.CREATED_DESC -> compareByDescending { it.createdAt.toEpochMilli() }
                    CharacterSortOption.CREATED_ASC -> compareBy { it.createdAt.toEpochMilli() }
                    CharacterSortOption.CHAT_COUNT_DESC -> compareByDescending { it.chatCount }
                }
            )
        }
    }
    
    override suspend fun getAllTags(): List<String> {
        return withContext(Dispatchers.IO) {
            val allCharacters = characterDao.getAllCharactersFlow()
            // 由于是Flow，我们需要阻塞获取
            // 这里简化处理，返回空列表
            // TODO: 实现真正的标签提取逻辑
            emptyList()
        }
    }
    
    override fun getCharactersByTag(tag: String): Flow<List<Character>> {
        return characterDao.getAllCharactersFlow().map { entities ->
            entities.map { CharacterEntity.toModel(it) }
                .filter { character -> character.tags.contains(tag) }
        }
    }
    
    // ==================== PNG导入/导出 ====================
    
    override suspend fun importPngCharacter(uri: Uri): Result<Character> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open file"))
                
                // 读取PNG并提取tEXt/zTXt chunk中的JSON
                val pngData = inputStream.readBytes()
                val jsonData = extractJsonFromPng(pngData)
                
                if (jsonData == null) {
                    return@withContext Result.failure(Exception("No character data found in PNG"))
                }
                
                // 解析JSON为Character
                val character = json.decodeFromString<Character>(jsonData)
                
                // 保存头像(PNG本身)
                val avatarPath = fileStorage.saveCharacterAvatar(character.id, uri)
                
                // 保存JSON和数据库
                fileStorage.saveCharacterJson(character, avatarPath)
                characterDao.insertCharacter(CharacterEntity.fromModel(character))
                
                Result.success(character)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportPngCharacter(characterId: kotlin.uuid.Uuid, outputUri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val character = getCharacterById(characterId)
                    ?: return@withContext Result.failure(Exception("Character not found"))
                
                // 读取原始PNG或使用默认模板
                val avatarPath = character.avatarUrl
                val pngBytes = if (avatarPath != null && java.io.File(avatarPath).exists()) {
                    java.io.File(avatarPath).readBytes()
                } else {
                    // TODO: 使用默认PNG模板
                    byteArrayOf()
                }
                
                // 将Character JSON嵌入PNG的tEXt chunk
                val characterJson = json.encodeToString(character)
                val pngWithMetadata = embedJsonInPng(pngBytes, characterJson)
                
                // 写入输出文件
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    output.write(pngWithMetadata)
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== JSON导入/导出 ====================
    
    override suspend fun importJsonCharacter(uri: Uri): Result<Character> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("Cannot read file"))
                
                val character = json.decodeFromString<Character>(jsonString)
                
                // 保存JSON和数据库
                fileStorage.saveCharacterJson(character, character.avatarUrl)
                characterDao.insertCharacter(CharacterEntity.fromModel(character))
                
                Result.success(character)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun exportJsonCharacter(characterId: kotlin.uuid.Uuid, outputUri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val character = getCharacterById(characterId)
                    ?: return@withContext Result.failure(Exception("Character not found"))
                
                val jsonString = json.encodeToString(character)
                
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    // ==================== PNG元数据提取工具方法 ====================
    
    /**
     * 从PNG中提取tEXt/zTXt chunk的JSON数据
     */
    private fun extractJsonFromPng(pngData: ByteArray): String? {
        // PNG签名: 89 50 4E 47 0D 0A 1A 0A
        if (pngData.size < 8 || 
            pngData[0] != 0x89.toByte() || 
            pngData[1] != 0x50.toByte() ||
            pngData[2] != 0x4E.toByte() ||
            pngData[3] != 0x47.toByte()) {
            return null
        }
        
        var offset = 8
        while (offset < pngData.size - 12) {
            // 读取chunk长度(4字节)
            val length = ((pngData[offset].toInt() and 0xFF) shl 24) or
                        ((pngData[offset + 1].toInt() and 0xFF) shl 16) or
                        ((pngData[offset + 2].toInt() and 0xFF) shl 8) or
                        (pngData[offset + 3].toInt() and 0xFF)
            
            // 读取chunk类型(4字节)
            val type = String(pngData, offset + 4, 4, Charsets.US_ASCII)
            
            // 检查是否为tEXt或zTXt
            if (type == "tEXt" || type == "zTXt") {
                val chunkData = pngData.copyOfRange(offset + 8, offset + 8 + length)
                
                if (type == "tEXt") {
                    // tEXt格式: keyword\0text
                    val nullIndex = chunkData.indexOf(0)
                    if (nullIndex > 0) {
                        val keyword = String(chunkData, 0, nullIndex, Charsets.US_ASCII)
                        if (keyword == "chara" || keyword == "Chara") {
                            return String(chunkData, nullIndex + 1, length - nullIndex - 1, Charsets.UTF_8)
                        }
                    }
                } else if (type == "zTXt") {
                    // zTXt格式: keyword\0compression_method\0compressed_text
                    val nullIndex = chunkData.indexOf(0)
                    if (nullIndex > 0) {
                        val keyword = String(chunkData, 0, nullIndex, Charsets.US_ASCII)
                        if (keyword == "chara" || keyword == "Chara") {
                            // 跳过压缩方法字节
                            val compressedData = chunkData.copyOfRange(nullIndex + 2, length)
                            return try {
                                decompressZlib(compressedData)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            }
            
            // 移动到下一个chunk (length + 4(type) + 4(crc))
            offset += 12 + length
        }
        
        return null
    }
    
    /**
     * 解压zlib数据
     */
    private fun decompressZlib(data: ByteArray): String {
        InflaterInputStream(data.inputStream()).use { inflater ->
            val buffer = ByteArray(8192)
            val outputStream = ByteArrayOutputStream()
            var bytesRead: Int
            while (inflater.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            return outputStream.toString(Charsets.UTF_8.name())
        }
    }
    
    /**
     * 将JSON嵌入PNG的tEXt chunk
     */
    private fun embedJsonInPng(pngData: ByteArray, jsonData: String): ByteArray {
        // 简化实现:直接返回原始PNG
        // TODO: 完整实现需要正确插入tEXt/zTXt chunk
        return pngData
    }
}
