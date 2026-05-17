package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 群组模型
 */
@Serializable
data class Group(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val description: String = "",
    val members: List<GroupMember> = emptyList(),
    val activeMembers: Set<Uuid> = emptySet(),  // 当前活跃成员ID
    val avatarUrl: String? = null,
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
) {
    fun getDisplayName(): String {
        return name.ifBlank { "Untitled Group" }
    }
}

/**
 * 群组成员
 */
@Serializable
data class GroupMember(
    val characterId: Uuid,
    val name: String = "",
    val priority: Int = 0,  // 发言优先级 (越高越优先)
    val responseProbability: Float = 1.0f,  // 响应概率 (0.0 - 1.0)
    val forcedResponse: Boolean = false  // 强制响应
)
