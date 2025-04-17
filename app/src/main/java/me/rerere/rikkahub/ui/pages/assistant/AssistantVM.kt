package me.rerere.rikkahub.ui.pages.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant

class AssistantVM(private val settingsStore: SettingsStore) : ViewModel() {
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    fun addAssistant(assistant: Assistant) {
        viewModelScope.launch {
            val settings = settings.value
            settingsStore.update(
                settings.copy(
                    assistants = settings.assistants.plus(assistant)
                )
            )
        }
    }

    fun updateAssistant(assistant: Assistant) {
        viewModelScope.launch {
            val settings = settings.value
            settingsStore.update(
                settings.copy(
                    assistants = settings.assistants.map {
                        if (it.id == assistant.id) {
                            assistant
                        } else {
                            it
                        }
                    }
                )
            )
        }
    }

    fun removeAssistant(assistant: Assistant) {
        viewModelScope.launch {
            val settings = settings.value
            settingsStore.update(
                settings.copy(
                    assistants = settings.assistants.filter { it.id != assistant.id }
                )
            )
        }
    }
}