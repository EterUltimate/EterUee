package me.rerere.rikkahub.data.datastore

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.utils.JsonInstant
import kotlin.uuid.Uuid

private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "settings"),
        )
    }
)

class SettingsStore(context: Context) {
    companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SELECT_MODEL = stringPreferencesKey("chat_model")
        val TITLE_MODEL = stringPreferencesKey("title_model")
        val PROVIDERS = stringPreferencesKey("providers")

        val SELECT_ASSISTANT = stringPreferencesKey("select_assistant")
        val ASSISTANTS = stringPreferencesKey("assistants")
    }

    private val dataStore = context.settingsStore

    val settingsFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            Settings(
                chatModelId = preferences[SELECT_MODEL]?.let { Uuid.parse(it) } ?: Uuid.random(),
                titleModelId = preferences[TITLE_MODEL]?.let { Uuid.parse(it) } ?: Uuid.random(),
                assistantId = preferences[SELECT_ASSISTANT]?.let { Uuid.parse(it) } ?: DEFAULT_ASSISTANT_ID,
                providers = JsonInstant.decodeFromString(preferences[PROVIDERS] ?: "[]"),
                assistants = JsonInstant.decodeFromString(preferences[ASSISTANTS] ?: "[]"),
                dynamicColor = preferences[DYNAMIC_COLOR] != false,
            )
        }
        .catch {
            it.printStackTrace()
            update(Settings())
            emit(Settings())
        }
        .map {
            val providers = it.providers.ifEmpty { DEFAULT_PROVIDERS }.toMutableList()
            DEFAULT_PROVIDERS.forEach { defaultProvider ->
                if (providers.none { it.id == defaultProvider.id }) {
                    providers.add(0, defaultProvider)
                }
            }
            val assistants = it.assistants.ifEmpty { DEFAULT_ASSISTANTS }.toMutableList()
            DEFAULT_ASSISTANTS.forEach { defaultAssistant ->
                if(assistants.none { it.id == defaultAssistant.id }) {
                    assistants.add(0, defaultAssistant)
                }
            }
            it.copy(
                providers = providers,
                assistants = assistants
            )
        }

    suspend fun update(settings: Settings) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = settings.dynamicColor

            preferences[SELECT_MODEL] = settings.chatModelId.toString()
            preferences[TITLE_MODEL] = settings.titleModelId.toString()
            preferences[PROVIDERS] = JsonInstant.encodeToString(settings.providers)

            preferences[ASSISTANTS] = JsonInstant.encodeToString(settings.assistants)
            preferences[SELECT_ASSISTANT] = settings.assistantId.toString()
        }
    }
}

data class Settings(
    val dynamicColor: Boolean = true,
    val chatModelId: Uuid = Uuid.random(),
    val titleModelId: Uuid = Uuid.random(),
    val assistantId: Uuid = DEFAULT_ASSISTANT_ID,
    val providers: List<ProviderSetting> = DEFAULT_PROVIDERS,
    val assistants: List<Assistant> = DEFAULT_ASSISTANTS,
)

fun List<ProviderSetting>.findModelById(uuid: Uuid): Model? {
    this.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == uuid) {
                return model
            }
        }
    }
    return null
}

fun Model.findProvider(providers: List<ProviderSetting>): ProviderSetting? {
    providers.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == this.id) {
                return setting
            }
        }
    }
    return null
}

private val DEFAULT_PROVIDERS = listOf(
    ProviderSetting.OpenAI(
        id = Uuid.parse("1eeea727-9ee5-4cae-93e6-6fb01a4d051e"),
        name = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        apiKey = "sk-"
    ),
    ProviderSetting.OpenAI(
        id = Uuid.parse("f099ad5b-ef03-446d-8e78-7e36787f780b"),
        name = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1",
        apiKey = "sk-"
    ),
    ProviderSetting.OpenAI(
        id = Uuid.parse("d5734028-d39b-4d41-9841-fd648d65440e"),
        name = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        apiKey = ""
    ),
    ProviderSetting.OpenAI(
        id = Uuid.parse("f76cae46-069a-4334-ab8e-224e4979e58c"),
        name = "阿里云百炼",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        apiKey = ""
    ),
    ProviderSetting.OpenAI(
        id = Uuid.parse("3dfd6f9b-f9d9-417f-80c1-ff8d77184191"),
        name = "火山引擎",
        baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
        apiKey = ""
    ),
)

private val DEFAULT_ASSISTANT_ID = Uuid.parse("0950e2dc-9bd5-4801-afa3-aa887aa36b4e")
private val DEFAULT_ASSISTANTS = listOf(
    Assistant(
        id = DEFAULT_ASSISTANT_ID,
        name = "",
        temperature = 0.6f,
        systemPrompt = ""
    )
)