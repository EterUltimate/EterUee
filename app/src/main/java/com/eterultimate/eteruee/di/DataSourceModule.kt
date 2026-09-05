package com.eterultimate.eteruee.di

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.HttpHeaders
import io.pebbletemplates.pebble.PebbleEngine
import kotlinx.serialization.json.Json
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.common.http.AcceptLanguageBuilder
import com.eterultimate.eteruee.data.network.SettingsProxyAuthenticator
import com.eterultimate.eteruee.data.network.SettingsProxySelector
import com.eterultimate.eteruee.data.network.SettingsSocks5Authenticator
import com.eterultimate.eteruee.BuildConfig
import com.eterultimate.eteruee.data.ai.AIRequestInterceptor
import com.eterultimate.eteruee.data.ai.RequestLoggingInterceptor
import com.eterultimate.eteruee.data.ai.transformers.AssistantTemplateLoader
import com.eterultimate.eteruee.data.ai.GenerationHandler
import com.eterultimate.eteruee.data.ai.TranslationHandler
import com.eterultimate.eteruee.data.ai.transformers.TemplateTransformer
import com.eterultimate.eteruee.data.api.EterUeeAPI
import com.eterultimate.eteruee.data.api.SponsorAPI
import com.eterultimate.eteruee.data.datastore.DEFAULT_ETERUEE_OFFICIAL_API_BASE_URL
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.db.AppDatabase
import com.eterultimate.eteruee.data.db.AppDatabaseFactory
import com.eterultimate.eteruee.data.sync.BackupManager
import com.eterultimate.eteruee.data.db.fts.MessageFtsManager
import com.eterultimate.eteruee.data.ai.mcp.McpManager
import com.eterultimate.eteruee.data.sync.webdav.WebDavSync
import com.eterultimate.eteruee.search.SearchService
import com.eterultimate.eteruee.data.sync.PostgresGatewaySync
import com.eterultimate.eteruee.data.sync.S3Sync
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

val dataSourceModule = module {
    single {
        SettingsStore(context = get(), scope = get())
    }

    single {
        val context: Context = get()
        AppDatabaseFactory.create(context)
    }

    single {
        AssistantTemplateLoader(settingsStore = get())
    }

    single {
        PebbleEngine.Builder()
            .loader(get<AssistantTemplateLoader>())
            .defaultLocale(Locale.getDefault())
            .autoEscaping(false)
            .build()
    }

    single { TemplateTransformer(engine = get(), settingsStore = get()) }

    single {
        get<AppDatabase>().conversationDao()
    }

    single {
        get<AppDatabase>().memoryDao()
    }

    single {
        get<AppDatabase>().genMediaDao()
    }

    single {
        get<AppDatabase>().messageNodeDao()
    }

    single {
        get<AppDatabase>().managedFileDao()
    }

    single {
        get<AppDatabase>().favoriteDao()
    }

    single {
        get<AppDatabase>().folderDao()
    }

    single {
        MessageFtsManager(get())
    }

    single { McpManager(settingsStore = get(), appScope = get(), filesManager = get(), appEventBus = get()) }

    single {
        GenerationHandler(
            context = get(),
            providerManager = get(),
            aiSDK = get(),
            json = get(),
            memoryRepo = get()
        )
    }

    single {
        TranslationHandler(providerManager = get())
    }

    single<OkHttpClient> {
        val settingsStore: SettingsStore = get()
        val acceptLang = AcceptLanguageBuilder.fromAndroid(get())
            .build()
        java.net.Authenticator.setDefault(SettingsSocks5Authenticator(settingsStore))
        val initialNetworkSetting = settingsStore.settingsFlow.value.networkSetting
        val appliedProxySetting = AtomicReference(
            Triple(
                initialNetworkSetting.proxyUrl,
                initialNetworkSetting.proxyUsername,
                initialNetworkSetting.proxyPassword,
            )
        )
        lateinit var client: OkHttpClient
        client = OkHttpClient.Builder()
            .proxySelector(SettingsProxySelector(settingsStore))
            .proxyAuthenticator(SettingsProxyAuthenticator(settingsStore))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val networkSetting = settingsStore.settingsFlow.value.networkSetting
                val currentProxySetting = Triple(
                    networkSetting.proxyUrl,
                    networkSetting.proxyUsername,
                    networkSetting.proxyPassword,
                )
                if (appliedProxySetting.getAndSet(currentProxySetting) != currentProxySetting) {
                    client.connectionPool.evictAll()
                }

                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .addHeader(HttpHeaders.AcceptLanguage, acceptLang)

                if (originalRequest.header(HttpHeaders.UserAgent) == null) {
                    val userAgent = settingsStore.settingsFlow.value.networkSetting.userAgent
                        .trim()
                        .ifEmpty { "EterUee-Android/${BuildConfig.VERSION_NAME}" }
                    requestBuilder.addHeader(HttpHeaders.UserAgent, userAgent)
                }

                chain.proceed(requestBuilder.build())
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val contentTypeHeader = request.header("Content-Type")
                if (contentTypeHeader != null && contentTypeHeader.contains(";")) {
                    chain.proceed(
                        request.newBuilder()
                            .header("Content-Type", contentTypeHeader.substringBefore(";").trim())
                            .build()
                    )
                } else {
                    chain.proceed(request)
                }
            }
            .addNetworkInterceptor(RequestLoggingInterceptor())
            .addInterceptor(AIRequestInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
        client.also { SearchService.init(it, get()) }
    }

    single {
        SponsorAPI.create(get())
    }

    single {
        ProviderManager(client = get(), context = get())
    }

    single { BackupManager(context = get(), database = get(), settingsStore = get(), json = get()) }

    single {
        WebDavSync(
            backupManager = get(),
            context = get(),
            httpClient = get()
        )
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(20, TimeUnit.SECONDS)
                    readTimeout(10, TimeUnit.MINUTES)
                    writeTimeout(120, TimeUnit.SECONDS)
                    followSslRedirects(true)
                    followRedirects(true)
                    retryOnConnectionFailure(true)
                }
            }
        }
    }

    single {
        S3Sync(
            backupManager = get(),
            context = get(),
            httpClient = get()
        )
    }

    single {
        PostgresGatewaySync(
            settingsStore = get(),
            webDavSync = get(),
            httpClient = get()
        )
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(DEFAULT_ETERUEE_OFFICIAL_API_BASE_URL)
            .addConverterFactory(get<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single<EterUeeAPI> {
        get<Retrofit>().create(EterUeeAPI::class.java)
    }
}

