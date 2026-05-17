package com.eterultimate.eteruee.roleplay.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 生成预设模型 (KoboldAI/TextGen/OpenAI)
 */
@Serializable
data class Preset(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val description: String = "",
    val type: PresetType = PresetType.OPENAI,
    @Contextual val parameters: Map<String, @Contextual Any> = emptyMap(),
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
)

/**
 * 预设类型
 */
@Serializable
enum class PresetType {
    OPENAI,
    KOBOLDAI,
    TEXTGEN,
    CLAUDE,
    GEMINI
}
