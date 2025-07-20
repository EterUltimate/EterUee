package me.rerere.tts.provider

import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse

interface TTSProvider<T : TTSProviderSetting> {
    suspend fun generateSpeech(
        providerSetting: T,
        request: TTSRequest
    ): TTSResponse
}
