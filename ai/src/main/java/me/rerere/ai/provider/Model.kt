package me.rerere.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Model(
    val modelId: String = "",
    val displayName: String = "",
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val inputModalities: List<Modality> = listOf(Modality.TEXT),
    val outputModalities: List<Modality> = listOf(Modality.TEXT),
    val abilities: List<ModelAbility> = emptyList(),
    val tools: Set<BuiltInTools> = emptySet(),
)

@Serializable
enum class ModelType {
    CHAT,
    EMBEDDING,
}

@Serializable
enum class Modality {
    TEXT,
    IMAGE,
}

@Serializable
enum class ModelAbility {
    TOOL,
    REASONING,
}

// 模型(提供商)提供的内置工具选项
@Serializable
sealed class BuiltInTools {
    // https://ai.google.dev/gemini-api/docs/google-search?hl=zh-cn
    @Serializable
    @SerialName("search")
    data object Search : BuiltInTools()

    // https://ai.google.dev/gemini-api/docs/url-context?hl=zh-cn
    @Serializable
    @SerialName("url_context")
    data object UrlContext : BuiltInTools()
}

fun guessModalityFromModelId(modelId: String): Pair<List<Modality>, List<Modality>> {
    return when {
        GPT4O.containsMatchIn(modelId) || GPT_4_1.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        GEMINI_20_FLASH.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        CLAUDE_SONNET_3_5.containsMatchIn(modelId) || CLAUDE_SONNET_3_7.containsMatchIn(modelId) || CLAUDE_4.containsMatchIn(
            modelId
        ) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        DOUBAO_1_6.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        GROK_4.containsMatchIn(modelId) -> {
            listOf(Modality.TEXT, Modality.IMAGE) to listOf(Modality.TEXT)
        }

        else -> {
            listOf(Modality.TEXT) to listOf(Modality.TEXT)
        }
    }
}

fun guessModelAbilityFromModelId(modelId: String): List<ModelAbility> {
    return when {
        GPT4O.containsMatchIn(modelId) || GPT_4_1.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL)
        }

        OPENAI_O_MODELS.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        GEMINI_20_FLASH.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL)
        }

        GEMINI_2_5_FLASH.containsMatchIn(modelId) || GEMINI_2_5_PRO.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        CLAUDE_SONNET_3_5.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL)
        }

        CLAUDE_SONNET_3_7.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        CLAUDE_4.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        QWEN_3.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        DOUBAO_1_6.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        GROK_4.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL, ModelAbility.REASONING)
        }

        KIMI_K2.containsMatchIn(modelId) -> {
            listOf(ModelAbility.TOOL)
        }

        else -> {
            emptyList()
        }
    }
}

private val OPENAI_O_MODELS = Regex("o\\d")
private val GPT4O = Regex("gpt-4o")
private val GPT_4_1 = Regex("gpt-4\\.1")
private val GEMINI_20_FLASH = Regex("gemini-2.0-flash")
private val GEMINI_2_5_FLASH = Regex("gemini-2.5-flash")
private val GEMINI_2_5_PRO = Regex("gemini-2.5-pro")
private val CLAUDE_SONNET_3_5 = Regex("claude-3.5-sonnet")
private val CLAUDE_SONNET_3_7 = Regex("claude-3.7-sonnet")
private val CLAUDE_4 = Regex("claude.*-4")
private val QWEN_3 = Regex("qwen-?3")
private val DOUBAO_1_6 = Regex("doubao.+1([-.])6")
private val GROK_4 = Regex("grok-4")
private val KIMI_K2 = Regex("kimi-k2")
