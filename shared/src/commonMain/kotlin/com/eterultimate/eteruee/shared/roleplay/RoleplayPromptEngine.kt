package com.eterultimate.eteruee.shared.roleplay

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RoleplayPromptEngine {
    private const val WORLD_INFO_HEADER = "=== World Info ==="

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun buildPrompt(request: RoleplayPromptBuildRequest): RoleplayPromptBuildResult {
        val eligibleEntries = request.worldInfoEntries.filter { it.enabled && it.content.isNotBlank() }
        val matchedEntries = if (request.matchWorldInfoAgainst.isBlank()) {
            eligibleEntries
        } else {
            matchEntries(eligibleEntries, request.matchWorldInfoAgainst)
        }
        val truncatedMessages = truncateMessages(request.messages, request.maxContextLength)

        return RoleplayPromptBuildResult(
            prompt = buildPrompt(
                systemPrompt = request.systemPrompt,
                worldInfoEntries = matchedEntries,
                messages = truncatedMessages,
                maxContextLength = request.maxContextLength,
            ),
            injectedEntryCount = matchedEntries.size,
            truncatedMessageCount = request.messages.size - truncatedMessages.size,
        )
    }

    fun buildPrompt(
        systemPrompt: String,
        worldInfoEntries: List<SharedWorldInfoEntry>,
        messages: List<SharedChatMessage>,
        maxContextLength: Int = 4096,
    ): String {
        val builder = StringBuilder()

        if (systemPrompt.isNotBlank()) {
            builder.appendLine(systemPrompt)
            builder.appendLine()
        }

        val entriesByPosition = groupEntriesByPosition(worldInfoEntries.filter { it.enabled && it.content.isNotBlank() })

        entriesByPosition[SharedInsertionPosition.AFTER_SYSTEM_PROMPT]?.let { entries ->
            appendWorldInfoBlock(builder, entries)
            builder.appendLine()
        }

        val truncatedMessages = truncateMessages(messages, maxContextLength)
        val beforeLastUser = entriesByPosition[SharedInsertionPosition.BEFORE_LAST_USER_MESSAGE].orEmpty()
        val lastUserIndex = truncatedMessages.indexOfLast { it.role == SharedMessageRole.USER }
        val injectedBeforeLastUser = beforeLastUser.isNotEmpty() && lastUserIndex >= 0

        if (injectedBeforeLastUser) {
            for (index in 0 until lastUserIndex) {
                appendMessage(builder, truncatedMessages[index])
            }
            appendWorldInfoBlock(builder, beforeLastUser)
            builder.appendLine()
            for (index in lastUserIndex until truncatedMessages.size) {
                appendMessage(builder, truncatedMessages[index])
            }
        } else {
            truncatedMessages.forEach { message ->
                appendMessage(builder, message)
            }
        }

        entriesByPosition[SharedInsertionPosition.AT_END]?.let { entries ->
            if (entries.isNotEmpty()) {
                builder.appendLine()
                appendWorldInfoBlock(builder, entries)
            }
        }

        return builder.toString()
    }

    fun buildPromptJson(requestJson: String): String {
        val request = json.decodeFromString<RoleplayPromptBuildRequest>(requestJson)
        return json.encodeToString(buildPrompt(request))
    }

    fun samplePrompt(): String = buildPrompt(
        RoleplayPromptBuildRequest(
            systemPrompt = "You are writing in character.",
            worldInfoEntries = listOf(
                SharedWorldInfoEntry(
                    key = "Arcadia",
                    content = "Arcadia is a quiet port city used by the test host.",
                    order = 1,
                ),
            ),
            messages = listOf(
                SharedChatMessage(
                    role = SharedMessageRole.USER,
                    content = "Tell me about Arcadia.",
                ),
            ),
            matchWorldInfoAgainst = "Arcadia",
        ),
    ).prompt

    fun groupEntriesByPosition(
        entries: List<SharedWorldInfoEntry>,
    ): Map<SharedInsertionPosition, List<SharedWorldInfoEntry>> = entries.groupBy { it.position }

    fun formatEntry(entry: SharedWorldInfoEntry): String {
        val keys = entry.allKeys().joinToString(", ")
        return "[${keys}]: ${entry.content}"
    }

    fun formatEntries(entries: List<SharedWorldInfoEntry>): String = entries
        .sortedBy { it.order }
        .joinToString("\n") { formatEntry(it) }

    fun truncateMessages(
        messages: List<SharedChatMessage>,
        maxContextLength: Int,
    ): List<SharedChatMessage> {
        if (messages.isEmpty()) return emptyList()

        var totalLength = calculateMessageLength(messages)
        if (totalLength <= maxContextLength) return messages

        val result = messages.toMutableList()
        while (totalLength > maxContextLength && result.size > 1) {
            val removedMessage = result.removeAt(0)
            totalLength -= removedMessage.content.length + 20
        }

        return result
    }

    fun calculateMessageLength(messages: List<SharedChatMessage>): Int = messages.sumOf { message ->
        message.content.length + 20
    }

    fun matchEntries(
        entries: List<SharedWorldInfoEntry>,
        text: String,
    ): List<SharedWorldInfoEntry> = entries.filter { entry -> isEntryMatched(entry, text) }

    fun isEntryMatched(entry: SharedWorldInfoEntry, text: String): Boolean {
        if (!entry.enabled) return false
        if (entry.constant) return true

        val primaryMatched = anyKeyMatches(entry.allKeys(), text)
        if (!entry.selective) return primaryMatched

        val secondaryMatched = anyKeyMatches(entry.secondaryKeys, text)
        return when (entry.selectiveLogic) {
            SharedSelectiveLogic.AND -> primaryMatched && secondaryMatched
            SharedSelectiveLogic.OR -> primaryMatched || secondaryMatched
        }
    }

    private fun anyKeyMatches(keys: List<String>, text: String): Boolean = keys
        .filter { it.isNotBlank() }
        .any { key -> text.contains(key, ignoreCase = true) }

    private fun appendWorldInfoBlock(
        builder: StringBuilder,
        entries: List<SharedWorldInfoEntry>,
    ) {
        if (entries.isEmpty()) return

        builder.appendLine(WORLD_INFO_HEADER)
        builder.appendLine(formatEntries(entries))
    }

    private fun appendMessage(
        builder: StringBuilder,
        message: SharedChatMessage,
    ) {
        val roleLabel = when (message.role) {
            SharedMessageRole.USER -> "User"
            SharedMessageRole.ASSISTANT -> message.speakerName?.takeIf { it.isNotBlank() } ?: "Assistant"
            SharedMessageRole.SYSTEM -> "System"
            SharedMessageRole.TOOL -> "Tool"
        }

        builder.appendLine("$roleLabel: ${message.content}")
        builder.appendLine()
    }
}
