package com.eterultimate.eteruee.ui.pages.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.data.ai.GenerationHandler
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.datastore.SettingsStore
import java.util.Locale

private const val TAG = "TranslatorVM"

class TranslatorVM(
    private val settingsStore: SettingsStore,
    private val generationHandler: GenerationHandler,
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings.dummy())

    // 缈昏瘧鐘舵€?
    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating

    // 杈撳叆鏂囨湰
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    // 缈昏瘧缁撴灉
    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    // 缈昏瘧鐩爣璇█
    private val _targetLanguage = MutableStateFlow(Locale.SIMPLIFIED_CHINESE)
    val targetLanguage: StateFlow<Locale> = _targetLanguage

    // 閿欒娴?
    val errorFlow = MutableSharedFlow<Throwable>()

    // 褰撳墠浠诲姟
    private var currentJob: Job? = null

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun updateTargetLanguage(language: Locale) {
        _targetLanguage.value = language
    }

    fun translate() {
        val inputText = _inputText.value
        if (inputText.isBlank()) return

        // 鍙栨秷褰撳墠浠诲姟
        currentJob?.cancel()

        // 璁剧疆缈昏瘧涓姸鎬?
        _translating.value = true
        _translatedText.value = ""

        currentJob = viewModelScope.launch {
            runCatching {
                generationHandler.translateText(
                    settings = settings.value,
                    sourceText = inputText,
                    targetLanguage = targetLanguage.value
                ) { translatedText ->
                    // Update translation in real-time
                    _translatedText.value = translatedText
                }.collect { /* Final translation already handled in onStreamUpdate */ }
            }.onFailure {
                it.printStackTrace()
                errorFlow.emit(it)
            }

            _translating.value = false
        }
    }

    fun cancelTranslation() {
        currentJob?.cancel()
        _translating.value = false
    }
}

