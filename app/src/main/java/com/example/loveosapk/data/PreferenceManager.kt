package com.example.loveosapk.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val appStateKey = stringPreferencesKey("app_state")

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { preferences ->
        val json = preferences[appStateKey] ?: return@map AppState()
        try {
            Json.decodeFromString<AppState>(json)
        } catch (e: Exception) {
            AppState()
        }
    }

    suspend fun updateAppState(state: AppState) {
        context.dataStore.edit { preferences ->
            preferences[appStateKey] = Json.encodeToString(state)
        }
    }
}
