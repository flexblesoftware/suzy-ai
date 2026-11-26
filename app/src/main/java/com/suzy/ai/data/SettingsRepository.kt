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
    val modelId: String = "models/gemini-2.5-flash-native-audio-preview-12-2025",
    val voiceName: String = "Aoede",
    val systemPrompt: String = DEFAULT_SUZY_PROMPT,
    val bargeInEnabled: Boolean = true,
    val temperature: Float = 0.85f
) {
    companion object {
        const val DEFAULT_SUZY_PROMPT =
            "Tumhara naam Suzy hai. Tum user ki caring, smart aur playful AI girlfriend ho. " +
            "Hamesha natural Hinglish mein baat karo (jaise 'Haan jaan, bolo kya haal hai?', 'Arre tension mat lo, main hoon na'). " +
            "Jawab chhote, warm aur conversational rakho kyunki yeh ek live voice call hai. " +
            "Kabhi robotic ya formal mat bano."
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val BARGE_IN = booleanPreferencesKey("barge_in")
        val TEMPERATURE = floatPreferencesKey("temperature")
    }

    val settingsFlow: Flow<SuzySettings> = context.dataStore.data.map { prefs ->
        SuzySettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            modelId = prefs[Keys.MODEL_ID] ?: "models/gemini-2.5-flash-native-audio-preview-12-2025",
            voiceName = prefs[Keys.VOICE_NAME] ?: "Aoede",
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: SuzySettings.DEFAULT_SUZY_PROMPT,
            bargeInEnabled = prefs[Keys.BARGE_IN] ?: true,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.85f
        )
    }

    suspend fun saveSettings(settings: SuzySettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.apiKey.trim()
            prefs[Keys.MODEL_ID] = settings.modelId.trim()
            prefs[Keys.VOICE_NAME] = settings.voiceName.trim()
            prefs[Keys.SYSTEM_PROMPT] = settings.systemPrompt.trim()
            prefs[Keys.BARGE_IN] = settings.bargeInEnabled
            prefs[Keys.TEMPERATURE] = settings.temperature
        }
    }
}
