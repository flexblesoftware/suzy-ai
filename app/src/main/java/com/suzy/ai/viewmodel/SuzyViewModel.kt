package com.suzy.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suzy.ai.audio.AudioEngine
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.network.GeminiLiveClient
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
    val amplitude: Float = 0f,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val logs: List<LogEntry> = emptyList()
)

class SuzyViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application.applicationContext)
    private val audioEngine = AudioEngine(application.applicationContext, viewModelScope)
    private val suzyTools = SuzyTools(application.applicationContext)

    val settings: StateFlow<SuzySettings> = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SuzySettings()
    )

    private val _uiState = MutableStateFlow(SuzyUiState())
    val uiState: StateFlow<SuzyUiState> = _uiState.asStateFlow()

    private fun appendLog(source: String, msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _uiState.update { current ->
            current.copy(logs = (current.logs + LogEntry(ts, source, msg)).takeLast(60))
        }
    }

    private val liveClient = GeminiLiveClient(
        onConnected = {
            val activeName = settings.value.activePersonaName
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
            appendLog("SYSTEM", "$activeName (${settings.value.voiceName}) connected")
            _uiState.update {
                it.copy(state = SuzyState.LISTENING, statusText = "$activeName sun rahi hai...")
            }
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
                cur.copy(liveCaption = (cur.liveCaption + text).takeLast(220))
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
                appendLog(activeName.uppercase(), caption)
            }
            _uiState.update {
                it.copy(
                    state = SuzyState.LISTENING,
                    statusText = "$activeName sun rahi hai...",
                    liveCaption = "",
                    amplitude = 0.05f
                )
            }
        },
        onError = { err ->
            audioEngine.releaseAll()
            appendLog("ERROR", err)
            _uiState.update {
                it.copy(state = SuzyState.ERROR, statusText = "Error: $err", amplitude = 0f)
            }
        },
        onClosed = {
            audioEngine.releaseAll()
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
        _uiState.update {
            it.copy(
                state = SuzyState.CONNECTING,
                statusText = "${currentSettings.activePersonaName} se connect ho raha hai...",
                liveCaption = ""
            )
        }
        liveClient.connect(currentSettings)
    }

    fun stopSession() {
        audioEngine.playUiChime(false)
        liveClient.disconnect()
        audioEngine.releaseAll()
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

    fun selectVoice(voiceName: String) {
        val updated = settings.value.copy(voiceName = voiceName)
        viewModelScope.launch {
            repo.saveSettings(updated)
            appendLog("VOICE", "Voice changed to $voiceName")
        }
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        audioEngine.isMuted = next
        _uiState.update {
            it.copy(isMuted = next, statusText = if (next) "Mic muted hai" else "${settings.value.activePersonaName} sun rahi hai...")
        }
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
        stopSession()
    }
}
