package com.eterultimate.eteruee.di

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.serialization.json.Json
import com.eterultimate.eteruee.highlight.Highlighter
import com.eterultimate.eteruee.AppScope
import com.eterultimate.eteruee.data.ai.AILoggingManager
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.data.ai.DynamicAISDK
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.event.AppEventBus
import com.eterultimate.eteruee.device.DeviceAgentManager
import com.eterultimate.eteruee.network.HiddifyCoreManager
import com.eterultimate.eteruee.service.ChatService
import com.eterultimate.eteruee.utils.EmojiData
import com.eterultimate.eteruee.utils.EmojiUtils
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.utils.UpdateChecker
import com.eterultimate.eteruee.web.WebServerManager
import com.eterultimate.eteruee.web.relay.HttpRelayService
import com.eterultimate.eteruee.tts.provider.TTSManager
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService as RoleplayCharacterService
import com.eterultimate.eteruee.roleplay.domain.service.ChatService as RoleplayChatService
import com.eterultimate.eteruee.roleplay.domain.service.GroupService as RoleplayGroupService
import com.eterultimate.eteruee.roleplay.domain.service.PresetService as RoleplayPresetService
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService as RoleplayWorldInfoService
import org.koin.dsl.module

val appModule = module {
    single<Json> { JsonInstant }

    single<AISDK> {
        DynamicAISDK(get(), get())
    }

    single {
        Highlighter(get())
    }

    single {
        AppEventBus()
    }

    single {
        LocalTools(get(), get(), get(), get())
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
        HiddifyCoreManager(get())
    }

    single {
        DeviceAgentManager(get())
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
        HttpRelayService(okHttpClient = get())
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
            aiSDK = get(),
            conversationRepo = get(),
            settingsStore = get(),
            filesManager = get(),
            webDavSync = get(),
            httpRelayService = get(),
            roleplayCharacterService = get<RoleplayCharacterService>(),
            roleplayChatService = get<RoleplayChatService>(),
            roleplayWorldInfoService = get<RoleplayWorldInfoService>(),
            roleplayGroupService = get<RoleplayGroupService>(),
            roleplayPresetService = get<RoleplayPresetService>(),
            localTools = get(),
            deviceAgentManager = get(),
        )
    }
}

