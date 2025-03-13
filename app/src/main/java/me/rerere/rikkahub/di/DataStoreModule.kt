package me.rerere.rikkahub.di

import me.rerere.rikkahub.data.datastore.SettingsStore
import org.koin.dsl.module

val dataStoreModule = module {
    single {
        SettingsStore(get())
    }
}