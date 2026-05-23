package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson

/**
 * 世界书数据库实体
 */
@Entity(tableName = "rp_world_infos")
data class WorldInfoEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val scanDepth: Int,
    val scanTrigger: String,  // enum ordinal name
    val selectiveLogic: String,  // enum ordinal name
    val createdAt: Long,
    val updatedAt: Long,
    val entriesJson: String,  // List<WorldInfoEntry> JSON
    val jsonData: String = ""
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.WorldInfo): WorldInfoEntity {
            return WorldInfoEntity(
                id = model.id.toString(),
                name = model.name,
                description = model.description,
                scanDepth = model.scanDepth,
                scanTrigger = model.scanTrigger.name,
                selectiveLogic = model.selectiveLogic.name,
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli(),
                entriesJson = RoleplayJson.encodeToString(model.entries),
                jsonData = RoleplayJson.encodeToString(model)
            )
        }
        
        fun toModel(entity: WorldInfoEntity): com.eterultimate.eteruee.roleplay.data.model.WorldInfo {
            if (entity.jsonData.isNotBlank()) {
                return RoleplayJson.decodeFromString(entity.jsonData)
            }

            val entries = RoleplayJson.decodeFromString<List<com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry>>(
                entity.entriesJson
            )
            
            return com.eterultimate.eteruee.roleplay.data.model.WorldInfo(
                id = kotlin.uuid.Uuid.parse(entity.id),
                name = entity.name,
                description = entity.description,
                entries = entries,
                scanDepth = entity.scanDepth,
                scanTrigger = com.eterultimate.eteruee.roleplay.data.model.ScanTrigger.valueOf(entity.scanTrigger),
                selectiveLogic = com.eterultimate.eteruee.roleplay.data.model.SelectiveLogic.valueOf(entity.selectiveLogic),
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }
}
