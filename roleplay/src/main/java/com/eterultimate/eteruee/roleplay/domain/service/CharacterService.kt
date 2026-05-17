package com.eterultimate.eteruee.roleplay.domain.service

import android.net.Uri
import androidx.paging.PagingData
import com.eterultimate.eteruee.roleplay.data.model.Character
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
     * 导入PNG角色卡
     */
    suspend fun importPngCharacter(uri: Uri): Result<Character>
    
    /**
     * 导出PNG角色卡
     */
    suspend fun exportPngCharacter(characterId: kotlin.uuid.Uuid, outputUri: Uri): Result<Unit>
    
    /**
     * 导入JSON角色卡
     */
    suspend fun importJsonCharacter(uri: Uri): Result<Character>
    
    /**
     * 导出JSON角色卡
     */
    suspend fun exportJsonCharacter(characterId: kotlin.uuid.Uuid, outputUri: Uri): Result<Unit>
}
