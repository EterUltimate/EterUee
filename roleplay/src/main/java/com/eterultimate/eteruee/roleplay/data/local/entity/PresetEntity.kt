package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 生成预设数据库实体
 */
@Entity(tableName = "rp_presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val type: String,  // PresetType.name()
    val parametersJson: String,  // JSON字符串存储Map参数
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Preset): PresetEntity {
            return PresetEntity(
                id = model.id.toString(),
                name = model.name,
                description = model.description,
                type = model.type.name,
                parametersJson = kotlinx.serialization.json.Json.encodeToString(model.parameters),
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli()
            )
        }
        
        fun toModel(entity: PresetEntity): com.eterultimate.eteruee.roleplay.data.model.Preset {
            val parameters: Map<String, Any> = kotlinx.serialization.json.Json.decodeFromString(entity.parametersJson)
            return com.eterultimate.eteruee.roleplay.data.model.Preset(
                id = kotlin.uuid.Uuid.parse(entity.id),
                name = entity.name,
                description = entity.description,
                type = com.eterultimate.eteruee.roleplay.data.model.PresetType.valueOf(entity.type),
                parameters = parameters,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }
}
