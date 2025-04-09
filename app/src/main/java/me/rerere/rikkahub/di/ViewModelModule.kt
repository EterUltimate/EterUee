package me.rerere.rikkahub.di

import me.rerere.rikkahub.ui.pages.chat.ChatVM
import me.rerere.rikkahub.ui.pages.debug.DebugVM
import me.rerere.rikkahub.ui.pages.setting.SettingVM
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::ChatVM)
    viewModelOf(::SettingVM)
    viewModelOf(::DebugVM)
}