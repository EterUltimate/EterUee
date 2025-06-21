package me.rerere.rikkahub.ui.pages.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.sync.BackupFileItem
import me.rerere.rikkahub.data.sync.DataSync
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.UiState
import java.io.File

class BackupVM(
  private val settingsStore: SettingsStore,
  private val dataSync: DataSync,
) : ViewModel() {
  val settings = settingsStore.settingsFlow.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    Settings()
  )

  val backupFileItems = MutableStateFlow<UiState<List<BackupFileItem>>>(UiState.Idle)

  init {
    loadBackupFileItems()
  }

  fun updateSettings(settings: Settings) {
    viewModelScope.launch {
      settingsStore.update(settings)
    }
  }

  fun loadBackupFileItems() {
    viewModelScope.launch {
      runCatching {
        backupFileItems.emit(UiState.Loading)
        backupFileItems.emit(
          value = UiState.Success(
            data = dataSync.listBackupFiles(
              webDavConfig = settings.value.webDavConfig
            ).sortedByDescending { it.lastModified }
          )
        )
      }.onFailure {
        backupFileItems.emit(UiState.Error(it))
      }
    }
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

  suspend fun deleteWebDavBackupFile(item: BackupFileItem) {
    dataSync.deleteWebDavBackupFile(settings.value.webDavConfig, item)
  }

  suspend fun exportToFile(): File {
    return dataSync.prepareBackupFile(settings.value.webDavConfig.copy())
  }

  suspend fun restoreFromLocalFile(file: File) {
    dataSync.restoreFromLocalFile(file, settings.value.webDavConfig)
  }

  fun restoreFromChatBox(file: File) {
    val jsonElements = JsonInstant.parseToJsonElement(file.readText()).jsonObject
    val settings = jsonElements["settings"]?.jsonObject
    if(settings != null) {
      println(settings)
      settings["providers"]?.jsonObject?.let { providers ->
        providers["openai"]?.jsonObject?.let { openai ->

        }
        val claude = providers["claude"]?.jsonObject
        val gemini = providers["gemini"]?.jsonObject

      }
    }
  }
}