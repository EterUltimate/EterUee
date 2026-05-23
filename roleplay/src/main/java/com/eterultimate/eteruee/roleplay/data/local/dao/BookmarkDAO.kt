package com.eterultimate.eteruee.roleplay.data.local.dao

import androidx.room.*
import com.eterultimate.eteruee.roleplay.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签数据访问对象
 */
@Dao
interface BookmarkDao {
    
    /**
     * 插入书签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)
    
    /**
     * 批量插入书签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)
    
    /**
     * 删除书签
     */
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
    
    /**
     * 根据ID删除书签
     */
    @Query("DELETE FROM rp_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)
    
    /**
     * 获取特定聊天的所有书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getBookmarksByChat(chatId: String): Flow<List<BookmarkEntity>>
    
    /**
     * 根据ID获取书签
     */
    @Query("SELECT * FROM rp_bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: String): BookmarkEntity?
    
    /**
     * 获取所有书签
     */
    @Query("SELECT * FROM rp_bookmarks ORDER BY updatedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    
    /**
     * 更新书签标题和备注
     */
    @Query("UPDATE rp_bookmarks SET title = :title, note = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: String, title: String, note: String, updatedAt: Long)
}
