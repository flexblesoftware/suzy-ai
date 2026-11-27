package com.suzy.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suzy.ai.audio.AudioEngine
import com.suzy.ai.data.MemoryItem
import com.suzy.ai.data.MoodOption
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.SavedTranscript
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.network.GeminiLiveClient
import com.suzy.ai.service.CallServiceBridge
import com.suzy.ai.tools.SuzyTools
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SuzyState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class SuzyUiState(
    val state: SuzyState = SuzyState.IDLE,
    val statusText: String = "Tap Start Call to talk",
    val liveCaption: String = "",
    val lastCaption: String = "",
    val amplitude: Float = 0f,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val callDurationSec: Long = 0L,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val toolFeedback: String = ""
)

class SuzyViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repo = SettingsRepository(appContext)
    private val audioEngine = AudioEngine(appContext, viewModelScope)
    private val suzyTools = SuzyTools(appContext)

    val settings: StateFlow<SuzySettings> = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SuzySettings()
    )

    val memories: StateFlow<List<MemoryItem>> = repo.memoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val customPersonas: StateFlow<List<PersonaProfile>> = repo.customPersonasFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val transcripts: StateFlow<List<SavedTranscript>> = repo.transcriptsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(SuzyUiState())
    val uiState: StateFlow<SuzyUiState> = _uiState.asStateFlow()

    @Volatile
    private var sessionGeneration: Long = 0L

    @Volatile
    private var liveClient: GeminiLiveClient? = null

    private var timerJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private val sessionTurnLines = mutableListOf<String>()

    init {
        CallServiceBridge.onToggleMuteRequested = { toggleMute() }
        CallServiceBridge.onStopCallRequested = { stopSession() }
        refreshDeviceTelemetry()
    }

    fun refreshDeviceTelemetry() {
        val bat = suzyTools.getBatteryTelemetry()
        _uiState.update {
            it.copy(
                batteryPercent = bat.percent,
                isCharging = bat.isCharging,
                isFlashlightOn = suzyTools.isTorchOn
            )
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        if (sessionStartMillis == 0L) {
            sessionStartMillis = System.currentTimeMillis()
        }
        timerJob = viewModelScope.launch {
            while (isActive) {
                val sec = ((System.currentTimeMillis() - sessionStartMillis) / 1000L).coerceAtLeast(0L)
                _uiState.update { it.copy(callDurationSec = sec) }
                delay(1000L)
            }
        }
    }

    private fun stopCallTimer() {
        timerJob?.cancel()
        timerJob = null
        sessionStartMillis = 0L
        _uiState.update { it.copy(callDurationSec = 0L) }
    }

    private fun buildEnrichedSettings(base: SuzySettings): SuzySettings {
        val hardcodedBasePrompt = SuzyCatalog.resolveHardcodedPrompt(base.activePersonaName, base.systemPrompt)
        val mood = SuzyCatalog.moods.firstOrNull { it.id == base.activeMoodId }
        val savedMemories = memories.value

        val promptBuilder = StringBuilder(hardcodedBasePrompt.trim())
        if (base.customNote.isNotBlank()) {
            promptBuilder.append("\n\n[USER PERSONAL INSTRUCTION]: ${base.customNote.trim()}")
        }
        if (mood != null) {
            promptBuilder.append("\n\n[CURRENT LIVE MOOD - ${mood.label}]: ${mood.instruction}")
        }
        if (savedMemories.isNotEmpty()) {
            promptBuilder.append("\n\n[USER PERSONAL MEMORY VAULT - Hamesha yaad rakho aur naturally baat mein use karo]:")
            savedMemories.take(25).forEach { mem ->
                promptBuilder.append("\n- (${mem.category}) ${mem.content}")
            }
        }
        return base.copy(systemPrompt = promptBuilder.toString())
    }

    private fun persistCurrentTranscriptIfAny() {
        if (sessionTurnLines.isEmpty()) return
        val linesSnapshot = sessionTurnLines.toList()
        sessionTurnLines.clear()

        val elapsedSec = if (sessionStartMillis > 0L) {
            ((System.currentTimeMillis() - sessionStartMillis) / 1000L).coerceAtLeast(1L)
        } else 1L
        val mins = elapsedSec / 60L
        val secs = elapsedSec % 60L
        val durationStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val personaName = settings.value.activePersonaName

        val item = SavedTranscript(
            id = UUID.randomUUID().toString(),
            personaName = personaName,
            dateTime = dateStr,
            durationLabel = durationStr,
            dialogue = linesSnapshot.joinToString("\n")
        )
        viewModelScope.launch {
            repo.saveTranscript(item)
        }
    }

    private fun createClientForGeneration(gen: Long, targetSettings: SuzySettings): GeminiLiveClient {
        return GeminiLiveClient(
            onConnected = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                val activeName = targetSettings.activePersonaName
                startCallTimer()
                audioEngine.setSpeakerphone(_uiState.value.isSpeakerOn)
                audioEngine.playUiChime(true)
                audioEngine.startPlayback()
                audioEngine.startRecording(
                    onAudioChunk = { pcm ->
                        if (gen == sessionGeneration) {
                            liveClient?.sendPcmAudio(pcm)
                        }
                    },
                    onAmplitude = { amp ->
                        if (gen == sessionGeneration) {
                            _uiState.update { cur ->
                                if (cur.state == SuzyState.LISTENING) cur.copy(amplitude = amp) else cur
                            }
                        }
                    }
                )
                _uiState.update {
                    it.copy(state = SuzyState.LISTENING, statusText = "$activeName sun rahi hai...")
                }
                CallServiceBridge.updateNotification(
                    appContext,
                    activeName,
                    "$activeName sun rahi hai...",
                    _uiState.value.isMuted
                )
            },
            onAudioOutput = { pcm ->
                if (gen != sessionGeneration) return@GeminiLiveClient
                val activeName = targetSettings.activePersonaName
                _uiState.update {
                    it.copy(state = SuzyState.SPEAKING, statusText = "$activeName bol rahi hai...")
                }
                audioEngine.playPcmChunk(pcm) { amp ->
                    if (gen == sessionGeneration) {
                        _uiState.update { it.copy(amplitude = amp) }
                    }
                }
            },
            onTextOutput = { text ->
                if (gen != sessionGeneration) return@GeminiLiveClient
                _uiState.update { cur ->
                    val combined = (cur.liveCaption + text).takeLast(260)
                    cur.copy(liveCaption = combined, lastCaption = combined)
                }
            },
            onToolCall = { callId: String, name: String, args: JSONObject ->
                if (gen != sessionGeneration) return@GeminiLiveClient
                val result = suzyTools.executeTool(name, args)
                refreshDeviceTelemetry()
                _uiState.update { it.copy(toolFeedback = result) }
                liveClient?.sendToolResponse(callId, name, result)
            },
            onInterrupted = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                val activeName = targetSettings.activePersonaName
                if (targetSettings.bargeInEnabled) {
                    audioEngine.flushPlayback()
                }
                _uiState.update {
                    it.copy(state = SuzyState.LISTENING, statusText = "$activeName sun rahi hai...", amplitude = 0.1f)
                }
            },
            onTurnComplete = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                val activeName = targetSettings.activePersonaName
                val caption = _uiState.value.liveCaption.trim()
                if (caption.isNotBlank()) {
                    val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    sessionTurnLines.add("[$ts] $activeName: $caption")
                }
                _uiState.update {
                    it.copy(
                        state = SuzyState.LISTENING,
                        statusText = "$activeName sun rahi hai...",
                        lastCaption = if (caption.isNotBlank()) caption else it.lastCaption,
                        liveCaption = "",
                        amplitude = 0.05f
                    )
                }
            },
            onError = { err ->
                if (gen != sessionGeneration) return@GeminiLiveClient
                audioEngine.releaseAll()
                persistCurrentTranscriptIfAny()
                stopCallTimer()
                CallServiceBridge.stopService(appContext)
                _uiState.update {
                    it.copy(state = SuzyState.ERROR, statusText = "Error: $err", amplitude = 0f)
                }
            },
            onClosed = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                audioEngine.releaseAll()
                persistCurrentTranscriptIfAny()
                stopCallTimer()
                CallServiceBridge.stopService(appContext)
                _uiState.update { cur ->
                    if (cur.state != SuzyState.ERROR) {
                        cur.copy(
                            state = SuzyState.IDLE,
                            statusText = "Tap Start Call to talk",
                            amplitude = 0f
                        )
                    } else cur
                }
            }
        )
    }

    private fun isSessionActive(): Boolean {
        val st = _uiState.value.state
        return st == SuzyState.CONNECTING || st == SuzyState.LISTENING || st == SuzyState.SPEAKING
    }

    fun startSession(customSettings: SuzySettings? = null) {
        val baseSettings = customSettings ?: settings.value
        if (baseSettings.apiKey.isBlank()) {
            _uiState.update {
                it.copy(state = SuzyState.ERROR, statusText = "Pehle AI Config mein Gemini API Key daalo")
            }
            return
        }
        val gen = ++sessionGeneration
        liveClient?.disconnect()
        audioEngine.flushPlayback()

        val enrichedSettings = buildEnrichedSettings(baseSettings)
        _uiState.update {
            it.copy(
                state = SuzyState.CONNECTING,
                statusText = "${baseSettings.activePersonaName} connect ho rahi hai...",
                liveCaption = "",
                lastCaption = ""
            )
        }
        CallServiceBridge.startService(
            appContext,
            baseSettings.activePersonaName,
            "Connecting live voice...",
            _uiState.value.isMuted
        )
        val client = createClientForGeneration(gen, enrichedSettings)
        liveClient = client
        client.connect(enrichedSettings)
    }

    fun stopSession() {
        ++sessionGeneration
        audioEngine.playUiChime(false)
        liveClient?.disconnect()
        liveClient = null
        audioEngine.releaseAll()
        persistCurrentTranscriptIfAny()
        stopCallTimer()
        CallServiceBridge.stopService(appContext)
        _uiState.update {
            it.copy(state = SuzyState.IDLE, statusText = "Tap Start Call to talk", amplitude = 0f)
        }
    }

    private fun hotReloadIfActive(updatedSettings: SuzySettings) {
        if (isSessionActive()) {
            startSession(updatedSettings)
        } else {
            _uiState.update {
                it.copy(statusText = "${updatedSettings.activePersonaName} (${updatedSettings.voiceName}) ready")
            }
        }
    }

    fun selectPersona(persona: PersonaProfile) {
        val updated = settings.value.copy(
            activePersonaName = persona.name,
            voiceName = persona.voiceName,
            systemPrompt = persona.systemPrompt,
            temperature = persona.temperature
        )
        viewModelScope.launch {
            repo.saveSettings(updated)
            hotReloadIfActive(updated)
        }
    }

    fun createCustomPersona(
        name: String,
        role: String,
        voiceName: String,
        accentHex: Long,
        prompt: String
    ) {
        viewModelScope.launch {
            val created = repo.saveCustomPersona(name, role, voiceName, accentHex, prompt)
            if (created != null) {
                val updated = settings.value.copy(
                    activePersonaName = created.name,
                    voiceName = created.voiceName,
                    systemPrompt = created.systemPrompt,
                    temperature = created.temperature
                )
                repo.saveSettings(updated)
                hotReloadIfActive(updated)
            }
        }
    }

    fun deleteCustomPersona(id: String) {
        viewModelScope.launch {
            repo.deleteCustomPersona(id)
        }
    }

    fun selectMood(mood: MoodOption) {
        val updated = settings.value.copy(activeMoodId = mood.id)
        viewModelScope.launch {
            repo.saveSettings(updated)
            if (isSessionActive()) {
                liveClient?.sendReactionPrompt(
                    "Apna mood turant '${mood.label}' par shift karo: ${mood.instruction} Isi mood mein 1 chhoti cute line bolo."
                )
            }
        }
    }

    fun selectVoice(voiceName: String) {
        val updated = settings.value.copy(voiceName = voiceName)
        viewModelScope.launch {
            repo.saveSettings(updated)
            hotReloadIfActive(updated)
        }
    }

    fun toggleCaptions() {
        val updated = settings.value.copy(captionsEnabled = !settings.value.captionsEnabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
        }
    }

    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            repo.addMemory(category, content)
            if (isSessionActive()) {
                liveClient?.sendReactionPrompt(
                    "Yeh nayi baat hamesha ke liye yaad kar lo ($category): $content. Bas pyaar se chhota sa 'Haan yaad rakhungi' bolo."
                )
            }
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            repo.deleteMemory(id)
        }
    }

    fun clearTranscripts() {
        viewModelScope.launch {
            repo.clearTranscripts()
        }
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        audioEngine.isMuted = next
        val activeName = settings.value.activePersonaName
        val newStatus = if (next) "Mic muted hai" else "$activeName sun rahi hai..."
        _uiState.update {
            it.copy(isMuted = next, statusText = newStatus)
        }
        CallServiceBridge.updateNotification(appContext, activeName, newStatus, next)
    }

    fun toggleSpeaker() {
        val next = !_uiState.value.isSpeakerOn
        audioEngine.setSpeakerphone(next)
        _uiState.update { it.copy(isSpeakerOn = next) }
    }

    fun toggleFlashlightManual() {
        val next = !suzyTools.isTorchOn
        val res = suzyTools.toggleFlashlight(next)
        _uiState.update { it.copy(isFlashlightOn = suzyTools.isTorchOn, toolFeedback = res) }
    }

    fun openAppManual(appName: String) {
        val res = suzyTools.openApp(appName)
        _uiState.update { it.copy(toolFeedback = res) }
    }

    fun sendQuickReaction(prompt: String) {
        audioEngine.flushPlayback()
        liveClient?.sendReactionPrompt(prompt)
    }

    fun interruptSuzy() {
        audioEngine.flushPlayback()
        _uiState.update { it.copy(state = SuzyState.LISTENING, statusText = "${settings.value.activePersonaName} sun rahi hai...") }
    }

    fun updateSettings(updated: SuzySettings) {
        val resolved = updated.copy(
            systemPrompt = SuzyCatalog.resolveHardcodedPrompt(updated.activePersonaName, updated.systemPrompt)
        )
        viewModelScope.launch {
            repo.saveSettings(resolved)
            hotReloadIfActive(resolved)
        }
    }

    override fun onCleared() {
        super.onCleared()
        CallServiceBridge.onToggleMuteRequested = null
        CallServiceBridge.onStopCallRequested = null
        stopSession()
    }
}
