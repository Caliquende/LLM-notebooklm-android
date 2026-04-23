package com.researchflow.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val supportedProviders = listOf("openai", "gemini", "anthropic", "groq", "openrouter")

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(providerId: String): String? {
        return prefs.getString("key_$providerId", null)
    }

    fun setApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("key_$providerId", apiKey).apply()
    }

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("key_$providerId").apply()
    }

    fun hasApiKey(providerId: String): Boolean {
        return !prefs.getString("key_$providerId", null).isNullOrBlank()
    }

    fun getConfiguredProviders(): List<String> {
        return supportedProviders.filter { hasApiKey(it) }
    }
}
