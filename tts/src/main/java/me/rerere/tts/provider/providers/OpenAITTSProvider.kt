package me.rerere.tts.provider.providers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import me.rerere.tts.model.AudioChunk
import me.rerere.tts.model.AudioFormat
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse
import me.rerere.tts.provider.TTSProvider
import me.rerere.tts.provider.TTSProviderSetting
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OpenAITTSProvider : TTSProvider<TTSProviderSetting.OpenAI> {

    private val httpClient = OkHttpClient()

    override suspend fun generateSpeech(
        providerSetting: TTSProviderSetting.OpenAI,
        request: TTSRequest
    ): TTSResponse {
        val requestBody = JSONObject().apply {
            put("model", providerSetting.model)
            put("input", request.text)
            put("voice", providerSetting.voice)
            put("speed", providerSetting.speed)
            put("response_format", "mp3") // Default to MP3
        }

        val httpRequest = Request.Builder()
            .url("${providerSetting.baseUrl}/audio/speech")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            throw Exception("TTS request failed: ${response.code} ${response.message}")
        }

        val audioData = response.body.bytes()

        return TTSResponse(
            audioData = audioData,
            format = AudioFormat.MP3,
            metadata = mapOf(
                "provider" to "openai",
                "model" to providerSetting.model,
                "voice" to providerSetting.voice,
                "speed" to providerSetting.speed.toString()
            )
        )
    }

    override suspend fun streamSpeech(
        providerSetting: TTSProviderSetting.OpenAI,
        request: TTSRequest
    ): Flow<AudioChunk> = flow {
        // OpenAI currently doesn't support streaming TTS
        // We'll implement it as a single chunk
        val response = generateSpeech(providerSetting, request)
        emit(AudioChunk(
            data = response.audioData,
            isLast = true,
            metadata = response.metadata
        ))
    }

    override suspend fun testConnection(providerSetting: TTSProviderSetting.OpenAI): Boolean {
        return try {
            val request = Request.Builder()
                .url("${providerSetting.baseUrl}/models")
                .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

}