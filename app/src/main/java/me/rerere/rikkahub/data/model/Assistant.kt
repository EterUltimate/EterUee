package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import me.rerere.ai.provider.CustomBody
import me.rerere.ai.provider.CustomHeader
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.ai.LocalToolOption
import kotlin.uuid.Uuid

@Serializable
enum class AssistantMode {
    GENERAL, // 方便大多数用户使用，配置门槛低，直观
    ADVANCED, // 适合高级用户，可配置项目多
    ROLE_PLAY; // 适合角色扮演模式
}

@Serializable
data class Assistant(
    val id: Uuid = Uuid.random(),
    val mode: AssistantMode = AssistantMode.ADVANCED,
    val chatModelId: Uuid? = null, // 如果为null, 使用全局默认模型
    val name: String = "",
    val avatar: Avatar = Avatar.Dummy,
    val useAssistantAvatar: Boolean = false, // 使用助手头像替代模型头像
    val tags: List<Uuid> = emptyList(),
    val systemPrompt: String = "",
    val temperature: Float = 1.0f,
    val topP: Float = 1.0f,
    val contextMessageSize: Int = 64,
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val enableRecentChatsReference: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val presetMessages: List<UIMessage> = emptyList(),
    val thinkingBudget: Int? = 1024,
    val maxTokens: Int? = null,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val mcpServers: Set<Uuid> = emptySet(),
    val localTools: List<LocalToolOption> = emptyList(),
    val background: String? = null,
)

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)

@Serializable
data class AssistantGeneralSetting(
    val userNick: String,
    val userJob: String,
    val preferences: String,
    val traits: Set<String>,
)
