package me.rerere.ai.ui

import android.content.Context
import me.rerere.ai.provider.Model

interface MessageTransformer {
    fun transform(
        context: Context,
        messages: List<UIMessage>,
        model: Model,
    ): List<UIMessage>
}

fun List<UIMessage>.transforms(
    transformers: List<MessageTransformer>,
    context: Context,
    model: Model
): List<UIMessage> {
    return transformers.fold(this) { acc, transformer ->
        transformer.transform(context, acc, model)
    }
}