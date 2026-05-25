package com.eterultimate.eteruee.shared.roleplay

import kotlinx.serialization.Serializable

@Serializable
enum class SharedMessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL,
}

@Serializable
enum class SharedInsertionPosition {
    AFTER_SYSTEM_PROMPT,
    BEFORE_LAST_USER_MESSAGE,
    AT_END,
}

@Serializable
enum class SharedSelectiveLogic {
    AND,
    OR,
}

@Serializable
data class SharedChatMessage(
    val role: SharedMessageRole,
    val content: String,
    val speakerName: String? = null,
)

@Serializable
data class SharedWorldInfoEntry(
    val key: String = "",
    val keys: List<String> = emptyList(),
    val secondaryKeys: List<String> = emptyList(),
    val content: String = "",
    val constant: Boolean = false,
    val selective: Boolean = false,
    val selectiveLogic: SharedSelectiveLogic = SharedSelectiveLogic.AND,
    val order: Int = 0,
    val position: SharedInsertionPosition = SharedInsertionPosition.AFTER_SYSTEM_PROMPT,
    val enabled: Boolean = true,
) {
    fun allKeys(): List<String> = if (key.isNotBlank()) {
        listOf(key) + keys
    } else {
        keys
    }
}

@Serializable
data class RoleplayPromptBuildRequest(
    val systemPrompt: String = "",
    val worldInfoEntries: List<SharedWorldInfoEntry> = emptyList(),
    val messages: List<SharedChatMessage> = emptyList(),
    val maxContextLength: Int = 4096,
    val matchWorldInfoAgainst: String = "",
)

@Serializable
data class RoleplayPromptBuildResult(
    val prompt: String,
    val injectedEntryCount: Int,
    val truncatedMessageCount: Int,
)
