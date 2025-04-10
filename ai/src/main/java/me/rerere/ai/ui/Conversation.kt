package me.rerere.ai.ui

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
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
    val files: List<Uri>
        get() = messages
            .flatMap { it.parts }
            .filterIsInstance<UIMessagePart.Image>()
            .map { it.url.toUri() }

    companion object {
        fun empty() = Conversation(messages = emptyList())

        fun ofId(id: Uuid) = Conversation(
            id = id,
            messages = emptyList(),
        )

        fun ofUser(prompt: String) = Conversation(
            messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(UIMessagePart.Text(prompt)),
                ),
            ),
        )
    }
}