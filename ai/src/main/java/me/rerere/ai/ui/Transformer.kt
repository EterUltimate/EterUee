package me.rerere.ai.ui

import me.rerere.ai.provider.Model

interface MessageTransformer {
    fun transform(
        messages: List<UIMessage>,
        model: Model,
    ): List<UIMessage>

    companion object {
        fun transform(
            messages: List<UIMessage>,
            model: Model,
            transformers: List<MessageTransformer>,
        ): List<UIMessage> {
            var result = messages
            transformers.forEach {
                result = it.transform(result, model)
            }
            return result
        }
    }
}