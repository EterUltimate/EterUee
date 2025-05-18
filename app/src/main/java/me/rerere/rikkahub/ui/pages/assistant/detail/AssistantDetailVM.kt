package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import kotlin.uuid.Uuid

class AssistantDetailVM(
    savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val assistantId = Uuid.parse(checkNotNull(savedStateHandle["id"] as? String))

    val settings = settingsStore
        .settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Settings()
        )
    val assistant = settings
        .map {
            it.assistants.find { assistant ->
                assistant.id == assistantId
            } ?: Assistant(id = assistantId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Assistant(id = assistantId)
        )

    fun update(assistant: Assistant) {
        viewModelScope.launch {
            settingsStore.update(
                settings.value.copy(
                assistants = settings.value.assistants.map {
                    if (it.id == assistant.id) {
                        assistant
                    } else {
                        it
                    }
                }
            ))
        }
    }
}