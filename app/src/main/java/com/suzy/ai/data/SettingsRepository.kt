package com.suzy.ai.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val createdAt: String,
    val pinned: Boolean = false
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
    val backupApiKey: String = "",
    val modelId: String = "models/gemini-3.8-live",
    val voiceName: String = "Aoede",
    val activePersonaName: String = "Suzy",
    val activeMoodId: String = "romantic",
    val customNote: String = "",
    val systemPrompt: String = DEFAULT_SUZY_PROMPT,
    val bargeInEnabled: Boolean = true,
    val captionsEnabled: Boolean = true,
    val temperature: Float = 0.95f,
    // Wake word & Privacy Shields
    val wakeWordEnabled: Boolean = true,
    val customWakePhrase: String = "Hey Suzy",
    val mediaShieldEnabled: Boolean = true,
    val callPrivacyShieldEnabled: Boolean = true,
    val proximityEarRoutingEnabled: Boolean = true,
    val callAnnouncerEnabled: Boolean = true,
    val announceCallerName: Boolean = true,
    val announcerRepeatCount: Int = 2,
    val announcerStyle: String = "Girlfriend",
    val notificationReaderEnabled: Boolean = true,
    val readNotificationContent: Boolean = true,
    val blockSensitiveNotifications: Boolean = true,
    val hideNotificationOnLockScreen: Boolean = true,
    val trustedAppsOnly: Boolean = true,
    val backgroundGuardEnabled: Boolean = true,
    // 3D ORB Morphing & FX (109-114)
    val orbStyle: String = "plasma",
    val orbPrimaryHex: Long = 0xFFA855F7L,
    val orbSecondaryHex: Long = 0xFF22D3EEL,
    val orbGlowIntensity: Float = 1.0f,
    val orbSpinSpeed: Float = 1.0f,
    val moodAutoOrbSync: Boolean = true,
    val backdropPreset: String = "starfield",
    val hapticMode: String = "Crisp",
    // Live UI Design Morphing (121-125)
    val uiThemeSkin: String = "cyberpunk",
    val layoutMode: String = "standard",
    val cornerRadiusDp: Int = 20,
    val fontScale: Float = 1.0f,
    val captionPosition: String = "below_orb",
    // Owner Identity, Voice ID & Security (25-30, 115-120)
    val ownerName: String = "Shahid",
    val ownerNickname: String = "Boss",
    val ownerPassphrase: String = "omega",
    val ownerPin: String = "1234",
    val ownerModeStrict: Boolean = false,
    val ownerVoiceEnrolled: Boolean = false,
    val ownerVoicePitchHz: Float = 135f,
    val ownerVoiceEnergy: Float = 0.5f,
    val guestModeActive: Boolean = false,
    val stealthCodeword: String = "code zero",
    // Emotional Attitude, Anger & Trust Engine (142, 145-150)
    val angerLevel: Int = 0,
    val angerSensitivity: Float = 0.6f,
    val roastIntensity: String = "Light",
    val jealousyLevel: String = "Cute",
    val trustScore: Int = 85,
    val bondXp: Int = 340,
    val streakDays: Int = 5,
    // Hardware Gestures, Audio Lab & Triggers (8, 10, 17, 97, 98, 134, 137)
    val shakeToCallEnabled: Boolean = false,
    val flipToMuteEnabled: Boolean = true,
    val chargerReactionEnabled: Boolean = true,
    val lowBatteryThreshold: Int = 15,
    val thermalAlertEnabled: Boolean = true,
    val loudnessBoostDb: Int = 0,
    val bluetoothAutoMode: Boolean = true,
    val vipOnlyNightCalls: Boolean = false,
    val activeAudioProfile: String = "Normal"
) {
    companion object {
        val DEFAULT_SUZY_PROMPT = SuzyCatalog.personas.first().systemPrompt
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BACKUP_API_KEY = stringPreferencesKey("backup_api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val PERSONA_NAME = stringPreferencesKey("persona_name")
        val ACTIVE_MOOD = stringPreferencesKey("active_mood")
        val CUSTOM_NOTE = stringPreferencesKey("custom_user_note")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt_v5")
        val BARGE_IN = booleanPreferencesKey("barge_in")
        val CAPTIONS_ENABLED = booleanPreferencesKey("captions_enabled")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val CUSTOM_WAKE_PHRASE = stringPreferencesKey("custom_wake_phrase")
        val MEDIA_SHIELD_ENABLED = booleanPreferencesKey("media_shield_enabled")
        val CALL_PRIVACY_SHIELD_ENABLED = booleanPreferencesKey("call_privacy_shield_enabled")
        val PROXIMITY_EAR = booleanPreferencesKey("proximity_ear")
        val CALL_ANNOUNCER_ENABLED = booleanPreferencesKey("call_announcer_enabled")
        val ANNOUNCE_CALLER_NAME = booleanPreferencesKey("announce_caller_name")
        val ANNOUNCER_REPEAT = intPreferencesKey("announcer_repeat_count")
        val ANNOUNCER_STYLE = stringPreferencesKey("announcer_style")
        val NOTIF_READER_ENABLED = booleanPreferencesKey("notif_reader_enabled")
        val READ_NOTIF_CONTENT = booleanPreferencesKey("read_notif_content")
        val BLOCK_SENSITIVE_NOTIF = booleanPreferencesKey("block_sensitive_notif")
        val HIDE_NOTIF_LOCK_SCREEN = booleanPreferencesKey("hide_notif_lock_screen")
        val TRUSTED_APPS_ONLY = booleanPreferencesKey("trusted_apps_only")
        val BACKGROUND_GUARD = booleanPreferencesKey("background_guard_enabled")

        val ORB_STYLE = stringPreferencesKey("orb_style")
        val ORB_PRIMARY_HEX = longPreferencesKey("orb_primary_hex")
        val ORB_SECONDARY_HEX = longPreferencesKey("orb_secondary_hex")
        val ORB_GLOW = floatPreferencesKey("orb_glow")
        val ORB_SPIN = floatPreferencesKey("orb_spin")
        val MOOD_AUTO_ORB = booleanPreferencesKey("mood_auto_orb")
        val BACKDROP_PRESET = stringPreferencesKey("backdrop_preset")
        val HAPTIC_MODE = stringPreferencesKey("haptic_mode")

        val UI_THEME_SKIN = stringPreferencesKey("ui_theme_skin")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CAPTION_POS = stringPreferencesKey("caption_pos")

        val OWNER_NAME = stringPreferencesKey("owner_name")
        val OWNER_NICKNAME = stringPreferencesKey("owner_nickname")
        val OWNER_PASSPHRASE = stringPreferencesKey("owner_passphrase")
        val OWNER_PIN = stringPreferencesKey("owner_pin")
        val OWNER_MODE_STRICT = booleanPreferencesKey("owner_mode_strict")
        val OWNER_VOICE_ENROLLED = booleanPreferencesKey("owner_voice_enrolled")
        val OWNER_VOICE_PITCH = floatPreferencesKey("owner_voice_pitch")
        val OWNER_VOICE_ENERGY = floatPreferencesKey("owner_voice_energy")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val STEALTH_CODEWORD = stringPreferencesKey("stealth_codeword")

        val ANGER_LEVEL = intPreferencesKey("anger_level")
        val ANGER_SENSITIVITY = floatPreferencesKey("anger_sensitivity")
        val ROAST_INTENSITY = stringPreferencesKey("roast_intensity")
        val JEALOUSY_LEVEL = stringPreferencesKey("jealousy_level")
        val TRUST_SCORE = intPreferencesKey("trust_score")
        val BOND_XP = intPreferencesKey("bond_xp")
        val STREAK_DAYS = intPreferencesKey("streak_days")

        val SHAKE_TO_CALL = booleanPreferencesKey("shake_to_call")
        val FLIP_TO_MUTE = booleanPreferencesKey("flip_to_mute")
        val CHARGER_REACTION = booleanPreferencesKey("charger_reaction")
        val LOW_BAT_THRESHOLD = intPreferencesKey("low_bat_threshold")
        val THERMAL_ALERT = booleanPreferencesKey("thermal_alert")
        val LOUDNESS_BOOST = intPreferencesKey("loudness_boost")
        val BT_AUTO_MODE = booleanPreferencesKey("bt_auto_mode")
        val VIP_NIGHT_CALLS = booleanPreferencesKey("vip_night_calls")
        val AUDIO_PROFILE = stringPreferencesKey("audio_profile")

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
            backupApiKey = prefs[Keys.BACKUP_API_KEY] ?: "",
            modelId = prefs[Keys.MODEL_ID] ?: "models/gemini-3.8-live",
            voiceName = prefs[Keys.VOICE_NAME] ?: "Aoede",
            activePersonaName = personaName,
            activeMoodId = prefs[Keys.ACTIVE_MOOD] ?: "romantic",
            customNote = prefs[Keys.CUSTOM_NOTE] ?: "",
            systemPrompt = resolvedPrompt,
            bargeInEnabled = prefs[Keys.BARGE_IN] ?: true,
            captionsEnabled = prefs[Keys.CAPTIONS_ENABLED] ?: true,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.95f,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
            customWakePhrase = prefs[Keys.CUSTOM_WAKE_PHRASE] ?: "Hey Suzy",
            mediaShieldEnabled = prefs[Keys.MEDIA_SHIELD_ENABLED] ?: true,
            callPrivacyShieldEnabled = prefs[Keys.CALL_PRIVACY_SHIELD_ENABLED] ?: true,
            proximityEarRoutingEnabled = prefs[Keys.PROXIMITY_EAR] ?: true,
            callAnnouncerEnabled = prefs[Keys.CALL_ANNOUNCER_ENABLED] ?: true,
            announceCallerName = prefs[Keys.ANNOUNCE_CALLER_NAME] ?: true,
            announcerRepeatCount = prefs[Keys.ANNOUNCER_REPEAT] ?: 2,
            announcerStyle = prefs[Keys.ANNOUNCER_STYLE] ?: "Girlfriend",
            notificationReaderEnabled = prefs[Keys.NOTIF_READER_ENABLED] ?: true,
            readNotificationContent = prefs[Keys.READ_NOTIF_CONTENT] ?: true,
            blockSensitiveNotifications = prefs[Keys.BLOCK_SENSITIVE_NOTIF] ?: true,
            hideNotificationOnLockScreen = prefs[Keys.HIDE_NOTIF_LOCK_SCREEN] ?: true,
            trustedAppsOnly = prefs[Keys.TRUSTED_APPS_ONLY] ?: true,
            backgroundGuardEnabled = prefs[Keys.BACKGROUND_GUARD] ?: true,
            orbStyle = prefs[Keys.ORB_STYLE] ?: "plasma",
            orbPrimaryHex = prefs[Keys.ORB_PRIMARY_HEX] ?: 0xFFA855F7L,
            orbSecondaryHex = prefs[Keys.ORB_SECONDARY_HEX] ?: 0xFF22D3EEL,
            orbGlowIntensity = prefs[Keys.ORB_GLOW] ?: 1.0f,
            orbSpinSpeed = prefs[Keys.ORB_SPIN] ?: 1.0f,
            moodAutoOrbSync = prefs[Keys.MOOD_AUTO_ORB] ?: true,
            backdropPreset = prefs[Keys.BACKDROP_PRESET] ?: "starfield",
            hapticMode = prefs[Keys.HAPTIC_MODE] ?: "Crisp",
            uiThemeSkin = prefs[Keys.UI_THEME_SKIN] ?: "cyberpunk",
            layoutMode = prefs[Keys.LAYOUT_MODE] ?: "standard",
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 20,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            captionPosition = prefs[Keys.CAPTION_POS] ?: "below_orb",
            ownerName = prefs[Keys.OWNER_NAME] ?: "Shahid",
            ownerNickname = prefs[Keys.OWNER_NICKNAME] ?: "Boss",
            ownerPassphrase = prefs[Keys.OWNER_PASSPHRASE] ?: "omega",
            ownerPin = prefs[Keys.OWNER_PIN] ?: "1234",
            ownerModeStrict = prefs[Keys.OWNER_MODE_STRICT] ?: false,
            ownerVoiceEnrolled = prefs[Keys.OWNER_VOICE_ENROLLED] ?: false,
            ownerVoicePitchHz = prefs[Keys.OWNER_VOICE_PITCH] ?: 135f,
            ownerVoiceEnergy = prefs[Keys.OWNER_VOICE_ENERGY] ?: 0.5f,
            guestModeActive = prefs[Keys.GUEST_MODE] ?: false,
            stealthCodeword = prefs[Keys.STEALTH_CODEWORD] ?: "code zero",
            angerLevel = prefs[Keys.ANGER_LEVEL] ?: 0,
            angerSensitivity = prefs[Keys.ANGER_SENSITIVITY] ?: 0.6f,
            roastIntensity = prefs[Keys.ROAST_INTENSITY] ?: "Light",
            jealousyLevel = prefs[Keys.JEALOUSY_LEVEL] ?: "Cute",
            trustScore = prefs[Keys.TRUST_SCORE] ?: 85,
            bondXp = prefs[Keys.BOND_XP] ?: 340,
            streakDays = prefs[Keys.STREAK_DAYS] ?: 5,
            shakeToCallEnabled = prefs[Keys.SHAKE_TO_CALL] ?: false,
            flipToMuteEnabled = prefs[Keys.FLIP_TO_MUTE] ?: true,
            chargerReactionEnabled = prefs[Keys.CHARGER_REACTION] ?: true,
            lowBatteryThreshold = prefs[Keys.LOW_BAT_THRESHOLD] ?: 15,
            thermalAlertEnabled = prefs[Keys.THERMAL_ALERT] ?: true,
            loudnessBoostDb = prefs[Keys.LOUDNESS_BOOST] ?: 0,
            bluetoothAutoMode = prefs[Keys.BT_AUTO_MODE] ?: true,
            vipOnlyNightCalls = prefs[Keys.VIP_NIGHT_CALLS] ?: false,
            activeAudioProfile = prefs[Keys.AUDIO_PROFILE] ?: "Normal"
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
            prefs[Keys.BACKUP_API_KEY] = settings.backupApiKey.trim()
            prefs[Keys.MODEL_ID] = settings.modelId.trim()
            prefs[Keys.VOICE_NAME] = settings.voiceName.trim()
            prefs[Keys.PERSONA_NAME] = settings.activePersonaName.trim()
            prefs[Keys.ACTIVE_MOOD] = settings.activeMoodId.trim()
            prefs[Keys.CUSTOM_NOTE] = settings.customNote.trim()
            prefs[Keys.SYSTEM_PROMPT] = resolvedPrompt
            prefs[Keys.BARGE_IN] = settings.bargeInEnabled
            prefs[Keys.CAPTIONS_ENABLED] = settings.captionsEnabled
            prefs[Keys.TEMPERATURE] = settings.temperature
            prefs[Keys.WAKE_WORD_ENABLED] = settings.wakeWordEnabled
            prefs[Keys.CUSTOM_WAKE_PHRASE] = settings.customWakePhrase.trim().ifBlank { "Hey Suzy" }
            prefs[Keys.MEDIA_SHIELD_ENABLED] = settings.mediaShieldEnabled
            prefs[Keys.CALL_PRIVACY_SHIELD_ENABLED] = settings.callPrivacyShieldEnabled
            prefs[Keys.PROXIMITY_EAR] = settings.proximityEarRoutingEnabled
            prefs[Keys.CALL_ANNOUNCER_ENABLED] = settings.callAnnouncerEnabled
            prefs[Keys.ANNOUNCE_CALLER_NAME] = settings.announceCallerName
            prefs[Keys.ANNOUNCER_REPEAT] = settings.announcerRepeatCount.coerceIn(1, 3)
            prefs[Keys.ANNOUNCER_STYLE] = settings.announcerStyle
            prefs[Keys.NOTIF_READER_ENABLED] = settings.notificationReaderEnabled
            prefs[Keys.READ_NOTIF_CONTENT] = settings.readNotificationContent
            prefs[Keys.BLOCK_SENSITIVE_NOTIF] = settings.blockSensitiveNotifications
            prefs[Keys.HIDE_NOTIF_LOCK_SCREEN] = settings.hideNotificationOnLockScreen
            prefs[Keys.TRUSTED_APPS_ONLY] = settings.trustedAppsOnly
            prefs[Keys.BACKGROUND_GUARD] = settings.backgroundGuardEnabled

            prefs[Keys.ORB_STYLE] = settings.orbStyle
            prefs[Keys.ORB_PRIMARY_HEX] = settings.orbPrimaryHex
            prefs[Keys.ORB_SECONDARY_HEX] = settings.orbSecondaryHex
            prefs[Keys.ORB_GLOW] = settings.orbGlowIntensity.coerceIn(0.3f, 2.0f)
            prefs[Keys.ORB_SPIN] = settings.orbSpinSpeed.coerceIn(0.2f, 2.5f)
            prefs[Keys.MOOD_AUTO_ORB] = settings.moodAutoOrbSync
            prefs[Keys.BACKDROP_PRESET] = settings.backdropPreset
            prefs[Keys.HAPTIC_MODE] = settings.hapticMode

            prefs[Keys.UI_THEME_SKIN] = settings.uiThemeSkin
            prefs[Keys.LAYOUT_MODE] = settings.layoutMode
            prefs[Keys.CORNER_RADIUS] = settings.cornerRadiusDp.coerceIn(6, 32)
            prefs[Keys.FONT_SCALE] = settings.fontScale.coerceIn(0.85f, 1.35f)
            prefs[Keys.CAPTION_POS] = settings.captionPosition

            prefs[Keys.OWNER_NAME] = settings.ownerName.trim()
            prefs[Keys.OWNER_NICKNAME] = settings.ownerNickname.trim()
            prefs[Keys.OWNER_PASSPHRASE] = settings.ownerPassphrase.trim()
            prefs[Keys.OWNER_PIN] = settings.ownerPin.trim()
            prefs[Keys.OWNER_MODE_STRICT] = settings.ownerModeStrict
            prefs[Keys.OWNER_VOICE_ENROLLED] = settings.ownerVoiceEnrolled
            prefs[Keys.OWNER_VOICE_PITCH] = settings.ownerVoicePitchHz
            prefs[Keys.OWNER_VOICE_ENERGY] = settings.ownerVoiceEnergy
            prefs[Keys.GUEST_MODE] = settings.guestModeActive
            prefs[Keys.STEALTH_CODEWORD] = settings.stealthCodeword.trim()

            prefs[Keys.ANGER_LEVEL] = settings.angerLevel.coerceIn(0, 100)
            prefs[Keys.ANGER_SENSITIVITY] = settings.angerSensitivity
            prefs[Keys.ROAST_INTENSITY] = settings.roastIntensity
            prefs[Keys.JEALOUSY_LEVEL] = settings.jealousyLevel
            prefs[Keys.TRUST_SCORE] = settings.trustScore.coerceIn(0, 100)
            prefs[Keys.BOND_XP] = settings.bondXp.coerceAtLeast(0)
            prefs[Keys.STREAK_DAYS] = settings.streakDays.coerceAtLeast(1)

            prefs[Keys.SHAKE_TO_CALL] = settings.shakeToCallEnabled
            prefs[Keys.FLIP_TO_MUTE] = settings.flipToMuteEnabled
            prefs[Keys.CHARGER_REACTION] = settings.chargerReactionEnabled
            prefs[Keys.LOW_BAT_THRESHOLD] = settings.lowBatteryThreshold.coerceIn(5, 40)
            prefs[Keys.THERMAL_ALERT] = settings.thermalAlertEnabled
            prefs[Keys.LOUDNESS_BOOST] = settings.loudnessBoostDb.coerceIn(0, 1500)
            prefs[Keys.BT_AUTO_MODE] = settings.bluetoothAutoMode
            prefs[Keys.VIP_NIGHT_CALLS] = settings.vipOnlyNightCalls
            prefs[Keys.AUDIO_PROFILE] = settings.activeAudioProfile
        }
    }

    suspend fun addMemory(category: String, content: String, pinned: Boolean = false) {
        val clean = content.trim()
        if (clean.isBlank()) return
        val ts = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val item = MemoryItem(
            id = UUID.randomUUID().toString(),
            category = category.ifBlank { "Personal" },
            content = clean,
            createdAt = ts,
            pinned = pinned
        )
        context.dataStore.edit { prefs ->
            val current = parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
            if (current.any { it.content.equals(clean, ignoreCase = true) }) return@edit
            val updated = (listOf(item) + current).take(80)
            prefs[Keys.MEMORIES_JSON] = serializeMemories(updated)
        }
    }

    suspend fun deleteMemory(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
            prefs[Keys.MEMORIES_JSON] = serializeMemories(current.filterNot { it.id == id })
        }
    }

    suspend fun togglePinMemory(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseMemories(prefs[Keys.MEMORIES_JSON] ?: "[]")
            val updated = current.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
                .sortedByDescending { it.pinned }
            prefs[Keys.MEMORIES_JSON] = serializeMemories(updated)
        }
    }

    suspend fun saveCustomPersona(
        name: String,
        role: String,
        voiceName: String,
        accentHex: Long,
        prompt: String,
        gender: String = "Female"
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
            isCustom = true,
            gender = gender
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
            val updated = (listOf(transcript) + current).take(40)
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
                            createdAt = obj.optString("createdAt", ""),
                            pinned = obj.optBoolean("pinned", false)
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
                put("pinned", item.pinned)
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
                            isCustom = true,
                            gender = obj.optString("gender", "Female")
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
                put("gender", p.gender)
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
