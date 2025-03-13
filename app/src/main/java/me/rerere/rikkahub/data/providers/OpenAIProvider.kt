package me.rerere.rikkahub.data.providers

class OpenAIProvider(
    override var name: String,
    override var enabled: Boolean,
    override var link: String
) : IProvider {
    override fun generateText() {
        TODO("Not yet implemented")
    }

    override fun streamText() {
        TODO("Not yet implemented")
    }
}