package me.rerere.rikkahub.di

import me.rerere.highlight.Highlighter
import me.rerere.rikkahub.utils.UpdateChecker
import org.koin.dsl.module

val appModule = module {
    single {
        Highlighter(get())
    }

    single {
        UpdateChecker()
    }
}