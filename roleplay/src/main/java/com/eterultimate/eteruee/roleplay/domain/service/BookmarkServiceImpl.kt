package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.local.dao.BookmarkDAO
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
    private val bookmarkDao: BookmarkDAO
) : BookmarkService {
    
    override fun getBookmarksByCharacter(characterId: Uuid): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByCharacter(characterId.toString()).map { entities ->
            entities.map { BookmarkEntity.toModel(it) }
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
    
    override suspend fun createBookmark(
        chatId: Uuid,
        characterId: Uuid,
        messageId: Uuid?,
        nodeId: Uuid?,
        title: String,
        note: String,
        color: String,
        tags: List<String>
    ): Result<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val bookmark = Bookmark(
                    chatId = chatId,
                    characterId = characterId,
                    messageId = messageId,
                    nodeId = nodeId,
                    title = title,
                    note = note,
                    createdAt = now,
                    updatedAt = now,
                    color = color,
                    tags = tags
                )
                
                // 保存到数据库
                val entity = BookmarkEntity.fromModel(bookmark)
                bookmarkDao.insertBookmark(entity)
                
                Result.success(bookmark)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateBookmark(bookmark: Bookmark): Result<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedBookmark = bookmark.copy(updatedAt = Instant.now())
                
                // 更新数据库
                val entity = BookmarkEntity.fromModel(updatedBookmark)
                bookmarkDao.insertBookmark(entity)
                
                Result.success(updatedBookmark)
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
    
    override suspend fun deleteBookmarksByCharacter(characterId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarksByCharacter(characterId.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteBookmarksByChat(chatId: Uuid): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarksByChat(chatId.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    override suspend fun searchBookmarks(query: String): List<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = bookmarkDao.searchBookmarks(query)
                entities.map { BookmarkEntity.toModel(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    override suspend fun getBookmarksByTag(tag: String): List<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = bookmarkDao.getBookmarksByTag(tag)
                entities.map { BookmarkEntity.toModel(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
