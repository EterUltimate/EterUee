package me.rerere.ai.provider

import kotlinx.coroutines.flow.Flow
import me.rerere.ai.ui.MessageChunk

abstract class AIProvider<S : AIProviderSetting> {
    abstract val name: String

    abstract val description: String

    abstract fun streamText(): Flow<MessageChunk>

    abstract fun generateText(): MessageChunk
}

abstract class AIProviderSetting(var enabled: Boolean)