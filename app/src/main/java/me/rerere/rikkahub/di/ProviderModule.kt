package me.rerere.rikkahub.di

import me.rerere.ai.provider.ProviderManager
import org.koin.dsl.module

val providerModule = module {
    single { ProviderManager() }
}