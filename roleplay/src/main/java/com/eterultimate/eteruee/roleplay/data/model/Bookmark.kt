package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 书签模型
 * 用于标记聊天中的重要消息或位置
 */
@Serializable
data class Bookmark(
    val id: Uuid = Uuid.random(),
    val chatId: Uuid,
    val characterId: Uuid,
    val messageId: Uuid? = null,  // 可选，标记特定消息
    val nodeId: Uuid? = null,     // 可选，标记特定分支节点
    val title: String = "",
    val note: String = "",        // 用户备注
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now(),
    val color: String = "#FFD700", // 书签颜色（十六进制）
    val tags: List<String> = emptyList()  // 标签列表
) {
    fun getDisplayName(): String {
        return title.ifBlank { "Bookmark" }
    }
    
    fun hasMessageReference(): Boolean {
        return messageId != null || nodeId != null
    }
}
