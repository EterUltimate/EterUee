package me.rerere.ai.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.providers.OpenAIProvider
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.handleMessageChunk


suspend fun test() = withContext(Dispatchers.IO) {
    var conversation = Conversation(
        messages = listOf(
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(
                    UIMessagePart.Text("Hi"),
                ),
                id = "??"
            )
        )
    )
    OpenAIProvider.streamText(
        providerSetting = ProviderSetting.OpenAI(
            enabled = true,
            name = "CloseAI",
            apiKey = "sk-8Jf80gTBWPL5mqPtuWpbNwfCHo7n8TcNUXCVx98i8cpIW1hf",
            baseUrl = "https://api.openai-proxy.org/v1",
            models = emptyList(),
        ),
        conversation = conversation,
        params = TextGenerationParams(
            model = Model("gemini-2.0-flash")
        )
    ).collect {
        println(it)

        conversation = conversation.copy(
            messages = conversation.messages.handleMessageChunk(it)
        )

        println(conversation.messages)
    }
    println("test done")
}