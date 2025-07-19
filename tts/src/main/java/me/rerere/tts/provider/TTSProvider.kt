package me.rerere.tts.provider

import kotlinx.coroutines.flow.Flow
import me.rerere.tts.model.AudioChunk
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse

interface TTSProvider<T : TTSProviderSetting> {
    suspend fun generateSpeech(
        providerSetting: T,
        request: TTSRequest
    ): TTSResponse

    suspend fun streamSpeech(
        providerSetting: T,
        request: TTSRequest
    ): Flow<AudioChunk>

    suspend fun testConnection(providerSetting: T): Boolean
}