package me.rerere.rikkahub

import android.app.Application
import me.rerere.highlight.Highlighter
import me.rerere.rikkahub.ui.pages.chat.ChatVM
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class RikkaHubApp : Application() {
    private val highlighter by lazy {
        Highlighter(this)
    }

    private val appModule = module {
        single { highlighter }

        viewModelOf(::ChatVM)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@RikkaHubApp)
            modules(appModule)
        }
    }
}