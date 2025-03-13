package me.rerere.rikkahub.data.providers

interface IProvider {
    var name: String
    var enabled: Boolean
    var link: String

    fun generateText()

    fun streamText()
}