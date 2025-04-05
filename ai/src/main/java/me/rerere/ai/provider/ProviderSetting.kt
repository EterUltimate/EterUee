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

    @Serializable
    @SerialName("openai")
    data class OpenAI(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "OpenAI",
        override var models: List<Model> = emptyList(),
        var apiKey: String = "sk-",
        var baseUrl: String = "https://api.openai.com/v1",
    ) : ProviderSetting()

    @Serializable
    @SerialName("google")
    data class Google(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "Google",
        override var models: List<Model> = emptyList(),
        var apiKey: String = "",
        var baseUrl: String = "https://generativelanguage.googleapis.com"
    ): ProviderSetting()

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
    val name: String,
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
)

@Serializable
enum class ModelType {
    CHAT,
    EMBEDDING
}

fun main() {
    val settings = listOf(
        ProviderSetting.OpenAI(
            enabled = true,
            name = "OpenAI",
            apiKey = "sk-...",
            baseUrl = "https://api.openai.com",
            models = listOf(
                Model("gpt-3.5-turbo"),
                Model("gpt-4")
            )
        ),
        ProviderSetting.Google(
            enabled = true,
            name = "Google",
            apiKey = "sk-...",
            baseUrl = "https://api.openai.com",
            models = listOf(
                Model("gpt-3.5-turbo"),
                Model("gpt-4")
            )
        )
    )

    println(Json.encodeToString(settings))
    println(Json.decodeFromString<List<ProviderSetting>>(Json.encodeToString(settings)))
}