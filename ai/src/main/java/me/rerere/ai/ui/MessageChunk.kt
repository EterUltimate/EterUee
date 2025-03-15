package me.rerere.ai.ui

import kotlinx.serialization.Serializable

@Serializable
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>,
)
