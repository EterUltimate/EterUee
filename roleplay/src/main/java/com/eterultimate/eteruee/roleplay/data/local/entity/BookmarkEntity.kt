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
    val characterId: String,
    val messageId: String?,  // 可选，标记特定消息
    val nodeId: String?,     // 可选，标记特定分支节点
    val title: String,
    val note: String,        // 用户备注
    val createdAt: Long,
    val updatedAt: Long,
    val color: String,       // 书签颜色（十六进制）
    val tagsJson: String     // 标签列表(JSON数组)
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Bookmark): BookmarkEntity {
            return BookmarkEntity(
                id = model.id.toString(),
                chatId = model.chatId.toString(),
                characterId = model.characterId.toString(),
                messageId = model.messageId?.toString(),
                nodeId = model.nodeId?.toString(),
                title = model.title,
                note = model.note,
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli(),
                color = model.color,
                tagsJson = kotlinx.serialization.json.Json.encodeToString(model.tags)
            )
        }
        
        fun toModel(entity: BookmarkEntity): com.eterultimate.eteruee.roleplay.data.model.Bookmark {
            val tags = try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.tagsJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            return com.eterultimate.eteruee.roleplay.data.model.Bookmark(
                id = kotlin.uuid.Uuid.parse(entity.id),
                chatId = kotlin.uuid.Uuid.parse(entity.chatId),
                characterId = kotlin.uuid.Uuid.parse(entity.characterId),
                messageId = entity.messageId?.let { kotlin.uuid.Uuid.parse(it) },
                nodeId = entity.nodeId?.let { kotlin.uuid.Uuid.parse(it) },
                title = entity.title,
                note = entity.note,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt),
                color = entity.color,
                tags = tags
            )
        }
    }
}
