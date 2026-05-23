package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 书签数据模型
 */
@Serializable
data class Bookmark(
    val id: Uuid = Uuid.random(),
    val chatId: Uuid,
    val messageIndex: Int,  // 消息在聊天中的索引位置
    val title: String = "",  // 书签标题（可选）
    val note: String = "",  // 备注说明（可选）
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
) {
    fun getDisplayName(): String {
        return title.ifBlank { "Bookmark #${id.toString().take(8)}" }
    }
}
