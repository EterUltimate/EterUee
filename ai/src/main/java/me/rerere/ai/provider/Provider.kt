package me.rerere.ai.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage

// 提供商实现
// 采用无状态设计，使用时除了需要传入需要的参数外，还需要传入provider setting作为参数
interface Provider<T : ProviderSetting> {
    suspend fun listModels(providerSetting: T) : List<Model>

    suspend fun generateText(
        providerSetting: T,
        messages: List<UIMessage>,
        params: TextGenerationParams,
        messageTransformers: List<MessageTransformer> = emptyList()
    ): MessageChunk

    suspend fun streamText(
        providerSetting: T,
        messages: List<UIMessage>,
        params: TextGenerationParams,
        messageTransformers: List<MessageTransformer> = emptyList()
    ): Flow<MessageChunk>
}

@Serializable
data class TextGenerationParams(
    val model: Model,
    val temperature: Float = 0.6f,
    val topP: Float = 1f,
)