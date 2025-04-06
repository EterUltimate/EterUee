package me.rerere.rikkahub.data.datastore

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
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
        val SELECT_MODEL = stringPreferencesKey("chat_model")
        val PROVIDERS = stringPreferencesKey("providers")
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
                providers = JsonInstant
                    .decodeFromString<List<ProviderSetting>>(preferences[PROVIDERS] ?: "[]"),
            )
        }

    suspend fun update(settings: Settings) {
        dataStore.edit { preferences ->
            preferences[SELECT_MODEL] = settings.chatModelId.toString()
            preferences[PROVIDERS] = JsonInstant.encodeToString(settings.providers)
        }
    }
}

data class Settings(
    val theme: String = "system",
    val chatModelId: Uuid = Uuid.random(),
    val providers: List<ProviderSetting> = emptyList(),
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