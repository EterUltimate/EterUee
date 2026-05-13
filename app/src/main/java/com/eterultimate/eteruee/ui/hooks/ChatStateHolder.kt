package com.eterultimate.eteruee.ui.hooks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.FinishReason
import com.eterultimate.eteruee.ai.sdk.GenerateTextResult
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.model.MessageNode
import com.eterultimate.eteruee.data.model.toMessageNode
import kotlin.uuid.Uuid

import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.streamTextWithTools

/**
 * Chat 状态持有者
 * 封装聊天逻辑,可在 ViewModel 中使用
 */
class ChatStateHolder(
    val conversationId: Uuid,
    private val aiSDK: AISDK,
    private val scope: CoroutineScope,
    initialNodes: List<MessageNode> = emptyList()
) {
    private val _nodes = MutableStateFlow(initialNodes)
    val nodes: StateFlow<List<MessageNode>> = _nodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Exception?>(null)
    val error: StateFlow<Exception?> = _error.asStateFlow()

    private var currentJob: Job? = null

    var onFinish: ((result: GenerateTextResult) -> Unit)? = null
    var onError: ((error: Exception) -> Unit)? = null
    var toolExecutor: ToolExecutor? = null

    fun appendNode(node: MessageNode) {
        _nodes.value = _nodes.value + node
    }

    fun setNodes(nodes: List<MessageNode>) {
        _nodes.value = nodes
    }

    fun handleSubmit(
        model: Model,
        text: String,
        attachments: List<UIMessagePart> = emptyList(),
        addMessage: Boolean = true
    ) {
        if (text.isBlank() && attachments.isEmpty() && addMessage) return

        _isLoading.value = true
        _error.value = null

        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                if (addMessage) {
                    // 1. 添加用户消息
                    val userMessage = UIMessage(
                        role = MessageRole.USER,
                        parts = buildList {
                            if (text.isNotBlank()) {
                                add(UIMessagePart.Text(text = text))
                            }
                            addAll(attachments)
                        }
                    )
                    appendNode(userMessage.toMessageNode())
                }

                // 2. 创建空的 assistant 消息用于流式更新
                val assistantMessageId = Uuid.random()
                val assistantMessage = UIMessage(
                    id = assistantMessageId,
                    role = MessageRole.ASSISTANT,
                    parts = emptyList()
                )
                appendNode(assistantMessage.toMessageNode())

                // 3. 构建请求
                val request = StreamTextRequest(
                    model = model,
                    messages = _nodes.value.map { it.currentMessage }
                )

                // 4. 流式生成
                var accumulatedText = ""
                val stream = if (toolExecutor != null) {
                    aiSDK.streamTextWithTools(request, toolExecutor!!)
                } else {
                    aiSDK.streamText(request)
                }

                stream.collectLatest { chunk ->
                    when (chunk) {
                        is TextChunk.TextDelta -> {
                            accumulatedText += chunk.text
                            updateAssistantMessage(assistantMessageId) {
                                it.copy(parts = listOf(UIMessagePart.Text(text = accumulatedText)))
                            }
                        }
                        is TextChunk.Usage -> {
                            updateAssistantMessage(assistantMessageId) {
                                it.copy(usage = chunk.tokenUsage)
                            }
                        }
                        is TextChunk.Finish -> {
                            _isLoading.value = false
                            val finalMessage = _nodes.value.lastOrNull { it.currentMessage.id == assistantMessageId }?.currentMessage
                            if (finalMessage != null) {
                                val result = GenerateTextResult(
                                    text = accumulatedText,
                                    usage = finalMessage.usage,
                                    finishReason = FinishReason.STOP,
                                    message = finalMessage
                                )
                                onFinish?.invoke(result)
                            }
                        }
                        is TextChunk.ToolCall -> {
                            // 工具调用由 streamTextWithTools 内部处理并产生新的 TextDelta
                            // 这里可以更新 UI 显示正在调用工具
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e
                _isLoading.value = false
                onError?.invoke(e)
            }
        }
    }

    private fun updateAssistantMessage(id: Uuid, transform: (UIMessage) -> UIMessage) {
        _nodes.value = _nodes.value.map { node ->
            if (node.currentMessage.id == id) {
                node.copy(messages = node.messages.map {
                    if (it.id == id) transform(it) else it
                })
            } else node
        }
    }

    fun stop() {
        currentJob?.cancel()
        _isLoading.value = false
    }

    fun reload(model: Model) {
        val currentNodes = _nodes.value
        if (currentNodes.size < 2) return

        // 移除最后一条 assistant 消息
        val lastAssistantIndex = currentNodes.indexOfLast { it.currentMessage.role == MessageRole.ASSISTANT }
        if (lastAssistantIndex != -1) {
            _nodes.value = currentNodes.filterIndexed { index, _ -> index != lastAssistantIndex }
        }

        // 获取最后一条用户消息重新提交
        val lastUserNode = _nodes.value.lastOrNull { it.currentMessage.role == MessageRole.USER }
        if (lastUserNode != null) {
            val lastUserMessage = lastUserNode.currentMessage
            val text = lastUserMessage.parts.filterIsInstance<UIMessagePart.Text>().joinToString("") { it.text }
            val attachments = lastUserMessage.parts.filter { it !is UIMessagePart.Text }
            // 先从列表中移除最后一条用户消息，因为 handleSubmit 会重新添加它
            _nodes.value = _nodes.value.dropLast(1)
            handleSubmit(model, text, attachments)
        }
    }
}
