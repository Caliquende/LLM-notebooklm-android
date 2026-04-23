package com.researchflow.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SOURCE_LIMIT = intPreferencesKey("source_limit")
        val BRIDGE_URL = stringPreferencesKey("bridge_url")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    val sourceLimit: Flow<Int> = context.dataStore.data.map { it[Keys.SOURCE_LIMIT] ?: 15 }
    val bridgeUrl: Flow<String> = context.dataStore.data.map { it[Keys.BRIDGE_URL] ?: "http://10.0.2.2:8080" }
    val selectedProvider: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_PROVIDER] ?: "" }
    val selectedModel: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_MODEL] ?: "" }

    suspend fun setSourceLimit(limit: Int) {
        context.dataStore.edit { it[Keys.SOURCE_LIMIT] = limit.coerceIn(5, 50) }
    }

    suspend fun setBridgeUrl(url: String) {
        context.dataStore.edit { it[Keys.BRIDGE_URL] = url }
    }

    suspend fun setSelectedProvider(provider: String) {
        context.dataStore.edit { it[Keys.SELECTED_PROVIDER] = provider }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { it[Keys.SELECTED_MODEL] = model }
    }
}
