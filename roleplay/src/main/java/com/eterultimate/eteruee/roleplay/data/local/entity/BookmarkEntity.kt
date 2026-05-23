package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书签数据库实体
 */
@Entity(tableName = "rp_bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val messageIndex: Int,
    val title: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Bookmark): BookmarkEntity {
            return BookmarkEntity(
                id = model.id.toString(),
                chatId = model.chatId.toString(),
                messageIndex = model.messageIndex,
                title = model.title,
                note = model.note,
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli()
            )
        }
        
        fun toModel(entity: BookmarkEntity): com.eterultimate.eteruee.roleplay.data.model.Bookmark {
            return com.eterultimate.eteruee.roleplay.data.model.Bookmark(
                id = kotlin.uuid.Uuid.parse(entity.id),
                chatId = kotlin.uuid.Uuid.parse(entity.chatId),
                messageIndex = entity.messageIndex,
                title = entity.title,
                note = entity.note,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }
}
