package me.rerere.tts.provider.providers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.model.TTSResponse
import me.rerere.tts.provider.TTSProvider
import me.rerere.tts.provider.TTSProviderSetting
import java.io.File
import java.util.Locale
import java.util.UUID
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
                val locale = Locale.getDefault()
                val langResult = tts.setLanguage(locale)

                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
//                    continuation.resumeWithException(
//                        Exception("Language $locale not supported")
//                    )
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
}
