package me.rerere.rikkahub.ui.hooks

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getSelectedTTSProvider
import me.rerere.rikkahub.utils.stripMarkdown
import me.rerere.tts.model.TTSResponse
import me.rerere.tts.provider.TTSManager
import me.rerere.tts.provider.TTSProviderSetting
import me.rerere.tts.controller.TtsController
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "TTS"

/**
 * Composable function to remember and manage custom TTS state.
 * Uses user-configured TTS providers instead of system TTS.
 */
@Composable
fun rememberCustomTtsState(): CustomTtsState {
    val context = LocalContext.current
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()

    // Remember the CustomTtsState instance across recompositions
    val ttsState = remember {
        CustomTtsStateImpl(
            context = context.applicationContext,
            settingsStore = settingsStore
        )
    }

    // Update the provider when settings change
    DisposableEffect(settings.selectedTTSProviderId, settings.ttsProviders) {
        ttsState.updateProvider(settings.getSelectedTTSProvider())
        onDispose { }
    }

    // Cleanup resources when the state is disposed
    DisposableEffect(ttsState) {
        onDispose {
            ttsState.cleanup()
        }
    }

    return ttsState
}

/**
 * Interface defining the public API of our custom TTS state holder.
 */
interface CustomTtsState {
    /** Flow indicating if the TTS provider is available and ready. */
    val isAvailable: StateFlow<Boolean>

    /** Flow indicating if the TTS is currently speaking. */
    val isSpeaking: StateFlow<Boolean>

    /** Flow holding any error message. */
    val error: StateFlow<String?>

    /** Flow indicating current chunk being processed (index) */
    val currentChunk: StateFlow<Int>

    /** Flow indicating total chunks in queue */
    val totalChunks: StateFlow<Int>

    /**
     * Speaks the given text using the selected TTS provider.
     * Long texts will be automatically chunked and queued.
     */
    fun speak(text: String, flushCalled: Boolean = true)

    /** Stops the current speech and clears the queue. */
    fun stop()

    /** Pauses the current playback. */
    fun pause()

    /** Resumes the paused playback. */
    fun resume()

    /** Skips to the next chunk in the queue. */
    fun skipNext()

    /** Cleanup resources. */
    fun cleanup()
}

/**
 * Internal implementation of CustomTtsState.
 */
private class CustomTtsStateImpl(
    private val context: Context,
    private val settingsStore: SettingsStore
) : CustomTtsState, KoinComponent {

    private val ttsManager by inject<TTSManager>()
    private val controller by lazy { me.rerere.tts.controller.TtsController(context, ttsManager) }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentJob: Job? = null

    override val isAvailable: StateFlow<Boolean> get() = controller.isAvailable
    override val isSpeaking: StateFlow<Boolean> get() = controller.isSpeaking
    override val error: StateFlow<String?> get() = controller.error
    override val currentChunk: StateFlow<Int> get() = controller.currentChunk
    override val totalChunks: StateFlow<Int> get() = controller.totalChunks

    fun updateProvider(provider: TTSProviderSetting?) {
        controller.setProvider(provider)
    }

//    private fun chunkText(text: String): List<String> {
//        if (text.isBlank()) {
//            return emptyList()
//        }
//
//        // 1. 按段落分割
//        val paragraphs = text.split("\n\n")
//
//        // 正则表达式会在标点符号后分割，并保留标点
//        val punctuationRegex = "(?<=[。！？，、：;.!?:,\n])".toRegex()
//
//        // 2. 对每个段落进行处理，然后将结果合并
//        return paragraphs.flatMap { paragraph ->
//            if (paragraph.isBlank()) {
//                emptyList()
//            } else {
//                paragraph.stripMarkdown()
//                    .split(punctuationRegex)
//                    .asSequence()
//                    .map { it.trim() }
//                    .filter { it.isNotEmpty() }
//                    .fold<String, MutableList<StringBuilder>>(mutableListOf()) { acc, chunk ->
//                        if (acc.isEmpty() || acc.last().length + chunk.length > maxChunkLength) {
//                            acc.add(StringBuilder(chunk))
//                        } else {
//                            acc.last().append(chunk)
//                        }
//                        acc
//                    }
//                    .map { it.toString() }
//            }
//        }
//    }

    override fun speak(text: String, flushCalled: Boolean) {
        val processed = text.stripMarkdown()
        controller.speak(processed, flushCalled)
    }

    // 预合成逻辑由 tts 模块接管

    // 预合成触发由 tts 模块接管

    // 队列处理由 tts 模块接管

    // 队列处理由 tts 模块接管

    override fun stop() {
        controller.stop()
    }

    override fun pause() {
        controller.pause()
        Log.d("CustomTtsState", "TTS paused")
    }

    override fun resume() {
        controller.resume()
        Log.d("CustomTtsState", "TTS resumed")
    }

    override fun skipNext() {
        controller.skipNext()
    }

    override fun cleanup() {
        controller.dispose()
        currentJob = null
    }
}
