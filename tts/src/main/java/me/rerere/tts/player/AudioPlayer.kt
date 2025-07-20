package me.rerere.tts.player

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.rerere.tts.model.AudioFormat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AudioPlayer"

/**
 * Coroutine-based audio player for TTS module
 */
class AudioPlayer(private val context: Context) {

    /**
     * Play audio data with specified format using coroutines
     */
    suspend fun playSound(
        sound: ByteArray,
        format: AudioFormat
    ): Unit = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val tempFile = File.createTempFile("audio_temp", getFileExtension(format), context.cacheDir)
                tempFile.writeBytes(sound)

                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnCompletionListener { player ->
                        player.release()
                        tempFile.delete()
                        Log.d(TAG, "Audio playback completed")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    setOnErrorListener { player, what, extra ->
                        val errorMsg = "MediaPlayer error occurred $what $extra"
                        Log.e(TAG, errorMsg)
                        player.release()
                        tempFile.delete()
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception(errorMsg))
                        }
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener { player ->
                        player.start()
                        Log.d(TAG, "Audio playback started")
                    }
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Audio playback cancelled")
                    try {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.stop()
                        }
                        mediaPlayer.release()
                        tempFile.delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during cancellation cleanup", e)
                    }
                }

                Log.i(TAG, "Playing audio with format: $format")
            } catch (e: Exception) {
                val errorMsg = "Failed to play sound: ${e.message}"
                Log.e(TAG, errorMsg, e)
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception(errorMsg))
                }
            }
        }
    }

    /**
     * Play PCM audio data using coroutines
     */
    suspend fun playPcmSound(
        pcmData: ByteArray,
        sampleRate: Int = 24000
    ): Unit = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            var audioTrack: AudioTrack? = null
            
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
                )

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "PCM playback cancelled")
                    try {
                        audioTrack?.let { track ->
                            if (track.state == AudioTrack.STATE_INITIALIZED) {
                                track.stop()
                                track.release()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during PCM cancellation cleanup", e)
                    }
                }

                audioTrack.play()
                Log.d(TAG, "PCM playback started")
                
                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack.stop()
                audioTrack.release()
                
                Log.i(TAG, "PCM playback completed")
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            } catch (e: Exception) {
                val errorMsg = "PCM playback failed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                audioTrack?.release()
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception(errorMsg))
                }
            }
        }
    }

    private fun getFileExtension(format: AudioFormat): String {
        return when (format) {
            AudioFormat.MP3 -> ".mp3"
            AudioFormat.WAV -> ".wav"
            AudioFormat.OGG -> ".ogg"
            AudioFormat.AAC -> ".aac"
            AudioFormat.OPUS -> ".opus"
            AudioFormat.PCM -> ".pcm"
        }
    }
} 