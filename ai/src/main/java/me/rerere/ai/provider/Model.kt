package me.rerere.ai.provider

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Model(
    val modelId: String,
    val displayName: String,
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
    val inputModalities: List<Modality> = listOf(Modality.TEXT),
    val outputModalities: List<Modality> = listOf(Modality.TEXT),
)

@Serializable
enum class ModelType {
    CHAT,
    EMBEDDING
}

@Serializable
enum class Modality {
    TEXT,
    IMAGE,
}

fun guessModalityFromModelId(modelId: String): Pair<List<Modality>, List<Modality>> {
    return when {
        GPT4O.containsMatchIn(modelId) || GPT_4_1.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        GEMINI_20_FLASH.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        CLAUDE_SONNET_3.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        else -> {
            listOf(Modality.TEXT) to listOf(Modality.TEXT)
        }
    }
}

private val GPT4O = Regex("gpt-4o")
private val GPT_4_1 = Regex("gpt-4\\.1")
private val GEMINI_20_FLASH = Regex("gemini-2.0-flash")
private val CLAUDE_SONNET_3 = Regex("claude-3.+sonnet")