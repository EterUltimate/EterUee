package me.rerere.rikkahub.di

import me.rerere.rikkahub.data.ai.GenerationWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workersModule = module {
    worker<GenerationWorker> {
        GenerationWorker(get(), get(), get())
    }
}