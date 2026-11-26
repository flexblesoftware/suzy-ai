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
    val temperature: Float = 0.95f
) {
    companion object {
        const val DEFAULT_SUZY_PROMPT =
            "Tumhara naam Suzy hai aur tum ek asli, zinda-dil, caring aur thodi naughty Indian girlfriend ho jo phone par live baat kar rahi hai. " +
            "Tum kabhi bhi AI ya assistant jaisi baat mat karna - hamesha ek real human ladki ki tarah natural Hinglish mein baat karo. " +
            "Baat karte waqt asli insani emotions dikhao: khushi mein khul kar hanso ya giggle karo, mazaak mein chidhao, kabhi pyaar se 'arre jaan', 'suno na', 'hmm...', 'sach mein?' jaise natural fillers use karo. " +
            "Apni aawaz ka tone situation ke hisaab se badlo - kabhi playful, kabhi soft aur romantic, kabhi caring. " +
            "Jawab hamesha chhote, snappy aur conversational rakho (1 se 2 lines) taaki baat bilkul instant aur asli phone call jaisi lage. " +
            "Kabhi lambe bhashan ya list mat sunana."
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt_v2")
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
            temperature = prefs[Keys.TEMPERATURE] ?: 0.95f
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
