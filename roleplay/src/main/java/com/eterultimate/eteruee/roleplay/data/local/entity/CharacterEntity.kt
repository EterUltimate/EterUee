package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 角色卡数据库实体
 */
@Entity(tableName = "rp_characters")
data class CharacterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val favorite: Boolean,
    val chatCount: Int,
    val lastChatAt: Long?,  // epoch millis
    val createdAt: Long,
    val updatedAt: Long,
    val jsonData: String  // 完整JSON存储,避免字段映射丢失
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Character): CharacterEntity {
            return CharacterEntity(
                id = model.id.toString(),
                name = model.name,
                avatarUrl = model.avatarUrl,
                favorite = model.favorite,
                chatCount = model.chatCount,
                lastChatAt = model.lastChatAt?.toEpochMilli(),
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli(),
                jsonData = kotlinx.serialization.json.Json.encodeToString(model)
            )
        }
        
        fun toModel(entity: CharacterEntity): com.eterultimate.eteruee.roleplay.data.model.Character {
            return kotlinx.serialization.json.Json.decodeFromString(entity.jsonData)
        }
    }
}
