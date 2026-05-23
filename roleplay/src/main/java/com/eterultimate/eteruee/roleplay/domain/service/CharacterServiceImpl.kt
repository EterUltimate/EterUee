package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.data.local.dao.CharacterDAO
import com.eterultimate.eteruee.roleplay.data.local.entity.CharacterEntity
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.tavern.TavernCharacterCardFormat
import com.eterultimate.eteruee.roleplay.data.tavern.TavernCharacterCodec
import com.eterultimate.eteruee.roleplay.data.tavern.TavernPngCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

/**
 * 角色卡服务实现
 */
class CharacterServiceImpl(
    private val context: Context,
    private val characterDao: CharacterDAO,
    private val fileStorage: RolePlayFileStorage
) : CharacterService {

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
                
                // 读取PNG并提取 SillyTavern chara/ccv3 元数据
                val pngData = inputStream.readBytes()
                val jsonData = TavernPngCodec.readCharacterJson(pngData)
                
                if (jsonData == null) {
                    return@withContext Result.failure(Exception("No character data found in PNG"))
                }
                
                // 解析JSON为Character
                val character = TavernCharacterCodec.decode(jsonData)
                
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
    
    override suspend fun exportPngCharacter(
        characterId: kotlin.uuid.Uuid,
        outputUri: Uri,
        format: TavernCharacterCardFormat
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val character = getCharacterById(characterId)
                    ?: return@withContext Result.failure(Exception("Character not found"))
                
                // 读取原始PNG
                val avatarPath = character.avatarUrl
                val pngBytes = if (avatarPath != null && java.io.File(avatarPath).exists()) {
                    java.io.File(avatarPath).readBytes()
                } else {
                    return@withContext Result.failure(Exception("Character avatar PNG is required for PNG export"))
                }
                
                // V1/V2 use SillyTavern's chara key. V3 uses ccv3 and keeps V2 as a fallback for older tools.
                val characterJson = if (format == TavernCharacterCardFormat.V3) {
                    TavernCharacterCodec.encode(character, TavernCharacterCardFormat.V2)
                } else {
                    TavernCharacterCodec.encode(character, format)
                }
                val v3Json = if (format == TavernCharacterCardFormat.V3) {
                    TavernCharacterCodec.encode(character, TavernCharacterCardFormat.V3)
                } else {
                    null
                }
                val pngWithMetadata = TavernPngCodec.writeCharacterJson(pngBytes, characterJson, v3Json)
                
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
                
                val character = TavernCharacterCodec.decode(jsonString)
                
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
    
    override suspend fun exportJsonCharacter(
        characterId: kotlin.uuid.Uuid,
        outputUri: Uri,
        format: TavernCharacterCardFormat
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val character = getCharacterById(characterId)
                    ?: return@withContext Result.failure(Exception("Character not found"))
                
                val jsonString = TavernCharacterCodec.encode(character, format)
                
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
    
}
