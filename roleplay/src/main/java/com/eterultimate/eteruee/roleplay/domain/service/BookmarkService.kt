package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

/**
 * 书签服务接口
 */
interface BookmarkService {
    /**
     * 获取角色的所有书签
     */
    fun getBookmarksByCharacter(characterId: kotlin.uuid.Uuid): Flow<List<Bookmark>>
    
    /**
     * 获取聊天的所有书签
     */
    fun getBookmarksByChat(chatId: kotlin.uuid.Uuid): Flow<List<Bookmark>>
    
    /**
     * 根据ID获取书签
     */
    suspend fun getBookmarkById(bookmarkId: kotlin.uuid.Uuid): Bookmark?
    
    /**
     * 创建书签
     */
    suspend fun createBookmark(
        chatId: kotlin.uuid.Uuid,
        characterId: kotlin.uuid.Uuid,
        messageId: kotlin.uuid.Uuid? = null,
        nodeId: kotlin.uuid.Uuid? = null,
        title: String = "",
        note: String = "",
        color: String = "#FFD700",
        tags: List<String> = emptyList()
    ): Result<Bookmark>
    
    /**
     * 更新书签
     */
    suspend fun updateBookmark(bookmark: Bookmark): Result<Bookmark>
    
    /**
     * 删除书签
     */
    suspend fun deleteBookmark(bookmarkId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 删除角色的所有书签
     */
    suspend fun deleteBookmarksByCharacter(characterId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 删除聊天的所有书签
     */
    suspend fun deleteBookmarksByChat(chatId: kotlin.uuid.Uuid): Result<Unit>
    
    /**
     * 搜索书签
     */
    suspend fun searchBookmarks(query: String): List<Bookmark>
    
    /**
     * 按标签查询书签
     */
    suspend fun getBookmarksByTag(tag: String): List<Bookmark>
}
