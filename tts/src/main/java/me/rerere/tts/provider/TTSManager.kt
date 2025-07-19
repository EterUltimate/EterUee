package me.rerere.tts.provider

import kotlinx.coroutines.flow.Flow
import me.rerere.tts.model.AudioChunk
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse
import me.rerere.tts.provider.providers.OpenAITTSProvider
import me.rerere.tts.provider.providers.GeminiTTSProvider

class TTSManager {
  private val openAIProvider = OpenAITTSProvider()
  private val geminiProvider = GeminiTTSProvider()

  suspend fun generateSpeech(
    providerSetting: TTSProviderSetting,
    request: TTSRequest
  ): TTSResponse {
    return when (providerSetting) {
      is TTSProviderSetting.OpenAI -> openAIProvider.generateSpeech(providerSetting, request)
      is TTSProviderSetting.Gemini -> geminiProvider.generateSpeech(providerSetting, request)
    }
  }

  suspend fun streamSpeech(
    providerSetting: TTSProviderSetting,
    request: TTSRequest
  ): Flow<AudioChunk> {
    return when (providerSetting) {
      is TTSProviderSetting.OpenAI -> openAIProvider.streamSpeech(providerSetting, request)
      is TTSProviderSetting.Gemini -> geminiProvider.streamSpeech(providerSetting, request)
    }
  }

  suspend fun testConnection(providerSetting: TTSProviderSetting): Boolean {
    return when (providerSetting) {
      is TTSProviderSetting.OpenAI -> openAIProvider.testConnection(providerSetting)
      is TTSProviderSetting.Gemini -> geminiProvider.testConnection(providerSetting)
    }
  }
}