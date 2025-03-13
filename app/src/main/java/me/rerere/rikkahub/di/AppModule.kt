package me.rerere.rikkahub.di

import me.rerere.highlight.Highlighter
import org.koin.dsl.module

val appModule = module {
    single {
        Highlighter(get())
    }
}