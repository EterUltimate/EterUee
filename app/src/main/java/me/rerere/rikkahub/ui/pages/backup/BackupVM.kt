package me.rerere.rikkahub.ui.pages.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.sync.DataSync

class BackupVM(
    private val settingsStore: SettingsStore,
    private val dataSync: DataSync,
) : ViewModel() {
    val settings = settingsStore.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings()
    )

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    suspend fun testWebDav() {
        dataSync.testWebdav(settings.value.webDavConfig)
    }

    suspend fun backup() {
        dataSync.backupToWebDav(settings.value.webDavConfig)
    }
}