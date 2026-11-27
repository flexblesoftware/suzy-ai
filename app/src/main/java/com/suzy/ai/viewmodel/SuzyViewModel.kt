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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

data class LogEntry(
    val time: String,
    val source: String,
    val message: String
)

data class SuzyUiState(
    val state: SuzyState = SuzyState.IDLE,
    val statusText: String = "Tap Start to talk",
    val liveCaption: String = "",
    val lastCaption: String = "",
    val amplitude: Float = 0f,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val logs: List<LogEntry> = emptyList()
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

    private var sessionStartMillis: Long = 0L
    private val sessionTurnLines = mutableListOf<String>()

    init {
        CallServiceBridge.onToggleMuteRequested = { toggleMute() }
        CallServiceBridge.onStopCallRequested = { stopSession() }
    }

    private fun appendLog(source: String, msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _uiState.update { current ->
            current.copy(logs = (current.logs + LogEntry(ts, source, msg)).takeLast(80))
        }
    }

    private fun buildEnrichedSettings(base: SuzySettings): SuzySettings {
        val mood = SuzyCatalog.moods.firstOrNull { it.id == base.activeMoodId }
        val savedMemories = memories.value

        val promptBuilder = StringBuilder(base.systemPrompt.trim())
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

    private val liveClient = GeminiLiveClient(
        onConnected = {
            val activeName = settings.value.activePersonaName
            sessionStartMillis = System.currentTimeMillis()
            sessionTurnLines.clear()

            audioEngine.setSpeakerphone(_uiState.value.isSpeakerOn)
            audioEngine.playUiChime(true)
            audioEngine.startPlayback()
            audioEngine.startRecording(
                onAudioChunk = { pcm -> liveClientSend(pcm) },
                onAmplitude = { amp ->
                    _uiState.update { cur ->
                        if (cur.state == SuzyState.LISTENING) cur.copy(amplitude = amp) else cur
                    }
                }
            )
            appendLog("SYSTEM", "$activeName (${settings.value.voiceName}) connected • Screen-off call active")
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
            val activeName = settings.value.activePersonaName
            _uiState.update {
                it.copy(state = SuzyState.SPEAKING, statusText = "$activeName bol rahi hai...")
            }
            audioEngine.playPcmChunk(pcm) { amp ->
                _uiState.update { it.copy(amplitude = amp) }
            }
        },
        onTextOutput = { text ->
            _uiState.update { cur ->
                val combined = (cur.liveCaption + text).takeLast(260)
                cur.copy(liveCaption = combined, lastCaption = combined)
            }
        },
        onToolCall = { callId, name, args ->
            val result = suzyTools.executeTool(name, args)
            appendLog("JARVIS TOOL", "$name -> $result")
            _uiState.update { it.copy(isFlashlightOn = suzyTools.isTorchOn) }
            liveClientSendToolResponse(callId, name, result)
        },
        onInterrupted = {
            val activeName = settings.value.activePersonaName
            if (settings.value.bargeInEnabled) {
                audioEngine.flushPlayback()
            }
            _uiState.update {
                it.copy(state = SuzyState.LISTENING, statusText = "$activeName sun rahi hai...", amplitude = 0.1f)
            }
        },
        onTurnComplete = {
            val activeName = settings.value.activePersonaName
            val caption = _uiState.value.liveCaption.trim()
            if (caption.isNotBlank()) {
                val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                sessionTurnLines.add("[$ts] $activeName: $caption")
                appendLog(activeName.uppercase(), caption)
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
            audioEngine.releaseAll()
            persistCurrentTranscriptIfAny()
            CallServiceBridge.stopService(appContext)
            appendLog("ERROR", err)
            _uiState.update {
                it.copy(state = SuzyState.ERROR, statusText = "Error: $err", amplitude = 0f)
            }
        },
        onClosed = {
            audioEngine.releaseAll()
            persistCurrentTranscriptIfAny()
            CallServiceBridge.stopService(appContext)
            _uiState.update { cur ->
                if (cur.state != SuzyState.ERROR) {
                    cur.copy(
                        state = SuzyState.IDLE,
                        statusText = "Tap Start to talk",
                        amplitude = 0f
                    )
                } else cur
            }
        }
    )

    private fun liveClientSend(pcm: ByteArray) {
        liveClient.sendPcmAudio(pcm)
    }

    private fun liveClientSendToolResponse(callId: String, name: String, result: String) {
        liveClient.sendToolResponse(callId, name, result)
    }

    fun startSession() {
        val currentSettings = settings.value
        if (currentSettings.apiKey.isBlank()) {
            _uiState.update {
                it.copy(state = SuzyState.ERROR, statusText = "Pehle Settings mein Gemini API Key daalo")
            }
            return
        }
        val enrichedSettings = buildEnrichedSettings(currentSettings)
        _uiState.update {
            it.copy(
                state = SuzyState.CONNECTING,
                statusText = "${currentSettings.activePersonaName} se connect ho raha hai...",
                liveCaption = "",
                lastCaption = ""
            )
        }
        CallServiceBridge.startService(
            appContext,
            currentSettings.activePersonaName,
            "Connecting live voice...",
            _uiState.value.isMuted
        )
        liveClient.connect(enrichedSettings)
    }

    fun stopSession() {
        audioEngine.playUiChime(false)
        liveClient.disconnect()
        audioEngine.releaseAll()
        persistCurrentTranscriptIfAny()
        CallServiceBridge.stopService(appContext)
        appendLog("SYSTEM", "Session stopped")
        _uiState.update {
            it.copy(state = SuzyState.IDLE, statusText = "Tap Start to talk", amplitude = 0f)
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
            appendLog("PERSONA", "Switched to ${persona.name} (${persona.role} - ${persona.voiceName})")
            if (_uiState.value.state == SuzyState.LISTENING || _uiState.value.state == SuzyState.SPEAKING) {
                stopSession()
                _uiState.update { it.copy(statusText = "${persona.name} ready! Tap Start") }
            } else {
                _uiState.update { it.copy(statusText = "${persona.name} selected (${persona.voiceName})") }
            }
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
            repo.saveCustomPersona(name, role, voiceName, accentHex, prompt)
            appendLog("CUSTOM PERSONA", "Created persona '$name' ($voiceName)")
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
            appendLog("MOOD", "Live mood switched to ${mood.label}")
            val isActive = _uiState.value.state == SuzyState.LISTENING || _uiState.value.state == SuzyState.SPEAKING
            if (isActive) {
                liveClient.sendReactionPrompt("Apna mood turant '${mood.label}' par shift karo: ${mood.instruction} Aur isi mood mein 1 chhoti cute line bolo.")
            }
        }
    }

    fun selectVoice(voiceName: String) {
        val updated = settings.value.copy(voiceName = voiceName)
        viewModelScope.launch {
            repo.saveSettings(updated)
            appendLog("VOICE", "Voice changed to $voiceName")
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
            appendLog("MEMORY VAULT", "Saved ($category): $content")
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
        appendLog("MANUAL TOOL", res)
        _uiState.update { it.copy(isFlashlightOn = suzyTools.isTorchOn) }
    }

    fun openAppManual(appName: String) {
        val res = suzyTools.openApp(appName)
        appendLog("MANUAL TOOL", res)
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun sendQuickReaction(prompt: String) {
        audioEngine.flushPlayback()
        liveClient.sendReactionPrompt(prompt)
    }

    fun interruptSuzy() {
        audioEngine.flushPlayback()
        _uiState.update { it.copy(state = SuzyState.LISTENING, statusText = "${settings.value.activePersonaName} sun rahi hai...") }
    }

    fun updateSettings(updated: SuzySettings) {
        viewModelScope.launch {
            repo.saveSettings(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        CallServiceBridge.onToggleMuteRequested = null
        CallServiceBridge.onStopCallRequested = null
        stopSession()
    }
}
