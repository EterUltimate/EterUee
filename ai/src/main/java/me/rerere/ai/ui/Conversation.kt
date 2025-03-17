package me.rerere.ai.ui

import kotlinx.serialization.Serializable
import me.rerere.ai.util.InstantSerializer
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Conversation(
    val id: Uuid,
    val title: String,
    val messages: List<UIMessage>,
    @Serializable(with = InstantSerializer::class)
    val createAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updateAt: Instant,
)