package me.rerere.ai.ui

interface MessageTransformer {
    fun transform(messages: List<UIMessage>): List<UIMessage>

    companion object {
        fun transform(messages: List<UIMessage>, transformers: List<MessageTransformer>): List<UIMessage> {
            var result = messages
            transformers.forEach {
                result = it.transform(result)
            }
            return result
        }
    }
}