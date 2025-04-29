package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Assistant(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val systemPrompt: String = "",
    val temperature: Float = 0.6f,
    val topP: Float = 1.0f,
    val contextMessageSize: Int = 32,
    val enableMemory: Boolean = false,
)

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)