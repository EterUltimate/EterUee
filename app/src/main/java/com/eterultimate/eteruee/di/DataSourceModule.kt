package com.eterultimate.eteruee.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.HttpHeaders
import io.pebbletemplates.pebble.PebbleEngine
import kotlinx.serialization.json.Json
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.common.http.AcceptLanguageBuilder
import com.eterultimate.eteruee.BuildConfig
import com.eterultimate.eteruee.data.ai.AIRequestInterceptor
import com.eterultimate.eteruee.data.ai.RequestLoggingInterceptor
import com.eterultimate.eteruee.data.ai.transformers.AssistantTemplateLoader
import com.eterultimate.eteruee.data.ai.GenerationHandler
import com.eterultimate.eteruee.data.ai.transformers.TemplateTransformer
import com.eterultimate.eteruee.data.api.EterUeeAPI
import com.eterultimate.eteruee.data.api.SponsorAPI
import com.eterultimate.eteruee.data.datastore.DEFAULT_ETERUEE_OFFICIAL_API_BASE_URL
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.db.AppDatabase
import com.eterultimate.eteruee.data.db.fts.MessageFtsManager
import com.eterultimate.eteruee.data.db.fts.MessageFtsSchema
import com.eterultimate.eteruee.data.db.migrations.Migration_6_7
import com.eterultimate.eteruee.data.db.migrations.MIGRATION_17_18
import com.eterultimate.eteruee.data.db.migrations.Migration_11_12
import com.eterultimate.eteruee.data.db.migrations.Migration_13_14
import com.eterultimate.eteruee.data.db.migrations.Migration_14_15
import com.eterultimate.eteruee.data.db.migrations.Migration_15_16
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

val dataSourceModule = module {
    single {
        SettingsStore(context = get(), scope = get())
    }

    single {
        val context: Context = get()
        Room.databaseBuilder(context, AppDatabase::class.java, "rikka_hub")
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(Migration_6_7, Migration_11_12, Migration_13_14, Migration_14_15, Migration_15_16, MIGRATION_17_18)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    MessageFtsSchema.ensure(db)
                }

                override fun onOpen(connection: SQLiteConnection) {
                    MessageFtsSchema.ensure(connection)
                }
            })
            .build()
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
        MessageFtsManager(get())
    }

    single { McpManager(settingsStore = get(), appScope = get(), filesManager = get()) }

    single {
        GenerationHandler(
            context = get(),
            providerManager = get(),
            aiSDK = get(),
            json = get(),
            memoryRepo = get(),
            conversationRepo = get(),
            aiLoggingManager = get()
        )
    }

    single<OkHttpClient> {
        val acceptLang = AcceptLanguageBuilder.fromAndroid(get())
            .build()
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .addHeader(HttpHeaders.AcceptLanguage, acceptLang)

                if (originalRequest.header(HttpHeaders.UserAgent) == null) {
                    requestBuilder.addHeader(HttpHeaders.UserAgent, "EterUee-Android/${BuildConfig.VERSION_NAME}")
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
            .addInterceptor(AIRequestInterceptor(remoteConfig = get()))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build().also { SearchService.init(it, get()) }
    }

    single {
        SponsorAPI.create(get())
    }

    single {
        ProviderManager(client = get(), context = get())
    }

    single {
        WebDavSync(
            settingsStore = get(),
            json = get(),
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
            settingsStore = get(),
            json = get(),
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

