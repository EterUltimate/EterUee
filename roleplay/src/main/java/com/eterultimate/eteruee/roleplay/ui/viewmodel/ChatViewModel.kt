package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.sdk.streamTextWithSubagent
import com.eterultimate.eteruee.ai.subagent.SubagentTextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.domain.subagent.RoleplaySubagentExecutor
import com.eterultimate.eteruee.roleplay.domain.service.ChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 聊天页 ViewModel
 */
@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(
    private val chatService: ChatService,
    private val aiSDK: AISDK,
    private val tokenService: com.eterultimate.eteruee.roleplay.domain.service.TokenService,
    private val bookmarkService: com.eterultimate.eteruee.roleplay.domain.service.BookmarkService,
    private val subagentExecutor: RoleplaySubagentExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 当前生成任务的 Job，用于取消
    private var currentGenerationJob: Job? = null
    private var enableSubagent: Boolean = false

    // 书签列表
    private val _bookmarks = MutableStateFlow<List<com.eterultimate.eteruee.roleplay.data.model.Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<com.eterultimate.eteruee.roleplay.data.model.Bookmark>> = _bookmarks.asStateFlow()

    /**
     * 初始化聊天
     */
    fun initialize(chatId: Uuid) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val chat = chatService.getChatById(chatId)
            if (chat != null) {
                // 加载消息列表
                val messageNodes = chatService.loadMessages(chatId, offset = 0, limit = 50)
                // 从 MessageNode 中提取当前选中的 ChatMessage
                val messages = messageNodes.mapNotNull { it.getCurrentMessage() }.reversed()

                // 计算总 Token 数
                val totalTokens = tokenService.calculateTotalTokens(
                    messages.map { it.content }
                )

                _uiState.value = _uiState.value.copy(
                    chat = chat,
                    messages = messages, // 最新消息在前
                    isLoading = false,
                    totalTokens = totalTokens
                )

                // 加载书签列表
                loadBookmarks(chatId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载聊天失败"
                )
            }
        }
    }

    /**
     * 发送用户消息
     */
    fun sendMessage(content: String) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            // 添加用户消息
            val result = chatService.appendUserMessage(chat.chatId, content)
            result.onSuccess { userMessage ->
                // 更新UI
                val newMessages = listOf(userMessage) + _uiState.value.messages
                _uiState.value = _uiState.value.copy(messages = newMessages)

                // 调用AI生成回复
                generateAIResponse(chat.chatId, newMessages)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "发送消息失败: ${error.message}"
                )
            }
        }
    }

    /**
     * AI流式生成回复
     */
    private fun generateAIResponse(chatId: Uuid, messages: List<ChatMessage>) {
        // 取消之前的生成任务
        currentGenerationJob?.cancel()

        currentGenerationJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isGenerating = true)

                // 转换为UIMessage格式
                val uiMessages = messages.map { message ->
                    UIMessage(
                        role = when (message.role) {
                            com.eterultimate.eteruee.ai.core.MessageRole.USER -> MessageRole.USER
                            com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT -> MessageRole.ASSISTANT
                            com.eterultimate.eteruee.ai.core.MessageRole.SYSTEM -> MessageRole.SYSTEM
                            com.eterultimate.eteruee.ai.core.MessageRole.TOOL -> MessageRole.TOOL
                        },
                        parts = listOf(UIMessagePart.Text(message.content)),
                        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                }

                // TODO: 从角色配置获取模型和参数
                val model = com.eterultimate.eteruee.ai.provider.Model(
                    modelId = "gpt-3.5-turbo",
                    displayName = "GPT-3.5 Turbo"
                )

                val request = StreamTextRequest(
                    model = model,
                    messages = uiMessages,
                    temperature = 0.7f,
                    maxTokens = 2048
                )

                var assistantMessageContent = ""

                if (enableSubagent) {
                    aiSDK.streamTextWithSubagent(
                        request = request,
                        toolExecutor = subagentExecutor.createToolExecutor()
                    ).collect { chunk ->
                        if (!isActive) return@collect

                        when (chunk) {
                            is SubagentTextChunk.PlanGenerating -> {
                                _uiState.value = _uiState.value.copy(subagentStatus = chunk.status)
                            }

                            is SubagentTextChunk.PlanGenerated -> {
                                _uiState.value = _uiState.value.copy(subagentStatus = chunk.plan.planText)
                            }

                            is SubagentTextChunk.Status -> {
                                _uiState.value = _uiState.value.copy(subagentStatus = chunk.status)
                            }

                            is SubagentTextChunk.StepExecuting -> {
                                _uiState.value = _uiState.value.copy(subagentStatus = "执行工具: ${chunk.toolName}")
                            }

                            is SubagentTextChunk.StepCompleted -> {
                                _uiState.value = _uiState.value.copy(
                                    subagentStatus = if (chunk.result.isError) {
                                        "工具失败: ${chunk.result.stepId}"
                                    } else {
                                        "工具完成: ${chunk.result.stepId}"
                                    }
                                )
                            }

                            is SubagentTextChunk.TextDelta -> {
                                assistantMessageContent += chunk.text
                                updateStreamingMessage(assistantMessageContent)
                            }

                            is SubagentTextChunk.Finish -> {
                                saveAssistantMessage(chatId, assistantMessageContent)
                                _uiState.value = _uiState.value.copy(
                                    isGenerating = false,
                                    subagentStatus = null
                                )
                                currentGenerationJob = null
                            }

                            is SubagentTextChunk.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isGenerating = false,
                                    subagentStatus = null,
                                    errorMessage = "Subagent 执行失败: ${chunk.error}"
                                )
                                currentGenerationJob = null
                            }

                            else -> {}
                        }
                    }
                } else {
                    aiSDK.streamText(request).collect { chunk ->
                        if (!isActive) return@collect

                        when (chunk) {
                            is TextChunk.TextDelta -> {
                                assistantMessageContent += chunk.text
                                updateStreamingMessage(assistantMessageContent)
                            }

                            is TextChunk.Finish -> {
                                saveAssistantMessage(chatId, assistantMessageContent)
                                _uiState.value = _uiState.value.copy(isGenerating = false)
                                currentGenerationJob = null
                            }

                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                // 如果是取消异常，不显示错误
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        subagentStatus = null,
                        errorMessage = "AI生成失败: ${e.message}"
                    )
                }
                currentGenerationJob = null
            }
        }
    }

    /**
     * 更新流式消息显示
     */
    private fun updateStreamingMessage(content: String) {
        val currentMessages = _uiState.value.messages.toMutableList()

        // 计算当前消息的 token 数
        val currentTokens = tokenService.estimateTokens(content)

        // 检查是否已有临时的助手消息
        if (currentMessages.isNotEmpty() &&
            currentMessages[0].role == com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT &&
            currentMessages[0].isStreaming) {
            // 更新现有消息
            currentMessages[0] = currentMessages[0].copy(content = content)
        } else {
            // 创建新的临时消息
            val streamingMessage = ChatMessage(
                id = Uuid.random(),
                role = com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT,
                content = content,
                timestamp = Instant.now(),
                isStreaming = true
            )
            currentMessages.add(0, streamingMessage)
        }

        _uiState.value = _uiState.value.copy(
            messages = currentMessages,
            currentMessageTokens = currentTokens
        )
    }

    /**
     * 保存助手消息到文件
     */
    private suspend fun saveAssistantMessage(chatId: Uuid, content: String) {
        chatService.appendAssistantMessage(chatId, content)
            .onSuccess { message ->
                // 替换临时消息为正式消息
                val currentMessages = _uiState.value.messages.toMutableList()
                if (currentMessages.isNotEmpty() &&
                    currentMessages[0].isStreaming) {
                    currentMessages[0] = message

                    // 重新计算总 token 数
                    val totalTokens = tokenService.calculateTotalTokens(
                        currentMessages.map { it.content }
                    )

                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages,
                        totalTokens = totalTokens,
                        currentMessageTokens = 0
                    )
                }
            }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(messageId: Uuid) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = chatService.deleteMessageById(chat.chatId, messageId)
            result.onSuccess {
                val newMessages = _uiState.value.messages.filter { it.id != messageId }
                _uiState.value = _uiState.value.copy(messages = newMessages)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除消息失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 停止当前生成
     */
    fun stopGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _uiState.value = _uiState.value.copy(isGenerating = false, subagentStatus = null)
    }

    fun toggleSubagent() {
        enableSubagent = !enableSubagent
        _uiState.value = _uiState.value.copy(enableSubagent = enableSubagent)
    }

    /**
     * 清空所有消息
     */
    fun clearAllMessages() {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = chatService.clearAllMessages(chat.chatId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(messages = emptyList())
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "清空消息失败: ${error.message}"
                )
            }
        }
    }

    // ==================== 分支管理 ====================

    /**
     * 创建新分支
     */
    fun createBranch(fromMessageIndex: Int) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = chatService.createBranch(chat.chatId, fromMessageIndex)
            result.onSuccess { branchId ->
                // 重新加载分支列表和消息
                loadBranches(chat.chatId)
                // 重新加载消息以显示新分支的内容
                val messageNodes = chatService.loadMessages(chat.chatId, offset = 0, limit = 50)
                val messages = messageNodes.mapNotNull { it.getCurrentMessage() }.reversed()

                val totalTokens = tokenService.calculateTotalTokens(
                    messages.map { it.content }
                )

                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    totalTokens = totalTokens,
                    activeBranchId = branchId  // 更新当前激活的分支
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "创建分支失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 切换分支
     */
    fun switchBranch(nodeId: kotlin.uuid.Uuid) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = chatService.switchToBranch(chat.chatId, nodeId)
            result.onSuccess {
                // 更新激活的分支ID
                _uiState.value = _uiState.value.copy(activeBranchId = nodeId)
                // 重新加载该分支的消息
                val messageNodes = chatService.loadMessages(chat.chatId, offset = 0, limit = 50)
                val messages = messageNodes.mapNotNull { it.getCurrentMessage() }.reversed()

                val totalTokens = tokenService.calculateTotalTokens(
                    messages.map { it.content }
                )

                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    totalTokens = totalTokens
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "切换分支失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 删除分支
     */
    fun deleteBranch(nodeId: kotlin.uuid.Uuid) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = chatService.deleteBranch(chat.chatId, nodeId)
            result.onSuccess {
                // 重新加载分支列表
                loadBranches(chat.chatId)
                // 如果删除的是当前分支，需要重新加载消息
                if (_uiState.value.activeBranchId == nodeId) {
                    val messageNodes = chatService.loadMessages(chat.chatId, offset = 0, limit = 50)
                    val messages = messageNodes.mapNotNull { it.getCurrentMessage() }.reversed()

                    val totalTokens = tokenService.calculateTotalTokens(
                        messages.map { it.content }
                    )

                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        totalTokens = totalTokens,
                        activeBranchId = chatService.getChatById(chat.chatId)?.activeBranchId
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除分支失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 加载分支列表
     */
    private suspend fun loadBranches(chatId: kotlin.uuid.Uuid) {
        val branches = chatService.getBranches(chatId)
        _uiState.value = _uiState.value.copy(branches = branches)
    }

    // ==================== 消息编辑 ====================

    /**
     * 开始编辑消息
     */
    fun startEditMessage(messageId: kotlin.uuid.Uuid, currentContent: String) {
        _uiState.value = _uiState.value.copy(
            editingMessageId = messageId,
            editContent = currentContent
        )
    }

    /**
     * 保存编辑后的消息
     */
    fun saveEditedMessage() {
        val chat = _uiState.value.chat ?: return
        val messageId = _uiState.value.editingMessageId ?: return
        val newContent = _uiState.value.editContent

        viewModelScope.launch {
            val result = chatService.editMessageContent(chat.chatId, messageId, newContent)
            result.onSuccess {
                // 更新UI中的消息
                val updatedMessages = _uiState.value.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(content = newContent, timestamp = java.time.Instant.now())
                    } else {
                        msg
                    }
                }
                _uiState.value = _uiState.value.copy(
                    messages = updatedMessages,
                    editingMessageId = null,
                    editContent = ""
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "编辑消息失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 取消编辑
     */
    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            editingMessageId = null,
            editContent = ""
        )
    }

    /**
     * 更新编辑内容
     */
    fun updateEditContent(content: String) {
        _uiState.value = _uiState.value.copy(editContent = content)
    }

    // ==================== 重新生成 ====================

    /**
     * 重新生成消息
     */
    fun regenerateMessage(messageId: kotlin.uuid.Uuid) {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRegenerating = true)

            // TODO: 从角色配置获取模型和参数
            val result = chatService.regenerateMessage(
                chatId = chat.chatId,
                messageId = messageId,
                providerId = "openai",
                modelId = "gpt-3.5-turbo",
                systemPrompt = "You are a helpful assistant.",
                temperature = 0.7f,
                maxTokens = 2048
            )

            result.collect { event ->
                when (event) {
                    is com.eterultimate.eteruee.roleplay.data.model.ChatGenerationEvent.Streaming -> {
                        // TODO: 更新流式消息
                    }
                    is com.eterultimate.eteruee.roleplay.data.model.ChatGenerationEvent.Complete -> {
                        _uiState.value = _uiState.value.copy(isRegenerating = false)
                        // TODO: 替换原消息
                    }
                    is com.eterultimate.eteruee.roleplay.data.model.ChatGenerationEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRegenerating = false,
                            errorMessage = "重新生成失败: ${event.error.message}"
                        )
                    }
                }
            }
        }
    }

    // ==================== 书签管理 ====================

    /**
     * 加载书签列表
     */
    private fun loadBookmarks(chatId: Uuid) {
        viewModelScope.launch {
            bookmarkService.getBookmarksByChat(chatId).collect { bookmarks ->
                _bookmarks.value = bookmarks
            }
        }
    }

    /**
     * 添加书签
     */
    fun addBookmark(messageIndex: Int, title: String = "", note: String = "") {
        val chat = _uiState.value.chat ?: return

        viewModelScope.launch {
            val result = bookmarkService.addBookmark(
                chatId = chat.chatId,
                messageIndex = messageIndex,
                title = title,
                note = note
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "添加书签失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 删除书签
     */
    fun deleteBookmark(bookmarkId: Uuid) {
        viewModelScope.launch {
            val result = bookmarkService.deleteBookmark(bookmarkId)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除书签失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 更新书签
     */
    fun updateBookmark(bookmarkId: Uuid, title: String, note: String) {
        viewModelScope.launch {
            val result = bookmarkService.updateBookmark(
                bookmarkId = bookmarkId,
                title = title,
                note = note
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "更新书签失败: ${error.message}"
                )
            }
        }
    }

    /**
     * 获取书签对应的消息索引（用于跳转）
     */
    fun getMessageIndexByBookmark(bookmark: com.eterultimate.eteruee.roleplay.data.model.Bookmark): Int? {
        val messages = _uiState.value.messages
        // 消息是反转的（最新消息在前），所以需要转换索引
        return if (bookmark.messageIndex >= 0 && bookmark.messageIndex < messages.size) {
            bookmark.messageIndex
        } else {
            null
        }
    }
}

/**
 * UI 状态
 */
data class ChatUiState(
    val chat: com.eterultimate.eteruee.roleplay.data.model.ChatMetadata? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    // 分支管理
    val branches: List<com.eterultimate.eteruee.roleplay.data.model.MessageNode> = emptyList(),
    val activeBranchId: kotlin.uuid.Uuid? = null,
    // 消息编辑
    val editingMessageId: kotlin.uuid.Uuid? = null,
    val editContent: String = "",
    // 重新生成
    val isRegenerating: Boolean = false,
    // Token 统计
    val totalTokens: Int = 0,
    val currentMessageTokens: Int = 0,
    val enableSubagent: Boolean = false,
    val subagentStatus: String? = null
)
