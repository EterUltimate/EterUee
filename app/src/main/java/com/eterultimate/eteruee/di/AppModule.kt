package com.eterultimate.eteruee.di

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.serialization.json.Json
import com.eterultimate.eteruee.highlight.Highlighter
import com.eterultimate.eteruee.AppScope
import com.eterultimate.eteruee.data.ai.AILoggingManager
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.event.AppEventBus
import com.eterultimate.eteruee.service.ChatService
import com.eterultimate.eteruee.utils.EmojiData
import com.eterultimate.eteruee.utils.EmojiUtils
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.utils.UpdateChecker
import com.eterultimate.eteruee.web.WebServerManager
import com.eterultimate.eteruee.tts.provider.TTSManager
import org.koin.dsl.module

val appModule = module {
    single<Json> { JsonInstant }

    single {
        Highlighter(get())
    }

    single {
        AppEventBus()
    }

    single {
        LocalTools(get(), get())
    }

    single {
        UpdateChecker(get())
    }

    single {
        AppScope()
    }

    single<EmojiData> {
        EmojiUtils.loadEmoji(get())
    }

    single {
        TTSManager(get())
    }

    single {
        Firebase.crashlytics
    }

    single {
        Firebase.remoteConfig
    }

    single {
        Firebase.analytics
    }

    single {
        AILoggingManager()
    }

    single {
        ChatService(
            context = get(),
            appScope = get(),
            settingsStore = get(),
            conversationRepo = get(),
            memoryRepository = get(),
            generationHandler = get(),
            templateTransformer = get(),
            providerManager = get(),
            localTools = get(),
            mcpManager = get(),
            filesManager = get(),
            skillManager = get()
        )
    }

    single {
        WebServerManager(
            context = get(),
            appScope = get(),
            chatService = get(),
            conversationRepo = get(),
            settingsStore = get(),
            filesManager = get()
        )
    }
}

