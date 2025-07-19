package me.rerere.rikkahub.ui.hooks.tts

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getSelectedTTSProvider
import me.rerere.rikkahub.utils.playPcmSound
import me.rerere.rikkahub.utils.playSound
import me.rerere.tts.model.AudioFormat
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.provider.TTSManager
import me.rerere.tts.provider.TTSProviderSetting
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

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
  DisposableEffect(settings.selectedTTSProviderId) {
    ttsState.updateProvider(settings.getSelectedTTSProvider())
    onDispose { }
  }

  // Use DisposableEffect to ensure resources are cleaned up when the composable leaves composition
  DisposableEffect(ttsState) {
    onDispose {
      Log.d("rememberCustomTtsState", "Disposing Custom TTS State")
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

  /**
   * Speaks the given text using the selected TTS provider.
   */
  fun speak(text: String)

  /** Stops the current speech. */
  fun stop()

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

  private val ttsManager = get<TTSManager>()
  private val scope = CoroutineScope(Dispatchers.Main)
  private var currentJob: Job? = null
  
  private var currentProvider: TTSProviderSetting? = null

  private val _isAvailable = MutableStateFlow(false)
  override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  override val error: StateFlow<String?> = _error.asStateFlow()

  fun updateProvider(provider: TTSProviderSetting?) {
    currentProvider = provider
    _isAvailable.update { provider != null && provider.enabled }
    _error.update { null }
    
    if (provider == null) {
      stop()
    }
  }

  override fun speak(text: String) {
    val provider = currentProvider
    if (provider == null || !provider.enabled) {
      _error.update { "No TTS provider selected or provider is disabled" }
      return
    }

    if (_isSpeaking.value) {
      stop()
    }

    _isSpeaking.update { true }
    _error.update { null }

    currentJob = scope.launch {
      try {
        Log.d("CustomTtsState", "Starting TTS with provider: ${provider.name}")
        
        val request = TTSRequest(text = text)
        val response = withContext(Dispatchers.IO) {
          ttsManager.generateSpeech(provider, request)
        }
        
        // Play the audio based on format
        when (response.format) {
          AudioFormat.PCM -> {
            val sampleRate = response.sampleRate ?: 24000
            context.playPcmSound(response.audioData, sampleRate)
          }
          else -> {
            context.playSound(response.audioData, response.format)
          }
        }
        
        Log.d("CustomTtsState", "TTS playback completed")
      } catch (e: Exception) {
        Log.e("CustomTtsState", "TTS error", e)
        _error.update { "TTS error: ${e.message}" }
      } finally {
        _isSpeaking.update { false }
      }
    }
  }

  override fun stop() {
    currentJob?.cancel()
    _isSpeaking.update { false }
    Log.d("CustomTtsState", "TTS stopped")
  }

  override fun cleanup() {
    stop()
    currentJob = null
  }
} 