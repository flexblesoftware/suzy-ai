package com.suzy.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suzy.ai.audio.AudioEngine
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.network.GeminiLiveClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SuzyState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class SuzyUiState(
    val state: SuzyState = SuzyState.IDLE,
    val statusText: String = "Tap Start to talk with Suzy",
    val liveCaption: String = "",
    val amplitude: Float = 0f,
    val isMuted: Boolean = false
)

class SuzyViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application.applicationContext)
    private val audioEngine = AudioEngine(viewModelScope)

    val settings: StateFlow<SuzySettings> = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SuzySettings()
    )

    private val _uiState = MutableStateFlow(SuzyUiState())
    val uiState: StateFlow<SuzyUiState> = _uiState.asStateFlow()

    private val liveClient = GeminiLiveClient(
        onConnected = {
            audioEngine.startPlayback()
            audioEngine.startRecording(
                onAudioChunk = { pcm -> liveClientSend(pcm) },
                onAmplitude = { amp ->
                    if (_uiState.value.state == SuzyState.LISTENING) {
                        _uiState.value = _uiState.value.copy(amplitude = amp)
                    }
                }
            )
            _uiState.value = _uiState.value.copy(
                state = SuzyState.LISTENING,
                statusText = "Suzy sun rahi hai..."
            )
        },
        onAudioOutput = { pcm ->
            _uiState.value = _uiState.value.copy(
                state = SuzyState.SPEAKING,
                statusText = "Suzy bol rahi hai..."
            )
            audioEngine.playPcmChunk(pcm) { amp ->
                _uiState.value = _uiState.value.copy(amplitude = amp)
            }
        },
        onTextOutput = { text ->
            _uiState.value = _uiState.value.copy(
                liveCaption = (_uiState.value.liveCaption + text).takeLast(220)
            )
        },
        onInterrupted = {
            if (settings.value.bargeInEnabled) {
                audioEngine.flushPlayback()
            }
            _uiState.value = _uiState.value.copy(
                state = SuzyState.LISTENING,
                statusText = "Suzy sun rahi hai...",
                amplitude = 0.1f
            )
        },
        onTurnComplete = {
            _uiState.value = _uiState.value.copy(
                state = SuzyState.LISTENING,
                statusText = "Suzy sun rahi hai...",
                amplitude = 0.05f
            )
        },
        onError = { err ->
            audioEngine.releaseAll()
            _uiState.value = _uiState.value.copy(
                state = SuzyState.ERROR,
                statusText = "Error: $err",
                amplitude = 0f
            )
        },
        onClosed = {
            audioEngine.releaseAll()
            if (_uiState.value.state != SuzyState.ERROR) {
                _uiState.value = SuzyUiState()
            }
        }
    )

    private fun liveClientSend(pcm: ByteArray) {
        liveClient.sendPcmAudio(pcm)
    }

    fun startSession() {
        val currentSettings = settings.value
        if (currentSettings.apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                state = SuzyState.ERROR,
                statusText = "Pehle Settings mein Gemini API Key daalo"
            )
            return
        }
        _uiState.value = SuzyUiState(
            state = SuzyState.CONNECTING,
            statusText = "Suzy se connect ho raha hai..."
        )
        liveClient.connect(currentSettings)
    }

    fun stopSession() {
        liveClient.disconnect()
        audioEngine.releaseAll()
        _uiState.value = SuzyUiState()
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        audioEngine.isMuted = next
        _uiState.value = _uiState.value.copy(
            isMuted = next,
            statusText = if (next) "Mic muted hai" else "Suzy sun rahi hai..."
        )
    }

    fun interruptSuzy() {
        audioEngine.flushPlayback()
        _uiState.value = _uiState.value.copy(
            state = SuzyState.LISTENING,
            statusText = "Suzy sun rahi hai..."
        )
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
