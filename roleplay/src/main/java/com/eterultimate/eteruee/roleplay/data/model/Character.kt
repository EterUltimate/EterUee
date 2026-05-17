package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 角色卡模型 - 兼容 SillyTavern V2/V3 格式
 */
@Serializable
data class Character(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val messageExamples: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val avatarUrl: String? = null,  // 本地文件路径或URL
    val creator: String = "",
    val creatorNotes: String = "",
    val tags: List<String> = emptyList(),
    val alternateGreetings: List<String> = emptyList(),  // 滑动回复备选问候
    val characterVersion: String = "",
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now(),
    val favorite: Boolean = false,
    val chatCount: Int = 0,
    @Contextual val lastChatAt: Instant? = null,
    // V2/V3 兼容字段
    val spec: String = "chara_card_v3",
    val specVersion: String = "3.0",
    // 扩展字段(透传,保持兼容性)
    val extensions: Map<String, JsonElement> = emptyMap()
) {
    /**
     * 获取显示名称,如果为空则返回默认值
     */
    fun getDisplayName(): String {
        return name.ifBlank { "Unnamed Character" }
    }
    
    /**
     * 获取系统提示词,如果为空则使用默认模板
     */
    fun getEffectiveSystemPrompt(): String {
        return systemPrompt.ifBlank { "You are ${name.ifBlank { "a character" }}." }
    }
}
