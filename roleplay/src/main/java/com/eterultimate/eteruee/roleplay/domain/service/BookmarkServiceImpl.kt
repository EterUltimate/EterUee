package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.local.dao.BookmarkDao
import com.eterultimate.eteruee.roleplay.data.local.entity.BookmarkEntity
import com.eterultimate.eteruee.roleplay.data.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 书签服务实现
 */
class BookmarkServiceImpl(
    private val bookmarkDao: BookmarkDao
) : BookmarkService {
    
    override suspend fun addBookmark(
        chatId: Uuid,
        messageIndex: Int,
        title: String,
        note: String
    ): Result<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val bookmark = Bookmark(
                    chatId = chatId,
                    messageIndex = messageIndex,
                    title = title,
                    note = note
                )
                
                val entity = BookmarkEntity.fromModel(bookmark)
                bookmarkDao.insertBookmark(entity)
                
                Result.success(bookmark)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteBookmark(bookmarkId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarkById(bookmarkId.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override fun getBookmarksByChat(chatId: Uuid): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByChat(chatId.toString()).map { entities ->
            entities.map { BookmarkEntity.toModel(it) }
        }
    }
    
    override suspend fun getBookmarkById(bookmarkId: Uuid): Bookmark? {
        return withContext(Dispatchers.IO) {
            val entity = bookmarkDao.getBookmarkById(bookmarkId.toString())
            entity?.let { BookmarkEntity.toModel(it) }
        }
    }
    
    override suspend fun updateBookmark(
        bookmarkId: Uuid,
        title: String,
        note: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedAt = Instant.now().toEpochMilli()
                bookmarkDao.updateBookmark(
                    id = bookmarkId.toString(),
                    title = title,
                    note = note,
                    updatedAt = updatedAt
                )
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
