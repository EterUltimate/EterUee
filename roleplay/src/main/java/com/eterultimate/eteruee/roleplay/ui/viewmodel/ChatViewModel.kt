package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.domain.service.ChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val aiSDK: AISDK
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
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
                _uiState.value = _uiState.value.copy(
                    chat = chat,
                    messages = messages, // 最新消息在前
                    isLoading = false
                )
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
        viewModelScope.launch {
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
                
                // 流式接收AI响应
                var assistantMessageContent = ""
                aiSDK.streamText(request).collect { chunk ->
                    when (chunk) {
                        is TextChunk.TextDelta -> {
                            assistantMessageContent += chunk.text
                            // 实时更新UI
                            updateStreamingMessage(assistantMessageContent)
                        }
                        is TextChunk.Finish -> {
                            // 完成,保存消息
                            saveAssistantMessage(chatId, assistantMessageContent)
                            _uiState.value = _uiState.value.copy(isGenerating = false)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "AI生成失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 更新流式消息显示
     */
    private fun updateStreamingMessage(content: String) {
        val currentMessages = _uiState.value.messages.toMutableList()
        
        // 检查是否已有临时的助手消息
        if (currentMessages.isNotEmpty() && 
            currentMessages[0].role == com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT &&
            currentMessages[0].id.toString().startsWith("streaming_")) {
            // 更新现有消息
            currentMessages[0] = currentMessages[0].copy(content = content)
        } else {
            // 创建新的临时消息
            val streamingMessage = ChatMessage(
                id = Uuid.parse("streaming_${System.currentTimeMillis()}"),
                role = com.eterultimate.eteruee.ai.core.MessageRole.ASSISTANT,
                content = content,
                timestamp = Instant.now()
            )
            currentMessages.add(0, streamingMessage)
        }
        
        _uiState.value = _uiState.value.copy(messages = currentMessages)
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
                    currentMessages[0].id.toString().startsWith("streaming_")) {
                    currentMessages[0] = message
                    _uiState.value = _uiState.value.copy(messages = currentMessages)
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
                // 重新加载分支列表
                loadBranches(chat.chatId)
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
                _uiState.value = _uiState.value.copy(activeBranchId = nodeId)
                // TODO: 加载该分支的消息
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
                loadBranches(chat.chatId)
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
    val isRegenerating: Boolean = false
)
