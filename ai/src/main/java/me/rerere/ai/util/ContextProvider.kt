package me.rerere.ai.util

import android.content.Context

interface ContextProvider {
    fun getContext(): Context
}

object GlobalContext {
    lateinit var provider: ContextProvider

    fun get(): Context {
        return provider.getContext()
    }
}