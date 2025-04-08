package me.rerere.rikkahub.ui.pages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.Conversation
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.repository.ConversationRepository
import kotlin.uuid.Uuid

class ChatVM(
    savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
) : ViewModel() {
    init {
        println("handle = ${savedStateHandle.keys()}")
    }

    // 用户设置
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    // 聊天列表
    val conversations = conversationRepo.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    fun setChatModel(model: Model) {
        viewModelScope.launch {
            settingsStore.update(
                settings.value.copy(
                    chatModelId = model.id
                )
            )
        }
    }

    fun getConversationById(id: Uuid) = conversationRepo.getConversationById(id)

    fun updateConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepo.updateConversation(conversation)
        }
    }
}

data class CompletionTask(
    val job: Job,
)