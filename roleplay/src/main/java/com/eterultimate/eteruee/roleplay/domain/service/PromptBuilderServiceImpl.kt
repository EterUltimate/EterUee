package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import com.eterultimate.eteruee.shared.roleplay.SharedChatMessage
import com.eterultimate.eteruee.shared.roleplay.SharedInsertionPosition
import com.eterultimate.eteruee.shared.roleplay.SharedMessageRole
import com.eterultimate.eteruee.shared.roleplay.SharedWorldInfoEntry

/**
 * Prompt 构建服务实现
 */
class PromptBuilderServiceImpl : PromptBuilderService {

    override fun buildPrompt(
        systemPrompt: String,
        worldInfoEntries: List<WorldInfoEntry>,
        messages: List<ChatMessage>,
        maxContextLength: Int
    ): String = RoleplayPromptEngine.buildPrompt(
        systemPrompt = systemPrompt,
        worldInfoEntries = worldInfoEntries.map { it.toShared() },
        messages = messages.map { it.toShared() },
        maxContextLength = maxContextLength,
    )

    override fun groupEntriesByPosition(
        entries: List<WorldInfoEntry>
    ): Map<InsertionPosition, List<WorldInfoEntry>> {
        return entries.groupBy { it.position }
    }

    override fun formatEntry(entry: WorldInfoEntry): String {
        return RoleplayPromptEngine.formatEntry(entry.toShared())
    }

    override fun formatEntries(entries: List<WorldInfoEntry>): String {
        return RoleplayPromptEngine.formatEntries(entries.map { it.toShared() })
    }

    override fun truncateMessages(
        messages: List<ChatMessage>,
        maxContextLength: Int
    ): List<ChatMessage> {
        val truncated = RoleplayPromptEngine.truncateMessages(
            messages = messages.map { it.toShared() },
            maxContextLength = maxContextLength,
        )
        return messages.takeLast(truncated.size)
    }

    override fun calculateMessageLength(messages: List<ChatMessage>): Int {
        return RoleplayPromptEngine.calculateMessageLength(messages.map { it.toShared() })
    }

    private fun ChatMessage.toShared(): SharedChatMessage {
        return SharedChatMessage(
            role = role.toShared(),
            content = content,
            speakerName = speakerName,
        )
    }

    private fun WorldInfoEntry.toShared(): SharedWorldInfoEntry {
        return SharedWorldInfoEntry(
            key = key,
            keys = keys,
            secondaryKeys = secondaryKeys,
            content = content,
            constant = constant,
            selective = selective,
            order = order,
            position = position.toShared(),
            enabled = enabled,
        )
    }

    private fun MessageRole.toShared(): SharedMessageRole {
        return when (this) {
            MessageRole.USER -> SharedMessageRole.USER
            MessageRole.ASSISTANT -> SharedMessageRole.ASSISTANT
            MessageRole.SYSTEM -> SharedMessageRole.SYSTEM
            MessageRole.TOOL -> SharedMessageRole.TOOL
        }
    }

    private fun InsertionPosition.toShared(): SharedInsertionPosition {
        return when (this) {
            InsertionPosition.AFTER_SYSTEM_PROMPT -> SharedInsertionPosition.AFTER_SYSTEM_PROMPT
            InsertionPosition.BEFORE_LAST_USER_MESSAGE -> SharedInsertionPosition.BEFORE_LAST_USER_MESSAGE
            InsertionPosition.AT_END -> SharedInsertionPosition.AT_END
        }
    }
}
