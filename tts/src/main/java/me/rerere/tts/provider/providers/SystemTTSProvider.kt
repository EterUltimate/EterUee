package me.rerere.tts.provider.providers

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import me.rerere.tts.model.AudioChunk
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse
import me.rerere.tts.provider.TTSProvider
import me.rerere.tts.provider.TTSProviderSetting
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SystemTTSProvider(private val context: Context) : TTSProvider<TTSProviderSetting.SystemTTS> {

    private var textToSpeech: TextToSpeech? = null

    override suspend fun generateSpeech(
        providerSetting: TTSProviderSetting.SystemTTS,
        request: TTSRequest
    ): TTSResponse = suspendCancellableCoroutine { continuation ->
        
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech!!
                
                // Set language
                val locale = Locale.forLanguageTag(providerSetting.language)
                val langResult = tts.setLanguage(locale)
                
                if (langResult == TextToSpeech.LANG_MISSING_DATA || 
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    continuation.resumeWithException(
                        Exception("Language ${providerSetting.language} not supported")
                    )
                    return@TextToSpeech
                }
                
                // Set speech parameters
                tts.setSpeechRate(providerSetting.speechRate)
                tts.setPitch(providerSetting.pitch)
                
                // Create temporary file for audio output
                val audioFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.wav")
                
                val utteranceId = UUID.randomUUID().toString()
                
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    
                    override fun onDone(utteranceId: String?) {
                        try {
                            if (audioFile.exists()) {
                                val audioData = audioFile.readBytes()
                                audioFile.delete()
                                
                                val response = TTSResponse(
                                    audioData = audioData,
                                    format = me.rerere.tts.model.AudioFormat.WAV,
                                    metadata = mapOf(
                                        "provider" to "system",
                                        "language" to providerSetting.language,
                                        "speechRate" to providerSetting.speechRate.toString(),
                                        "pitch" to providerSetting.pitch.toString()
                                    )
                                )
                                continuation.resume(response)
                            } else {
                                continuation.resumeWithException(
                                    Exception("Failed to generate audio file")
                                )
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            tts.shutdown()
                        }
                    }
                    
                    override fun onError(utteranceId: String?) {
                        audioFile.delete()
                        continuation.resumeWithException(
                            Exception("TTS synthesis failed")
                        )
                        tts.shutdown()
                    }
                })
                
                val result = tts.synthesizeToFile(
                    request.text,
                    null,
                    audioFile,
                    utteranceId
                )
                
                if (result != TextToSpeech.SUCCESS) {
                    continuation.resumeWithException(
                        Exception("Failed to start TTS synthesis")
                    )
                    tts.shutdown()
                }
                
            } else {
                continuation.resumeWithException(
                    Exception("Failed to initialize TextToSpeech engine")
                )
            }
        }
        
        continuation.invokeOnCancellation {
            textToSpeech?.shutdown()
        }
    }

    override suspend fun streamSpeech(
        providerSetting: TTSProviderSetting.SystemTTS,
        request: TTSRequest
    ): Flow<AudioChunk> = flow {
        // System TTS doesn't support streaming, so we'll implement it as a single chunk
        val response = generateSpeech(providerSetting, request)
        emit(AudioChunk(
            data = response.audioData,
            isLast = true,
            metadata = response.metadata
        ))
    }

    override suspend fun testConnection(providerSetting: TTSProviderSetting.SystemTTS): Boolean {
        return suspendCancellableCoroutine { continuation ->
            var testTts: TextToSpeech? = null
            try {
                testTts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val locale = Locale.forLanguageTag(providerSetting.language)
                        val langResult = testTts!!.setLanguage(locale)
                        val isSupported = langResult != TextToSpeech.LANG_MISSING_DATA && 
                                        langResult != TextToSpeech.LANG_NOT_SUPPORTED
                        continuation.resume(isSupported)
                    } else {
                        continuation.resume(false)
                    }
                    testTts!!.shutdown()
                }
                
                continuation.invokeOnCancellation {
                    testTts?.shutdown()
                }
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }
    }
}