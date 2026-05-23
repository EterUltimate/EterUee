package com.eterultimate.eteruee.roleplay.domain.service

import android.net.Uri
import androidx.paging.PagingData
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.tavern.TavernCharacterCardFormat
import kotlinx.coroutines.flow.Flow

/**
 * 角色卡服务接口
 */
interface CharacterService {
    /**
     * 获取所有角色(分页)
     */
    fun getAllCharacters(): Flow<PagingData<Character>>
    
    /**
     * 获取所有角色(完整列表)
     */
    fun getAllCharactersList(): Flow<List<Character>>
    
    /**
     * 根据ID获取角色
     */
    suspend fun getCharacterById(id: kotlin.uuid.Uuid): Character?
    
    /**
     * 创建新角色
     */
    suspend fun createCharacter(character: Character, avatarUri: Uri?): Result<Character>
    
    /**
     * 更新角色
     */
    suspend fun updateCharacter(character: Character, avatarUri: Uri?): Result<Character>
    
    /**
     * 删除角色
     */
    suspend fun deleteCharacter(id: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(id: kotlin.uuid.Uuid): Result<Boolean>
    
    /**
     * 搜索角色
     */
    suspend fun searchCharacters(query: String): List<Character>
    
    /**
     * 高级搜索 - 支持标签过滤、收藏过滤和排序
     */
    fun searchCharactersAdvanced(
        query: String = "",
        tags: List<String> = emptyList(),
        favoriteOnly: Boolean = false,
        sortBy: CharacterSortOption = CharacterSortOption.LAST_CHAT_DESC
    ): Flow<List<Character>>
    
    /**
     * 获取所有唯一标签
     */
    suspend fun getAllTags(): List<String>
    
    /**
     * 根据标签获取角色
     */
    fun getCharactersByTag(tag: String): Flow<List<Character>>
    
    /**
     * 导入PNG角色卡
     */
    suspend fun importPngCharacter(uri: Uri): Result<Character>
    
    /**
     * 导出PNG角色卡
     */
    suspend fun exportPngCharacter(
        characterId: kotlin.uuid.Uuid,
        outputUri: Uri,
        format: TavernCharacterCardFormat = TavernCharacterCardFormat.V2
    ): Result<Unit>
    
    /**
     * 导入JSON角色卡
     */
    suspend fun importJsonCharacter(uri: Uri): Result<Character>
    
    /**
     * 导出JSON角色卡
     */
    suspend fun exportJsonCharacter(
        characterId: kotlin.uuid.Uuid,
        outputUri: Uri,
        format: TavernCharacterCardFormat = TavernCharacterCardFormat.V2
    ): Result<Unit>
}

/**
 * 角色排序选项
 */
enum class CharacterSortOption {
    NAME_ASC,           // 名称升序
    NAME_DESC,          // 名称降序
    LAST_CHAT_DESC,     // 最后聊天时间降序（最近优先）
    LAST_CHAT_ASC,      // 最后聊天时间升序
    CREATED_DESC,       // 创建时间降序（最新优先）
    CREATED_ASC,        // 创建时间升序
    CHAT_COUNT_DESC     // 聊天数量降序（最多优先）
}
