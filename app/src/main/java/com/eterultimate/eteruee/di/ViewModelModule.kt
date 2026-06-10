package com.eterultimate.eteruee.di

import com.eterultimate.eteruee.ui.pages.assistant.AssistantVM
import com.eterultimate.eteruee.ui.pages.assistant.detail.AssistantDetailVM
import com.eterultimate.eteruee.ui.pages.backup.BackupVM
import com.eterultimate.eteruee.ui.pages.chat.ChatDrawerVM
import com.eterultimate.eteruee.ui.pages.chat.ChatVM
import com.eterultimate.eteruee.ui.pages.debug.DebugVM
import com.eterultimate.eteruee.ui.pages.developer.DeveloperVM
import com.eterultimate.eteruee.ui.pages.favorite.FavoriteVM
import com.eterultimate.eteruee.ui.pages.search.SearchVM
import com.eterultimate.eteruee.ui.pages.history.HistoryVM
import com.eterultimate.eteruee.ui.pages.stats.StatsVM
import com.eterultimate.eteruee.ui.pages.imggen.ImgGenVM
import com.eterultimate.eteruee.ui.pages.extensions.PromptVM
import com.eterultimate.eteruee.ui.pages.extensions.QuickMessagesVM
import com.eterultimate.eteruee.ui.pages.extensions.SkillDetailVM
import com.eterultimate.eteruee.ui.pages.extensions.SkillsVM
import com.eterultimate.eteruee.ui.pages.setting.SettingVM
import com.eterultimate.eteruee.ui.pages.share.handler.ShareHandlerVM
import com.eterultimate.eteruee.ui.pages.translator.TranslatorVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<ChatVM> { params ->
        ChatVM(
            id = params.get(),
            context = get(),
            settingsStore = get(),
            conversationRepo = get(),
            chatService = get(),
            updateChecker = get(),
            analytics = get(),
            filesManager = get(),
            favoriteRepository = get(),
        )
    }
    viewModelOf(::ChatDrawerVM)
    viewModelOf(::SettingVM)
    viewModelOf(::DebugVM)
    viewModelOf(::HistoryVM)
    viewModelOf(::AssistantVM)
    viewModel<AssistantDetailVM> {
        AssistantDetailVM(
            id = it.get(),
            settingsStore = get(),
            memoryRepository = get(),
            filesManager = get(),
            skillManager = get(),
        )
    }
    viewModelOf(::TranslatorVM)
    viewModel<ShareHandlerVM> {
        ShareHandlerVM(
            text = it.get(),
            settingsStore = get(),
        )
    }
    viewModelOf(::BackupVM)
    viewModelOf(::ImgGenVM)
    viewModelOf(::DeveloperVM)
    viewModelOf(::PromptVM)
    viewModelOf(::QuickMessagesVM)
    viewModelOf(::SkillsVM)
    viewModelOf(::SkillDetailVM)
    viewModelOf(::FavoriteVM)
    viewModelOf(::SearchVM)
    viewModelOf(::StatsVM)
}

