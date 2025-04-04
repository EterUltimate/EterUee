package me.rerere.ai.provider

import kotlinx.coroutines.flow.Flow
import me.rerere.ai.ui.MessageChunk

// 提供商实现
// 采用无状态设计，使用时除了需要传入需要的参数外，还需要传入provider setting作为参数
interface Provider<T : ProviderSetting> {
    suspend fun generateText(providerSetting: ProviderSetting): MessageChunk

    suspend fun streamText(providerSetting: ProviderSetting): Flow<MessageChunk>
}
