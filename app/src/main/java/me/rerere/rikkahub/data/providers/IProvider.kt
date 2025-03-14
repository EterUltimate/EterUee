package me.rerere.rikkahub.data.providers

import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.model.TextGenerationRequest

interface IProvider {
    var name: String
    var enabled: Boolean
    var link: String

    suspend fun generateText(request: TextGenerationRequest)

    fun streamText(request: TextGenerationRequest): Flow<String>
}