package me.rerere.rikkahub

import android.app.Application
import me.rerere.rikkahub.di.appModule
import me.rerere.rikkahub.di.dataStoreModule
import me.rerere.rikkahub.di.providerModule
import me.rerere.rikkahub.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RikkaHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@RikkaHubApp)
            modules(appModule, viewModelModule, dataStoreModule, providerModule)
        }
    }
}