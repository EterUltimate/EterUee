package me.rerere.ai.util

interface KeyRoulette {
    fun next(keys: String): String

    companion object {
        fun default(): KeyRoulette = DefaultKeyRoulette()
    }
}

private fun splitKey(key: String): List<String> {
    return key
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private class DefaultKeyRoulette : KeyRoulette {
    override fun next(keys: String): String {
        val keyList = splitKey(keys)
        return if (keyList.isNotEmpty()) {
            keyList.random()
        } else {
            keys
        }
    }
}
