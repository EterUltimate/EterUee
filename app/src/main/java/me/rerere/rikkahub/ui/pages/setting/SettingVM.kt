package me.rerere.rikkahub.ui.pages.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore

class SettingVM(private val settingsStore: SettingsStore) : ViewModel() {
    val settings = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())
    val theme = settings.map { it.theme }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsStore.update(
                settings.value.copy(
                    theme = theme,
                )
            )
        }
    }
}