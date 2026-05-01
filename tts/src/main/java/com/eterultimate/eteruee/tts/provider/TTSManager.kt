package com.eterultimate.eteruee.tts.provider

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.eterultimate.eteruee.tts.model.AudioChunk
import com.eterultimate.eteruee.tts.model.TTSRequest
import com.eterultimate.eteruee.tts.provider.providers.GeminiTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.GroqTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.MiMoTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.MiniMaxTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.OpenAITTSProvider
import com.eterultimate.eteruee.tts.provider.providers.QwenTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.SystemTTSProvider
import com.eterultimate.eteruee.tts.provider.providers.XAITTSProvider

class TTSManager(private val context: Context) {
    private val openAIProvider = OpenAITTSProvider()
    private val geminiProvider = GeminiTTSProvider()
    private val systemProvider = SystemTTSProvider()
    private val miniMaxProvider = MiniMaxTTSProvider()
    private val qwenProvider = QwenTTSProvider()
    private val groqProvider = GroqTTSProvider()
    private val xaiProvider = XAITTSProvider()
    private val miMoProvider = MiMoTTSProvider()

    fun generateSpeech(
        providerSetting: TTSProviderSetting,
        request: TTSRequest
    ): Flow<AudioChunk> {
        return when (providerSetting) {
            is TTSProviderSetting.OpenAI -> openAIProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.Gemini -> geminiProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.SystemTTS -> systemProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.MiniMax -> miniMaxProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.Qwen -> qwenProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.Groq -> groqProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.XAI -> xaiProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.MiMo -> miMoProvider.generateSpeech(context, providerSetting, request)
        }
    }
}

