package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 世界书(World Info)模型
 */
@Serializable
data class WorldInfo(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val description: String = "",
    val entries: List<WorldInfoEntry> = emptyList(),
    val scanDepth: Int = 4,  // 扫描最近N条消息
    val scanTrigger: ScanTrigger = ScanTrigger.ALWAYS,
    val selectiveLogic: SelectiveLogic = SelectiveLogic.AND,
    val extensions: Map<String, JsonElement> = emptyMap(),
    val originalData: JsonElement? = null,
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
)

/**
 * 世界书条目
 */
@Serializable
data class WorldInfoEntry(
    val id: Uuid = Uuid.random(),
    val key: String = "",  // 主要触发关键词
    val keys: List<String> = emptyList(),  // 多个关键词
    val secondaryKeys: List<String> = emptyList(),
    val comment: String = "",
    val content: String = "",  // 注入的内容
    val constant: Boolean = false,  // 是否始终注入
    val selective: Boolean = false,
    val order: Int = 0,  // 注入顺序
    val position: InsertionPosition = InsertionPosition.AFTER_SYSTEM_PROMPT,
    val tavernPosition: Int = 1,
    val enabled: Boolean = true,
    val probability: Float = 1.0f,  // 触发概率 (0.0 - 1.0)
    val useProbability: Boolean = false,
    val depth: Int = 4,
    val role: Int = 0,
    val displayIndex: Int = 0,
    val excludeRecursion: Boolean = false,
    val preventRecursion: Boolean = false,
    val delayUntilRecursion: Boolean = false,
    val outletName: String = "",
    val group: String = "",
    val groupOverride: Boolean = false,
    val groupWeight: JsonElement? = null,
    val scanDepthOverride: JsonElement? = null,
    val caseSensitive: JsonElement? = null,
    val matchWholeWords: JsonElement? = null,
    val useGroupScoring: JsonElement? = null,
    val automationId: String = "",
    val vectorized: Boolean = false,
    val sticky: JsonElement? = null,
    val cooldown: JsonElement? = null,
    val delay: JsonElement? = null,
    val triggers: List<String> = emptyList(),
    val ignoreBudget: Boolean = false,
    val matchPersonaDescription: Boolean = false,
    val matchCharacterDescription: Boolean = false,
    val matchCharacterPersonality: Boolean = false,
    val matchCharacterDepthPrompt: Boolean = false,
    val matchScenario: Boolean = false,
    val matchCreatorNotes: Boolean = false,
    val extensions: Map<String, JsonElement> = emptyMap()
) {
    /**
     * 获取所有关键词(包含主关键词)
     */
    fun getAllKeys(): List<String> {
        return if (key.isNotBlank()) {
            listOf(key) + keys
        } else {
            keys
        }
    }
}

/**
 * 扫描触发模式
 */
@Serializable
enum class ScanTrigger {
    ALWAYS,           // 始终扫描
    FIRST_MESSAGE,    // 仅第一条消息时扫描
    RECURSIVE_SCAN    // 递归扫描
}

/**
 * 选择逻辑
 */
@Serializable
enum class SelectiveLogic {
    AND,  // 所有关键词都匹配
    OR    // 任一关键词匹配
}

/**
 * 插入位置
 */
@Serializable
enum class InsertionPosition {
    AFTER_SYSTEM_PROMPT,      // 系统提示词之后
    BEFORE_LAST_USER_MESSAGE, // 最后一条用户消息之前
    AT_END                    // 末尾
}
