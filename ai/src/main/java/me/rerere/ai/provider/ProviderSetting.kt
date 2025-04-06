package me.rerere.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

@Serializable
sealed class ProviderSetting {
    abstract var id: Uuid
    abstract var enabled: Boolean
    abstract var name: String
    abstract var models: List<Model>

    abstract fun addModel(model: Model): ProviderSetting
    abstract fun editModel(model: Model): ProviderSetting
    abstract fun delModel(model: Model): ProviderSetting

    @Serializable
    @SerialName("openai")
    data class OpenAI(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "OpenAI",
        override var models: List<Model> = emptyList(),
        var apiKey: String = "sk-",
        var baseUrl: String = "https://api.openai.com/v1",
    ) : ProviderSetting() {
        override fun addModel(model: Model): ProviderSetting {
            return copy(models = models + model)
        }

        override fun editModel(model: Model): ProviderSetting {
            return copy(models = models.map { if (it.id == model.id) model else it })
        }

        override fun delModel(model: Model): ProviderSetting {
            return copy(models = models.filter { it.id != model.id })
        }
    }

    @Serializable
    @SerialName("google")
    data class Google(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "Google",
        override var models: List<Model> = emptyList(),
        var apiKey: String = "",
        var baseUrl: String = "https://generativelanguage.googleapis.com"
    ): ProviderSetting() {
        override fun addModel(model: Model): ProviderSetting {
            return copy(models = models + model)
        }

        override fun editModel(model: Model): ProviderSetting {
            return copy(models = models.map { if (it.id == model.id) model else it })
        }

        override fun delModel(model: Model): ProviderSetting {
            return copy(models = models.filter { it.id != model.id })
        }
    }

    companion object {
        val Types by lazy {
            listOf(
                OpenAI::class,
                Google::class
            )
        }
    }
}

@Serializable
data class Model(
    val modelId: String,
    val displayName: String,
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
)

@Serializable
enum class ModelType {
    CHAT,
    EMBEDDING
}