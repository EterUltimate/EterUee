package me.rerere.ai.core

import kotlinx.serialization.Serializable

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)