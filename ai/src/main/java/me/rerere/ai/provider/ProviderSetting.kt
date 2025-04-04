package me.rerere.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ProviderSetting {
    abstract var enabled: Boolean
    abstract var name: String
    abstract var models: List<Model>

    @Serializable
    @SerialName("openai")
    class OpenAI(
        override var enabled: Boolean,
        override var name: String,
        override var models: List<Model>,
        var apiKey: String,
        var baseUrl: String,
    ) : ProviderSetting()

    @Serializable
    @SerialName("google")
    class Google(
        override var enabled: Boolean,
        override var name: String,
        override var models: List<Model>,
        var apiKey: String,
        var baseUrl: String
    ): ProviderSetting()
}

@Serializable
data class Model(
    val name: String,
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