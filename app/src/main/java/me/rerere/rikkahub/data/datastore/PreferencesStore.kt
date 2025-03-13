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
        val THEME = stringPreferencesKey("theme")
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
                theme = preferences[THEME] ?: "system",
            )
        }

    suspend fun update(settings: Settings) {
        dataStore.edit { preferences ->
            preferences[THEME] = settings.theme
        }
    }
}

data class Settings(
    val theme: String = "system",
)