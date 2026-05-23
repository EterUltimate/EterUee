package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/**
 * 书签服务接口
 */
interface BookmarkService {
    
    /**
     * 添加书签
     */
    suspend fun addBookmark(
        chatId: Uuid,
        messageIndex: Int,
        title: String = "",
        note: String = ""
    ): Result<Bookmark>
    
    /**
     * 删除书签
     */
    suspend fun deleteBookmark(bookmarkId: Uuid): Result<Unit>
    
    /**
     * 获取特定聊天的所有书签
     */
    fun getBookmarksByChat(chatId: Uuid): Flow<List<Bookmark>>
    
    /**
     * 根据ID获取书签
     */
    suspend fun getBookmarkById(bookmarkId: Uuid): Bookmark?
    
    /**
     * 更新书签
     */
    suspend fun updateBookmark(
        bookmarkId: Uuid,
        title: String,
        note: String
    ): Result<Unit>
}
