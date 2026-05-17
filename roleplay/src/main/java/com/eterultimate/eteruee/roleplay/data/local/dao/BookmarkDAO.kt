package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签数据访问对象
 */
@Dao
interface BookmarkDAO {
    /**
     * 获取角色的所有书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE characterId = :characterId ORDER BY createdAt DESC")
    fun getBookmarksByCharacter(characterId: String): Flow<List<BookmarkEntity>>
    
    /**
     * 获取聊天的所有书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getBookmarksByChat(chatId: String): Flow<List<BookmarkEntity>>
    
    /**
     * 根据ID获取书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: String): BookmarkEntity?
    
    /**
     * 插入或更新书签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(entity: BookmarkEntity)
    
    /**
     * 批量插入书签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(entities: List<BookmarkEntity>)
    
    /**
     * 删除书签
     */
    @Delete
    suspend fun deleteBookmark(entity: BookmarkEntity)
    
    /**
     * 根据ID删除书签
     */
    @Query("DELETE FROM rp_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)
    
    /**
     * 删除角色的所有书签
     */
    @Query("DELETE FROM rp_bookmarks WHERE characterId = :characterId")
    suspend fun deleteBookmarksByCharacter(characterId: String)
    
    /**
     * 删除聊天的所有书签
     */
    @Query("DELETE FROM rp_bookmarks WHERE chatId = :chatId")
    suspend fun deleteBookmarksByChat(chatId: String)
    
    /**
     * 搜索书签(按标题或备注)
     */
    @Query("SELECT * FROM rp_bookmarks WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchBookmarks(query: String): List<BookmarkEntity>
    
    /**
     * 按标签查询书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE tagsJson LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    suspend fun getBookmarksByTag(tag: String): List<BookmarkEntity>
}
