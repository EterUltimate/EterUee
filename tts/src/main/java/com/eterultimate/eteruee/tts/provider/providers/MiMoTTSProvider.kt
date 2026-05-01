package com.eterultimate.eteruee.tts.provider.providers

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.common.http.SseEvent
import me.rerere.common.http.sseFlow
import com.eterultimate.eteruee.tts.model.AudioChunk
import com.eterultimate.eteruee.tts.model.AudioFormat
import com.eterultimate.eteruee.tts.model.TTSRequest
import com.eterultimate.eteruee.tts.provider.TTSProvider
import com.eterultimate.eteruee.tts.provider.TTSProviderSetting
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.TimeUnit

// MiMo 娴佸紡闊抽鎸夋枃妗ｇず渚嬩娇鐢?24kHz PCM16LE
private const val MIMO_SAMPLE_RATE = 24000
private val JSON_MEDIA_TYPE = "application/json".toMediaType()
// 鍙叧蹇?delta.audio.data 鍏朵綑瀛楁蹇界暐
private val mimoJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class MiMoChunk(
    val choices: List<MiMoChoice> = emptyList()
)

@Serializable
private data class MiMoChoice(
    val delta: MiMoDelta? = null
)

@Serializable
private data class MiMoDelta(
    val audio: MiMoAudio? = null
)

@Serializable
private data class MiMoAudio(
    val data: String? = null
)

internal fun decodeMiMoAudioData(data: String): ByteArray? {
    val payload = data.trim()
    // [DONE] 琛ㄧず娴佺粨鏉?涓嶈緭鍑洪煶棰?
    if (payload == "[DONE]") return null
    // 闈?[DONE] 鐨?data 瑙嗕负 JSON 鐗囨 瑙ｆ瀽澶辫触鐩存帴涓婃姏
    val chunk = mimoJson.decodeFromString<MiMoChunk>(payload)
    val encoded = chunk.choices.firstOrNull()?.delta?.audio?.data ?: return null
    // 绌哄瓧绗︿覆瑙嗕负鏃犻煶棰戠墖娈?
    if (encoded.isBlank()) return null
    return Base64.getDecoder().decode(encoded)
}

internal class MiMoSseProcessor(
    private val model: String,
    private val voice: String
) {
    private var hasAudio = false
    // metadata 鍙瀯閫犱竴娆?璐┛鏁翠釜娴?
    private val metadata = mapOf(
        "provider" to "mimo",
        "model" to model,
        "voice" to voice
    )

    fun process(event: SseEvent): AudioChunk? {
        return when (event) {
            is SseEvent.Open -> null
            is SseEvent.Event -> {
                // 鍙鐞嗗寘鍚?audio.data 鐨勫閲忎簨浠?鍏朵粬浜嬩欢蹇界暐
                val pcmData = decodeMiMoAudioData(event.data) ?: return null
                hasAudio = true
                AudioChunk(
                    data = pcmData,
                    format = AudioFormat.PCM,
                    sampleRate = MIMO_SAMPLE_RATE,
                    metadata = metadata
                )
            }

            is SseEvent.Closed -> {
                // 濡傛灉鏁存娴佹病鏈変换浣曢煶棰戠墖娈?鐩存帴鎶ラ敊
                if (!hasAudio) {
                    throw IllegalStateException("MiMo TTS returned no audio chunks")
                }
                // 娴佸叧闂椂琛ヤ竴涓粓缁?chunk 渚夸簬鎾斁鍣ㄦ敹灏?
                AudioChunk(
                    data = byteArrayOf(),
                    format = AudioFormat.PCM,
                    sampleRate = MIMO_SAMPLE_RATE,
                    isLast = true,
                    metadata = metadata
                )
            }

            is SseEvent.Failure -> throw event.throwable ?: Exception("MiMo TTS streaming failed")
        }
    }
}

class MiMoTTSProvider : TTSProvider<TTSProviderSetting.MiMo> {
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun generateSpeech(
        context: Context,
        providerSetting: TTSProviderSetting.MiMo,
        request: TTSRequest
    ): Flow<AudioChunk> = flow {
        // OpenAI 鍏煎鐨?chat/completions SSE 娴佸紡杩斿洖 闊抽澧為噺鍦?delta.audio.data
        val requestBody = buildJsonObject {
            put("model", providerSetting.model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "assistant")
                    put("content", request.text)
                })
            })
            put("audio", buildJsonObject {
                put("format", "pcm16")
                put("voice", providerSetting.voice)
            })
            put("stream", true)
        }

        // baseUrl 鍏佽鐢ㄦ埛鍦ㄨ缃〉鑷畾涔?杩欓噷鐩存帴鎷兼帴璺緞
        val httpRequest = Request.Builder()
            .url("${providerSetting.baseUrl}/chat/completions")
            // MiMo 浣跨敤 api-key 澶翠紶 token
            .addHeader("api-key", providerSetting.apiKey)
            .addHeader("Content-Type", "application/json")
            // JsonObject 鐨?toString 浼氳緭鍑?JSON 瀛楃涓?
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val processor = MiMoSseProcessor(
            model = providerSetting.model,
            voice = providerSetting.voice
        )

        httpClient.sseFlow(httpRequest).collect { event ->
            processor.process(event)?.let { emit(it) }
        }
    }
}

