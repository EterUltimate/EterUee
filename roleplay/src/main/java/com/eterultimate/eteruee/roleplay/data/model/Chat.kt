package com.eterultimate.eteruee.roleplay.data.model

import com.eterultimate.eteruee.ai.core.MessageRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * 聊天元数据
 */
@Serializable
data class ChatMetadata(
    val chatId: Uuid = Uuid.random(),
    val characterId: Uuid,
    val groupId: Uuid? = null,  // 群组聊天ID,单角色聊天为null
    val title: String = "",
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now(),
    val messageCount: Int = 0,
    val pinned: Boolean = false,
    // 分支管理
    val activeBranchId: Uuid? = null,  // 当前激活的分支节点ID
    val rootNodes: List<Uuid> = emptyList(),  // 根节点ID列表
    // 扩展字段
    val variables: Map<String, String> = emptyMap(),
    val extensions: Map<String, JsonElement> = emptyMap()
) {
    fun getDisplayName(): String {
        return title.ifBlank { "Untitled Chat" }
    }
}

/**
 * 聊天消息
 */
@Serializable
data class ChatMessage(
    val id: Uuid = Uuid.random(),
    val role: MessageRole,
    val content: String,
    @Contextual val timestamp: Instant = Instant.now(),
    val model: String? = null,
    val tokenCount: Int? = null,
    val swipeAlternatives: List<String> = emptyList(),  // 滑动回复备选
    val extra: Map<String, JsonElement> = emptyMap()  // 透传字段
)

/**
 * 消息节点(支持分支)
 */
@Serializable
data class MessageNode(
    val id: Uuid = Uuid.random(),
    val messages: List<ChatMessage>,
    val selectedIndex: Int = 0,  // 当前选中的备选消息索引
    val parentId: Uuid? = null,  // 父节点ID（null表示根节点）
    val children: List<Uuid> = emptyList(),  // 子节点ID列表
    val branchLabel: String = ""  // 分支标签（可选）
) {
    /**
     * 获取当前选中的消息
     */
    fun getCurrentMessage(): ChatMessage? {
        return messages.getOrNull(selectedIndex)
    }
    
    /**
     * 切换到下一个备选消息
     */
    fun nextSwipe(): MessageNode {
        if (messages.size <= 1) return this
        return copy(selectedIndex = (selectedIndex + 1) % messages.size)
    }
    
    /**
     * 切换到上一个备选消息
     */
    fun previousSwipe(): MessageNode {
        if (messages.size <= 1) return this
        return copy(selectedIndex = if (selectedIndex - 1 < 0) messages.size - 1 else selectedIndex - 1)
    }
    
    /**
     * 添加子节点
     */
    fun addChild(childId: Uuid): MessageNode {
        return copy(children = children + childId)
    }
    
    /**
     * 移除子节点
     */
    fun removeChild(childId: Uuid): MessageNode {
        return copy(children = children.filter { it != childId })
    }
}

/**
 * 聊天生成事件
 */
sealed interface ChatGenerationEvent {
    data class Streaming(val chunk: String) : ChatGenerationEvent
    data class Complete(val fullMessage: ChatMessage) : ChatGenerationEvent
    data class Error(val error: Throwable) : ChatGenerationEvent
}
