package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 聊天数据库实体
 */
@Entity(tableName = "rp_chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val characterId: String,
    val groupId: String?,  // 群组聊天ID,单角色聊天为null
    val title: String,
    val messageCount: Int,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val jsonFilePath: String,  // JSONL文件路径
    val activeBranchId: String? = null,  // 当前激活的分支节点ID
    val rootNodesJson: String = "[]"  // 根节点ID列表(JSON数组)
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.ChatMetadata): ChatEntity {
            return ChatEntity(
                id = model.chatId.toString(),
                characterId = model.characterId.toString(),
                groupId = model.groupId?.toString(),
                title = model.title,
                messageCount = model.messageCount,
                pinned = model.pinned,
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli(),
                jsonFilePath = "",  // 文件路径由服务层管理
                activeBranchId = model.activeBranchId?.toString(),
                rootNodesJson = kotlinx.serialization.json.Json.encodeToString(
                    model.rootNodes.map { it.toString() }
                )
            )
        }
        
        fun toModel(entity: ChatEntity): com.eterultimate.eteruee.roleplay.data.model.ChatMetadata {
            val rootNodes = try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.rootNodesJson)
                    .mapNotNull { id -> runCatching { kotlin.uuid.Uuid.parse(id) }.getOrNull() }
            } catch (e: Exception) {
                emptyList()
            }
            
            return com.eterultimate.eteruee.roleplay.data.model.ChatMetadata(
                chatId = kotlin.uuid.Uuid.parse(entity.id),
                characterId = kotlin.uuid.Uuid.parse(entity.characterId),
                groupId = entity.groupId?.let { kotlin.uuid.Uuid.parse(it) },
                title = entity.title,
                messageCount = entity.messageCount,
                pinned = entity.pinned,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt),
                activeBranchId = entity.activeBranchId?.let { kotlin.uuid.Uuid.parse(it) },
                rootNodes = rootNodes
            )
        }
    }
}
