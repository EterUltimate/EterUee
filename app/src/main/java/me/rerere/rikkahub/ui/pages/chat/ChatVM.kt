package me.rerere.rikkahub.ui.pages.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
import me.rerere.ai.ui.isEmptyMessage
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.utils.deleteChatFiles
import java.time.Instant
import java.util.Locale
import kotlin.uuid.Uuid

private const val TAG = "ChatVM"

class ChatVM(
    savedStateHandle: SavedStateHandle,
    private val context: Application,
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

    // 当前模型
    val currentChatModel = settings
        .map {
            it.providers.findModelById(it.chatModelId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

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
        if(content.isEmptyMessage()) return
        val newConversation = conversation.value.copy(
            messages = conversation.value.messages + UIMessage(
                role = MessageRole.USER,
                parts = content,
            ),
            updateAt = Instant.now()
        )
        this.saveConversation(newConversation)
        this.handleMessageComplete()
    }

    fun handleMessageEdit(parts: List<UIMessagePart>, uuid: Uuid?) {
        if(parts.isEmptyMessage()) return
        val newConversation = conversation.value.copy(
            messages = conversation.value.messages.map {
                if(it.id == uuid) {
                    it.copy(
                        parts = parts,
                    )
                } else {
                    it
                }
            },
            updateAt = Instant.now()
        )
        this.updateConversation(newConversation)
        val message = newConversation.messages.find { it.id == uuid } ?: return
        this.regenerateAtMessage(message)
    }

    private fun handleMessageComplete() {
        val model = settings.value.providers.findModelById(settings.value.chatModelId)
        val provider = model?.findProvider(settings.value.providers) ?: return
        val job = viewModelScope.launch {
            runCatching {
                val providerHandler = ProviderManager.getProviderByType(provider)
                providerHandler.streamText(
                    providerSetting = provider,
                    conversation = conversation.value,
                    params = TextGenerationParams(
                        model = model,
                        temperature = 0.5f,
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
            }.onSuccess {
                saveConversation(conversation.value)
                generateTitle()
            }
        }
        this.conversationJob.value = job
        job.invokeOnCompletion {
            this.conversationJob.value = null
        }
    }

    fun generateTitle(force: Boolean = false) {
        if(conversation.value.title.isNotBlank() && !force) return

        val model = settings.value.providers.findModelById(settings.value.titleModelId) ?: let {
            // 如果没有标题模型，则使用聊天模型
            settings.value.providers.findModelById(settings.value.chatModelId)
        } ?: return
        val provider = model.findProvider(settings.value.providers) ?: return

        viewModelScope.launch {
            runCatching {
                val providerHandler = ProviderManager.getProviderByType(provider)
                val result = providerHandler.generateText(
                    providerSetting = provider,
                    conversation = Conversation.ofUser("""
                        你是一名擅长会话的助理，我会给你一些对话内容在content内，你需要将用户的会话总结为 10 个字以内的标题
                        1. 标题语言与用户的首要语言一致
                        2. 不要使用标点符号和其他特殊符号
                        3. 直接回复标题即可
                        4. 使用 ${Locale.getDefault().displayName} 语言总结
                        
                        <content>
                        ${conversation.value.messages.joinToString("\n\n") { it.summaryAsText()}}
                        </content>
                    """.trimIndent()),
                    params = TextGenerationParams(
                        model = model,
                        temperature = 0.8f,
                    ),
                )
                val currConversation = conversation.value
                saveConversation(
                    currConversation.copy(
                        title = result.choices[0].message?.text() ?: "",
                        updateAt = Instant.now()
                    )
                )
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun regenerateAtMessage(message: UIMessage) {
        if(message.role == MessageRole.USER) {
            // 如果是用户消息，则截止到当前消息
            val indexAt = conversation.value.messages.indexOf(message)
            val newConversation = conversation.value.copy(
                messages = conversation.value.messages.subList(0, indexAt + 1)
            )
            this.saveConversation(newConversation)
        } else {
            // 如果是助手消息，则需要向上查找第一个用户消息
            var indexAt = conversation.value.messages.indexOf(message)
            for (i in indexAt downTo 0) {
                if (conversation.value.messages[i].role == MessageRole.USER) {
                    indexAt = i
                    break
                }
            }
            val newConversation = conversation.value.copy(
                messages = conversation.value.messages.subList(0, indexAt + 1)
            )
            this.saveConversation(newConversation)
        }
        this.handleMessageComplete()
    }

    fun updateConversation(conversation: Conversation) {
        checkFilesDelete(conversation, this._conversation.value)
        this._conversation.value = conversation
    }

    // 变更消息，检查文件删除
    private fun checkFilesDelete(newConversation: Conversation, oldConversation: Conversation) {
        val newFiles = newConversation.files
        val oldFiles = oldConversation.files
        val deletedFiles = oldFiles.filter { file ->
            newFiles.none { it == file }
        }
        if (deletedFiles.isNotEmpty()) {
            context.deleteChatFiles(deletedFiles)
            Log.w(TAG, "checkFilesDelete: $deletedFiles")
        }
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

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(conversation)
        }
    }
}