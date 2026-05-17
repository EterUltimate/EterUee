package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 角色卡数据访问对象
 */
@Dao
interface CharacterDAO {
    /**
     * 获取所有角色(分页)
     */
    @Query("SELECT * FROM rp_characters ORDER BY updatedAt DESC")
    fun getAllCharacters(): PagingSource<Int, CharacterEntity>
    
    /**
     * 获取所有角色(Flow)
     */
    @Query("SELECT * FROM rp_characters ORDER BY updatedAt DESC")
    fun getAllCharactersFlow(): Flow<List<CharacterEntity>>
    
    /**
     * 根据ID获取角色
     */
    @Query("SELECT * FROM rp_characters WHERE id = :id")
    suspend fun getCharacterById(id: String): CharacterEntity?
    
    /**
     * 插入或更新角色
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(entity: CharacterEntity)
    
    /**
     * 批量插入角色
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(entities: List<CharacterEntity>)
    
    /**
     * 删除角色
     */
    @Delete
    suspend fun deleteCharacter(entity: CharacterEntity)
    
    /**
     * 根据ID删除角色
     */
    @Query("DELETE FROM rp_characters WHERE id = :id")
    suspend fun deleteCharacterById(id: String)
    
    /**
     * 搜索角色(按名称)
     */
    @Query("SELECT * FROM rp_characters WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchCharacters(query: String): List<CharacterEntity>
    
    /**
     * 获取收藏的角色
     */
    @Query("SELECT * FROM rp_characters WHERE favorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteCharacters(): Flow<List<CharacterEntity>>
    
    /**
     * 更新角色的聊天计数
     */
    @Query("UPDATE rp_characters SET chatCount = :count, lastChatAt = :lastChatAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateChatCount(id: String, count: Int, lastChatAt: Long?, updatedAt: Long)
}
