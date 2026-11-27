package com.suzy.ai.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "suzy_settings")

data class MemoryItem(
    val id: String,
    val category: String,
    val content: String,
    val createdAt: String
)

data class SavedTranscript(
    val id: String,
    val personaName: String,
    val dateTime: String,
    val durationLabel: String,
    val dialogue: String
)

data class SuzySettings(
    val apiKey: String = "",
    val modelId: String = "models/gemini-3.8-live",
    val voiceName: String = "Aoede",
    val activePersonaName: String = "Suzy",
    val activeMoodId: String = "romantic",
    val customNote: String = "",
    val systemPrompt: String = DEFAULT_SUZY_PROMPT,
    val bargeInEnabled: Boolean = true,
    val captionsEnabled: Boolean = true,
    val temperature: Float = 0.95f,
    val callAnnouncerEnabled: Boolean = true,
    val announceCallerName: Boolean = true,
    val announcerRepeatCount: Int = 2,
    val announcerStyle: String = "Girlfriend",
    val notificationReaderEnabled: Boolean = true,
    val readNotificationContent: Boolean = true,
    val backgroundGuardEnabled: Boolean = true
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
        val ACTIVE_MOOD = stringPreferencesKey("active_mood")
        val CUSTOM_NOTE = stringPreferencesKey("custom_user_note")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt_v5")
        val BARGE_IN = booleanPreferencesKey("barge_in")
        val CAPTIONS_ENABLED = booleanPreferencesKey("captions_enabled")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val CALL_ANNOUNCER_ENABLED = booleanPreferencesKey("call_announcer_enabled")
        val ANNOUNCE_CALLER_NAME = booleanPreferencesKey("announce_caller_name")
        val ANNOUNCER_REPEAT = intPreferencesKey("announcer_repeat_count")
        val ANNOUNCER_STYLE = stringPreferencesKey("announcer_style")
        val NOTIF_READER_ENABLED = booleanPreferencesKey("notif_reader_enabled")
        val READ_NOTIF_CONTENT = booleanPreferencesKey("read_notif_content")
        val BACKGROUND_GUARD = booleanPreferencesKey("background_guard_enabled")
        val MEMORIES_JSON = stringPreferencesKey("memory_vault_v1")
        val CUSTOM_PERSONAS_JSON = stringPreferencesKey("custom_personas_v1")
        val TRANSCRIPTS_JSON = stringPreferencesKey("saved_transcripts_v1")
    }

    val settingsFlow: Flow<SuzySettings> = context.dataStore.data.map { prefs ->
        val personaName = prefs[Keys.PERSONA_NAME] ?: "Suzy"
        val storedPrompt = prefs[Keys.SYSTEM_PROMPT] ?: SuzySettings.DEFAULT_SUZY_PROMPT
        val resolvedPrompt = SuzyCatalog.resolveHardcodedPrompt(personaName, storedPrompt)

        SuzySettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            modelId = prefs[Keys.MODEL_ID] ?: "models/gemini-3.8-live",
            voiceName = prefs[Keys.VOICE_NAME] ?: "Aoede",
            activePersonaName = personaName,
            activeMoodId = prefs[Keys.ACTIVE_MOOD] ?: "romantic",
            customNote = prefs[Keys.CUSTOM_NOTE] ?: "",
            systemPrompt = resolvedPrompt,
            bargeInEnabled = prefs[Keys.BARGE_IN] ?: true,
            captionsEnabled = prefs[Keys.CAPTIONS_ENABLED] ?: true,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.95f,
            callAnnouncerEnabled = prefs[Keys.CALL_ANNOUNCER_ENABLED] ?: true,
            announceCallerName = prefs[Keys.ANNOUNCE_CALLER_NAME] ?: true,
            announcerRepeatCount = prefs[Keys.ANNOUNCER_REPEAT] ?: 2,
            announcerStyle = prefs[Keys.ANNOUNCER_STYLE] ?: "Girlfriend",
            notificationReaderEnabled = prefs[Keys.NOTIF_READER_ENABLED] ?: true,
            readNotificationContent = prefs[Keys.READ_NOTIF_CONTENT] ?: true,
            backgroundGuardEnabled = prefs[Keys.BACKGROUND_GUARD] ?: true
        )
    }

    val memoriesFlow: Flow<List<MemoryItem>> = context.dataStore.data.map { prefs ->
        parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
    }

    val customPersonasFlow: Flow<List<PersonaProfile>> = context.dataStore.data.map { prefs ->
        parseCustomPersonas(prefs[Keys.CUSTOM_PERSONAS_JSON] ?: "[]")
    }

    val transcriptsFlow: Flow<List<SavedTranscript>> = context.dataStore.data.map { prefs ->
        parseTranscripts(prefs[Keys.TRANSCRIPTS_JSON] ?: "[]")
    }

    suspend fun saveSettings(settings: SuzySettings) {
        val resolvedPrompt = SuzyCatalog.resolveHardcodedPrompt(
            settings.activePersonaName,
            settings.systemPrompt
        )
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.apiKey.trim()
            prefs[Keys.MODEL_ID] = settings.modelId.trim()
            prefs[Keys.VOICE_NAME] = settings.voiceName.trim()
            prefs[Keys.PERSONA_NAME] = settings.activePersonaName.trim()
            prefs[Keys.ACTIVE_MOOD] = settings.activeMoodId.trim()
            prefs[Keys.CUSTOM_NOTE] = settings.customNote.trim()
            prefs[Keys.SYSTEM_PROMPT] = resolvedPrompt
            prefs[Keys.BARGE_IN] = settings.bargeInEnabled
            prefs[Keys.CAPTIONS_ENABLED] = settings.captionsEnabled
            prefs[Keys.TEMPERATURE] = settings.temperature
            prefs[Keys.CALL_ANNOUNCER_ENABLED] = settings.callAnnouncerEnabled
            prefs[Keys.ANNOUNCE_CALLER_NAME] = settings.announceCallerName
            prefs[Keys.ANNOUNCER_REPEAT] = settings.announcerRepeatCount.coerceIn(1, 3)
            prefs[Keys.ANNOUNCER_STYLE] = settings.announcerStyle
            prefs[Keys.NOTIF_READER_ENABLED] = settings.notificationReaderEnabled
            prefs[Keys.READ_NOTIF_CONTENT] = settings.readNotificationContent
            prefs[Keys.BACKGROUND_GUARD] = settings.backgroundGuardEnabled
        }
    }

    suspend fun addMemory(category: String, content: String) {
        val clean = content.trim()
        if (clean.isBlank()) return
        val ts = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val item = MemoryItem(
            id = UUID.randomUUID().toString(),
            category = category.ifBlank { "Personal" },
            content = clean,
            createdAt = ts
        )
        context.dataStore.edit { prefs ->
            val current = parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
            val updated = (listOf(item) + current).take(50)
            prefs[Keys.MEMORIES_JSON] = serializeMemories(updated)
        }
    }

    suspend fun deleteMemory(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
            prefs[Keys.MEMORIES_JSON] = serializeMemories(current.filterNot { it.id == id })
        }
    }

    suspend fun saveCustomPersona(
        name: String,
        role: String,
        voiceName: String,
        accentHex: Long,
        prompt: String
    ): PersonaProfile? {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null
        val cleanRole = role.trim().ifBlank { "Custom Companion" }
        val newPersona = PersonaProfile(
            id = "custom_${UUID.randomUUID()}",
            name = cleanName,
            role = cleanRole,
            tagline = cleanRole,
            voiceName = voiceName,
            temperature = 0.95f,
            accentColor = Color(accentHex),
            accentColorHex = accentHex,
            icon = Icons.Rounded.AutoAwesome,
            systemPrompt = prompt.trim().ifBlank { SuzySettings.DEFAULT_SUZY_PROMPT },
            isCustom = true
        )
        context.dataStore.edit { prefs ->
            val current = parseCustomPersonas(prefs[Keys.CUSTOM_PERSONAS_JSON] ?: "[]")
            val updated = listOf(newPersona) + current.filterNot { it.name.equals(cleanName, ignoreCase = true) }
            prefs[Keys.CUSTOM_PERSONAS_JSON] = serializeCustomPersonas(updated)
        }
        return newPersona
    }

    suspend fun deleteCustomPersona(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseCustomPersonas(prefs[Keys.CUSTOM_PERSONAS_JSON] ?: "[]")
            prefs[Keys.CUSTOM_PERSONAS_JSON] = serializeCustomPersonas(current.filterNot { it.id == id })
        }
    }

    suspend fun saveTranscript(transcript: SavedTranscript) {
        context.dataStore.edit { prefs ->
            val current = parseTranscripts(prefs[Keys.TRANSCRIPTS_JSON] ?: "[]")
            val updated = (listOf(transcript) + current).take(30)
            prefs[Keys.TRANSCRIPTS_JSON] = serializeTranscripts(updated)
        }
    }

    suspend fun clearTranscripts() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRANSCRIPTS_JSON] = "[]"
        }
    }

    private fun parseMemories(raw: String): List<MemoryItem> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        MemoryItem(
                            id = obj.optString("id"),
                            category = obj.optString("category", "Personal"),
                            content = obj.optString("content"),
                            createdAt = obj.optString("createdAt", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeMemories(list: List<MemoryItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("category", item.category)
                put("content", item.content)
                put("createdAt", item.createdAt)
            })
        }
        return arr.toString()
    }

    private fun parseCustomPersonas(raw: String): List<PersonaProfile> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val hex = obj.optLong("accentColorHex", 0xFFEC4899L)
                    val role = obj.optString("role", "Custom")
                    add(
                        PersonaProfile(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            role = role,
                            tagline = role,
                            voiceName = obj.optString("voiceName", "Aoede"),
                            temperature = obj.optDouble("temperature", 0.95).toFloat(),
                            accentColor = Color(hex),
                            accentColorHex = hex,
                            icon = Icons.Rounded.AutoAwesome,
                            systemPrompt = obj.optString("systemPrompt"),
                            isCustom = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeCustomPersonas(list: List<PersonaProfile>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("role", p.role)
                put("voiceName", p.voiceName)
                put("temperature", p.temperature.toDouble())
                put("accentColorHex", p.accentColorHex)
                put("systemPrompt", p.systemPrompt)
            })
        }
        return arr.toString()
    }

    private fun parseTranscripts(raw: String): List<SavedTranscript> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        SavedTranscript(
                            id = obj.optString("id"),
                            personaName = obj.optString("personaName", "Suzy"),
                            dateTime = obj.optString("dateTime", ""),
                            durationLabel = obj.optString("durationLabel", ""),
                            dialogue = obj.optString("dialogue", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeTranscripts(list: List<SavedTranscript>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("personaName", t.personaName)
                put("dateTime", t.dateTime)
                put("durationLabel", t.durationLabel)
                put("dialogue", t.dialogue)
            })
        }
        return arr.toString()
    }
}
