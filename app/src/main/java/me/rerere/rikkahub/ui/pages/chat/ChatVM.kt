package me.rerere.rikkahub.ui.pages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.handleMessageChunk
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.repository.ConversationRepository
import java.time.Instant
import kotlin.uuid.Uuid

class ChatVM(
    savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
) : ViewModel() {
    private val _conversationId: Uuid = Uuid.parse(checkNotNull(savedStateHandle["id"]))
    private val _conversation = MutableStateFlow(Conversation.ofId(_conversationId))
    val conversation: StateFlow<Conversation>
        get() = _conversation

    // 异步任务
    val conversationJob = MutableStateFlow<Job?>(null)

    init {
        // Load the conversation from the repository (database)
        viewModelScope.launch {
            val conversation = conversationRepo.getConversationById(_conversationId)
            if (conversation != null) {
                this@ChatVM._conversation.value = conversation
            }
        }
    }

    // 用户设置
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    // 聊天列表
    val conversations = conversationRepo.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 更新设置
    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    // 设置聊天模型
    fun setChatModel(model: Model) {
        viewModelScope.launch {
            settingsStore.update(
                settings.value.copy(
                    chatModelId = model.id
                )
            )
        }
    }

    fun handleMessageSend(content: List<UIMessagePart>) {
        val model = settings.value.providers.findModelById(settings.value.chatModelId)
        val provider = model?.findProvider(settings.value.providers) ?: return
        val newConversation = conversation.value.copy(
            messages = conversation.value.messages + UIMessage(
                role = MessageRole.USER,
                parts = content,
            ),
            updateAt = Instant.now()
        )
        this.saveConversation(newConversation)
        val job = viewModelScope.launch {
            runCatching {
                val providerHandler = ProviderManager.getProviderByType(provider)
                providerHandler.streamText(
                    providerSetting = provider,
                    conversation = newConversation,
                    params = TextGenerationParams(
                        model = model,
                        temperature = 0.5f,
                        topP = 0.99f
                    )
                ).collect { chunk ->
                    val currConversation = conversation.value
                    updateConversation(
                        currConversation.copy(
                            messages = currConversation.messages.handleMessageChunk(chunk)
                        )
                    )
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
        this.conversationJob.value = job
        job.invokeOnCompletion {
            this.conversationJob.value = null
        }
    }

    fun updateConversation(conversation: Conversation) {
        this._conversation.value = conversation
    }

    fun saveConversation(conversation: Conversation) {
        this.updateConversation(conversation)
        viewModelScope.launch {
            try {
                if (conversationRepo.getConversationById(conversation.id) == null) {
                    conversationRepo.insertConversation(conversation)
                } else {
                    conversationRepo.updateConversation(conversation)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}