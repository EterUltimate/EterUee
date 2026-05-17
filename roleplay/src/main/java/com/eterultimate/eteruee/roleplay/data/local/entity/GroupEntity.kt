package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 群组数据库实体
 */
@Entity(tableName = "rp_groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val membersJson: String,  // List<GroupMember> JSON
    val activeMembersJson: String,  // Set<Uuid> JSON (stored as list)
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Group): GroupEntity {
            return GroupEntity(
                id = model.id.toString(),
                name = model.name,
                description = model.description,
                avatarUrl = model.avatarUrl,
                membersJson = kotlinx.serialization.json.Json.encodeToString(model.members),
                activeMembersJson = kotlinx.serialization.json.Json.encodeToString(model.activeMembers.toList()),
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli()
            )
        }
        
        fun toModel(entity: GroupEntity): com.eterultimate.eteruee.roleplay.data.model.Group {
            val members = kotlinx.serialization.json.Json.decodeFromString<List<com.eterultimate.eteruee.roleplay.data.model.GroupMember>>(
                entity.membersJson
            )
            val activeMembersList = kotlinx.serialization.json.Json.decodeFromString<List<String>>(
                entity.activeMembersJson
            )
            
            return com.eterultimate.eteruee.roleplay.data.model.Group(
                id = kotlin.uuid.Uuid.parse(entity.id),
                name = entity.name,
                description = entity.description,
                members = members,
                activeMembers = activeMembersList.map { kotlin.uuid.Uuid.parse(it) }.toSet(),
                avatarUrl = entity.avatarUrl,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }
}
