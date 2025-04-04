package me.rerere.ai.provider

import kotlinx.serialization.json.Json
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.providers.OpenAIProvider
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageContent

val TestProvider = OpenAIProvider()

suspend fun main() {
    val response = TestProvider.generateText(
        providerSetting = ProviderSetting.OpenAI(
            enabled = true,
            name = "CloseAI",
            apiKey = "sk-8Jf80gTBWPL5mqPtuWpbNwfCHo7n8TcNUXCVx98i8cpIW1hf",
            baseUrl = "https://api.openai-proxy.org",
            models = emptyList(),
        ),
        conversation = Conversation(
            messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    content = listOf(
                        UIMessageContent.Text("你好啊")
                    )
                )
            )
        ),
        params = TextGenerationParams()
    )
    val message = response.choices[0].message
    when(message) {
        is UIMessageContent.Text -> println(message.text)
        is UIMessageContent.Image -> TODO()
        is UIMessageContent.Reasoning -> TODO()
        null -> TODO()
    }
}