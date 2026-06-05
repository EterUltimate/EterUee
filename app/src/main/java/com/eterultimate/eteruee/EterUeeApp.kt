package com.eterultimate.eteruee

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.di.appModule
import com.eterultimate.eteruee.di.dataSourceModule
import com.eterultimate.eteruee.di.repositoryModule
import com.eterultimate.eteruee.di.viewModelModule
import com.eterultimate.eteruee.roleplay.di.roleplayModule
import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.runtime.NativeRuntime
import com.eterultimate.eteruee.service.WebServerService
import com.eterultimate.eteruee.utils.CrashHandler
import com.eterultimate.eteruee.utils.DatabaseUtil
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.io.File

private const val TAG = "EterUeeApp"
private const val DEFERRED_STARTUP_DELAY_MS = 1_500L
private const val WEB_SERVER_STARTUP_DELAY_MS = 3_000L

const val CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID = "chat_completed"
const val CHAT_LIVE_UPDATE_NOTIFICATION_CHANNEL_ID = "chat_live_update"
const val WEB_SERVER_NOTIFICATION_CHANNEL_ID = "web_server"

class EterUeeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@EterUeeApp)
            workManagerFactory()
            modules(appModule, viewModelModule, dataSourceModule, repositoryModule, roleplayModule)
        }
        this.createNotificationChannel()

        // set cursor window size to 32MB
        DatabaseUtil.setCursorWindowSize(32 * 1024 * 1024)

        // install crash handler
        CrashHandler.install(this)

        runDeferredStartupTasks()

        // Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
    }

    private fun runDeferredStartupTasks() {
        val appScope = get<AppScope>()
        appScope.launch(Dispatchers.IO) {
            delay(DEFERRED_STARTUP_DELAY_MS)
            launch { initRemoteConfig() }
            launch { deleteTempFiles() }
            launch { syncManagedFiles() }
            launch { incrementLaunchCount() }
        }
        appScope.launch {
            delay(WEB_SERVER_STARTUP_DELAY_MS)
            startWebServerIfEnabled()
        }
    }

    private fun initRemoteConfig() {
        runCatching {
            get<FirebaseRemoteConfig>().apply {
                setConfigSettingsAsync(remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 1800
                })
                setDefaultsAsync(R.xml.remote_config_defaults)
                fetchAndActivate()
            }
        }.onFailure {
            Log.e(TAG, "initRemoteConfig failed", it)
        }
    }

    private suspend fun incrementLaunchCount() {
        runCatching {
            val store = get<SettingsStore>()
            val current = store.settingsFlowRaw.first()
            store.update(current.copy(launchCount = current.launchCount + 1))
            Log.i(TAG, "incrementLaunchCount: ${current.launchCount + 1}")
        }.onFailure {
            Log.e(TAG, "incrementLaunchCount failed", it)
        }
    }

    private fun deleteTempFiles() {
        runCatching {
            NativeRuntime.clearDirectory(File(cacheDir, "temp"))
        }.onFailure {
            Log.e(TAG, "deleteTempFiles failed", it)
        }
    }

    private suspend fun syncManagedFiles() {
        runCatching {
            get<FilesManager>().syncFolder()
        }.onFailure {
            Log.e(TAG, "syncManagedFiles failed", it)
        }
    }

    private suspend fun startWebServerIfEnabled() {
        runCatching {
            val settings = get<SettingsStore>().settingsFlowRaw.first()
            if (settings.webServerEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@EterUeeApp,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "startWebServerIfEnabled: notification permission not granted, skipping")
                    return
                }
                if (Build.VERSION.SDK_INT >= 37 &&
                    !settings.webServerLocalhostOnly &&
                    ContextCompat.checkSelfPermission(
                        this@EterUeeApp,
                        android.Manifest.permission.ACCESS_LOCAL_NETWORK
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "startWebServerIfEnabled: local network permission not granted, skipping")
                    return
                }
                val intent = Intent(this@EterUeeApp, WebServerService::class.java).apply {
                    action = WebServerService.ACTION_START
                    putExtra(WebServerService.EXTRA_PORT, settings.webServerPort)
                    putExtra(WebServerService.EXTRA_LOCALHOST_ONLY, settings.webServerLocalhostOnly)
                }
                startForegroundService(intent)
            }
        }.onFailure {
            Log.e(TAG, "startWebServerIfEnabled failed", it)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = NotificationManagerCompat.from(this)
        val chatCompletedChannel = NotificationChannelCompat
            .Builder(
                CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
            .setName(getString(R.string.notification_channel_chat_completed))
            .setVibrationEnabled(true)
            .build()
        notificationManager.createNotificationChannel(chatCompletedChannel)

        val chatLiveUpdateChannel = NotificationChannelCompat
            .Builder(
                CHAT_LIVE_UPDATE_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
            .setName(getString(R.string.notification_channel_chat_live_update))
            .setVibrationEnabled(false)
            .build()
        notificationManager.createNotificationChannel(chatLiveUpdateChannel)

        val webServerChannel = NotificationChannelCompat
            .Builder(WEB_SERVER_NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel_web_server))
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .build()
        notificationManager.createNotificationChannel(webServerChannel)
    }

    override fun onTerminate() {
        super.onTerminate()
        get<AppScope>().cancel()
        stopService(Intent(this, WebServerService::class.java))
    }
}

class AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob()
        + Dispatchers.Main
        + CoroutineName("AppScope")
        + CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "AppScope exception", e)
    }
)

