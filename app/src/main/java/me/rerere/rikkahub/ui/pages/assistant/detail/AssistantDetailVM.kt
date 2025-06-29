package me.rerere.rikkahub.ui.pages.assistant.detail

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.Tag
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.utils.deleteChatFiles
import kotlin.uuid.Uuid

private const val TAG = "AssistantDetailVM"

class AssistantDetailVM(
  private val settingsStore: SettingsStore,
  private val memoryRepository: MemoryRepository,
  private val context: Application,
  savedStateHandle: SavedStateHandle
) : ViewModel() {
  private val assistantId = Uuid.parse(checkNotNull(savedStateHandle.get<String>("id")))

  val settings: StateFlow<Settings> =
    settingsStore.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

  val mcpServerConfigs = settingsStore
    .settingsFlow.map { settings ->
      settings.mcpServers
    }.stateIn(
      scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
    )

  val assistant: StateFlow<Assistant> = settingsStore
    .settingsFlow
    .map { settings ->
      settings.assistants.find { it.id == assistantId } ?: Assistant()
    }.stateIn(
      scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = Assistant()
    )

  val memories = memoryRepository.getMemoriesOfAssistantFlow(assistantId.toString())
    .stateIn(
      scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
    )

  val providers = settingsStore
    .settingsFlow
    .map { settings ->
      settings.providers
    }.stateIn(
      scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
    )

  val tags = settingsStore
    .settingsFlow
    .map { settings ->
      settings.assistantTags
    }.stateIn(
      scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
    )

  fun updateTags(tags: List<Tag>) {
    viewModelScope.launch {
      val settings = settings.value
      settingsStore.update(
        settings = settings.copy(
          assistantTags = tags
        )
      )
    }
  }

  fun cleanupUnusedTags() {
    viewModelScope.launch {
      val settings = settings.value
      val usedTagIds = settings.assistants.flatMap { it.tags }.toSet()
      val cleanedTags = settings.assistantTags.filter { tag ->
        usedTagIds.contains(tag.id)
      }
      if (cleanedTags.size != settings.assistantTags.size) {
        settingsStore.update(
          settings = settings.copy(
            assistantTags = cleanedTags
          )
        )
      }
    }
  }

  fun update(assistant: Assistant) {
    viewModelScope.launch {
      val settings = settings.value
      settingsStore.update(
        settings = settings.copy(
          assistants = settings.assistants.map {
            if (it.id == assistant.id) {
              checkAvatarDelete(old = it, new = assistant) // 删除旧头像
              assistant
            } else {
              it
            }
          })
      )
      // 自动清理无用的tags
      cleanupUnusedTags()
    }
  }

  fun addMemory(memory: AssistantMemory) {
    viewModelScope.launch {
      memoryRepository.addMemory(
        assistantId = assistantId.toString(),
        content = memory.content
      )
    }
  }

  fun updateMemory(memory: AssistantMemory) {
    viewModelScope.launch {
      memoryRepository.updateContent(id = memory.id, content = memory.content)
    }
  }

  fun deleteMemory(memory: AssistantMemory) {
    viewModelScope.launch {
      memoryRepository.deleteMemory(id = memory.id)
    }
  }

  fun checkAvatarDelete(old: Assistant, new: Assistant) {
    if (old.avatar is Avatar.Image && old.avatar != new.avatar) {
      context.deleteChatFiles(listOf(old.avatar.url.toUri()))
    }
  }
}