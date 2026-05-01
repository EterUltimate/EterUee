package com.eterultimate.eteruee.tts.provider.providers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.eterultimate.eteruee.tts.model.AudioChunk
import com.eterultimate.eteruee.tts.model.AudioFormat
import com.eterultimate.eteruee.tts.model.TTSRequest
import com.eterultimate.eteruee.tts.provider.TTSProvider
import com.eterultimate.eteruee.tts.provider.TTSProviderSetting
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "XAITTSProvider"

class XAITTSProvider : TTSProvider<TTSProviderSetting.XAI> {
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun generateSpeech(
        context: Context,
        providerSetting: TTSProviderSetting.XAI,
        request: TTSRequest
    ): Flow<AudioChunk> = flow {
        val requestBody = JSONObject().apply {
            put("text", request.text)
            put("voice_id", providerSetting.voiceId)
            put("language", providerSetting.language)
        }

        Log.i(TAG, "generateSpeech: $requestBody")

        val httpRequest = Request.Builder()
            .url("${providerSetting.baseUrl}/tts")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "generateSpeech: ${response.code} ${response.message}")
            Log.e(TAG, "generateSpeech: $errorBody")
            throw Exception("xAI TTS request failed: ${response.code} ${response.message}")
        }

        val audioData = response.body.bytes()

        emit(
            AudioChunk(
                data = audioData,
                format = AudioFormat.MP3,
                isLast = true,
                metadata = mapOf(
                    "provider" to "xai",
                    "voice_id" to providerSetting.voiceId,
                    "language" to providerSetting.language
                )
            )
        )
    }
}

