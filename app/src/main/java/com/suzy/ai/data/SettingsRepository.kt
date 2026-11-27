package com.suzy.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "suzy_settings")

data class SuzySettings(
    val apiKey: String = "",
    val modelId: String = "models/gemini-3.8-live",
    val voiceName: String = "Aoede",
    val activePersonaName: String = "Suzy",
    val systemPrompt: String = DEFAULT_SUZY_PROMPT,
    val bargeInEnabled: Boolean = true,
    val temperature: Float = 0.95f
) {
    companion object {
        val DEFAULT_SUZY_PROMPT = SuzyCatalog.personas.first().systemPrompt
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val PERSONA_NAME = stringPreferencesKey("persona_name")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt_v4")
        val BARGE_IN = booleanPreferencesKey("barge_in")
        val TEMPERATURE = floatPreferencesKey("temperature")
    }

    val settingsFlow: Flow<SuzySettings> = context.dataStore.data.map { prefs ->
        SuzySettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            modelId = prefs[Keys.MODEL_ID] ?: "models/gemini-3.8-live",
            voiceName = prefs[Keys.VOICE_NAME] ?: "Aoede",
            activePersonaName = prefs[Keys.PERSONA_NAME] ?: "Suzy",
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: SuzySettings.DEFAULT_SUZY_PROMPT,
            bargeInEnabled = prefs[Keys.BARGE_IN] ?: true,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.95f
        )
    }

    suspend fun saveSettings(settings: SuzySettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.apiKey.trim()
            prefs[Keys.MODEL_ID] = settings.modelId.trim()
            prefs[Keys.VOICE_NAME] = settings.voiceName.trim()
            prefs[Keys.PERSONA_NAME] = settings.activePersonaName.trim()
            prefs[Keys.SYSTEM_PROMPT] = settings.systemPrompt.trim()
            prefs[Keys.BARGE_IN] = settings.bargeInEnabled
            prefs[Keys.TEMPERATURE] = settings.temperature
        }
    }
}
