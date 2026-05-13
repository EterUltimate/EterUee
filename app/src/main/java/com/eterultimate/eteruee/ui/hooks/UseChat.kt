package com.eterultimate.eteruee.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.GenerateTextResult
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.model.toMessageNode
import kotlin.uuid.Uuid

/**
 * Chat 状态
 */
data class ChatState(
    val messages: List<UIMessage>,
    val isLoading: Boolean,
    val error: Exception?,
    val appendMessage: (UIMessage) -> Unit,
    val handleSubmit: (text: String, attachments: List<UIMessagePart>) -> Unit,
    val stop: () -> Unit,
    val reload: () -> Unit
)

/**
 * useChat Hook - 类似 Vercel AI SDK 的 useChat
 *
 * @param conversationId 对话ID
 * @param aiSDK AI SDK 实例
 * @param model 使用的模型
 * @param initialMessages 初始消息列表
 * @param onFinish 生成完成回调
 * @param onError 错误回调
 */
@Composable
fun useChat(
    conversationId: Uuid,
    aiSDK: AISDK,
    model: Model,
    initialMessages: List<UIMessage> = emptyList(),
    onFinish: ((result: GenerateTextResult) -> Unit)? = null,
    onError: ((error: Exception) -> Unit)? = null
): ChatState {
    val scope = rememberCoroutineScope()
    val holder = remember(conversationId) {
        val initialNodes = initialMessages.map { it.toMessageNode() }
        ChatStateHolder(conversationId, aiSDK, scope, initialNodes).apply {
            this.onFinish = onFinish
            this.onError = onError
        }
    }

    val nodes by holder.nodes.collectAsState()
    val isLoading by holder.isLoading.collectAsState()
    val error by holder.error.collectAsState()

    return ChatState(
        messages = nodes.map { it.currentMessage },
        isLoading = isLoading,
        error = error,
        appendMessage = { holder.appendNode(it.toMessageNode()) },
        handleSubmit = { text, attachments ->
            holder.handleSubmit(model, text, attachments)
        },
        stop = holder::stop,
        reload = {
            holder.reload(model)
        }
    )
}
