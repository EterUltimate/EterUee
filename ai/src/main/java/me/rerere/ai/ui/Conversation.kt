package me.rerere.ai.ui

import kotlinx.serialization.Serializable
import me.rerere.ai.util.InstantSerializer
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Conversation(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val messages: List<UIMessage>,
    @Serializable(with = InstantSerializer::class)
    val createAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updateAt: Instant = Instant.now(),
) {
    companion object {
        fun empty() = Conversation(messages = emptyList())

        fun ofId(id: Uuid) = Conversation(
            id = id,
            messages = emptyList(),
        )
    }
}