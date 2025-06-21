package me.rerere.rikkahub.ui.pages.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.sync.BackupFileItem
import me.rerere.rikkahub.data.sync.DataSync
import me.rerere.rikkahub.utils.UiState

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

    fun getWebDavBackupFiles(): Flow<UiState<List<BackupFileItem>>> = flow {
        emit(UiState.Loading)
        emit(
            UiState.Success(
                dataSync.listBackupFiles(
                    settings.value.webDavConfig
                )
            )
        )
    }.catch {
        emit(UiState.Error(it))
    }

    suspend fun testWebDav() {
        dataSync.testWebdav(settings.value.webDavConfig)
    }

    suspend fun backup() {
        dataSync.backupToWebDav(settings.value.webDavConfig)
    }

    suspend fun restore(item: BackupFileItem) {
        dataSync.restoreFromWebDav(webDavConfig = settings.value.webDavConfig, item = item)
    }
}