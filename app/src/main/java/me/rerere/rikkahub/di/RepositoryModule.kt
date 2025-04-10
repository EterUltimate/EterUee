package me.rerere.rikkahub.di

import me.rerere.rikkahub.data.repository.ConversationRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        ConversationRepository(get(), get())
    }
}