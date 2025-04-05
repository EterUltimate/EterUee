package me.rerere.ai.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.providers.OpenAIProvider
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

val TestProvider = OpenAIProvider()

suspend fun test() = withContext(Dispatchers.IO) {
    val response = TestProvider.generateText(
        providerSetting = ProviderSetting.OpenAI(
            enabled = true,
            name = "CloseAI",
            apiKey = "sk-8Jf80gTBWPL5mqPtuWpbNwfCHo7n8TcNUXCVx98i8cpIW1hf",
            baseUrl = "https://api.openai-proxy.org/v1",
            models = emptyList(),
        ),
        conversation = Conversation(
            messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(
                        UIMessagePart.Text("你好啊")
                    )
                )
            )
        ),
        params = TextGenerationParams(
            model = Model("deepseek-reasoner")
        )
    )
    val message = response.choices[0].message
    println(message)
}