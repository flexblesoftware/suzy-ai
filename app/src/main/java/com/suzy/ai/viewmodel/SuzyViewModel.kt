package com.suzy.ai.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.StatFs
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suzy.ai.audio.AudioBlockReason
import com.suzy.ai.audio.AudioEngine
import com.suzy.ai.audio.ExternalMicCoexistenceGuard
import com.suzy.ai.brain.OmniCommandRouter
import com.suzy.ai.data.AutomationRuleItem
import com.suzy.ai.data.ChatMessageItem
import com.suzy.ai.data.FinanceEntryItem
import com.suzy.ai.data.HabitEntryItem
import com.suzy.ai.data.MemoryItem
import com.suzy.ai.data.MoodOption
import com.suzy.ai.data.NoteVaultItem
import com.suzy.ai.data.OmniVaultRepository
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.PlannerTaskItem
import com.suzy.ai.data.SavedTranscript
import com.suzy.ai.data.SecurityAuditItem
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.network.GeminiChatClient
import com.suzy.ai.network.GeminiLiveClient
import com.suzy.ai.service.AnnouncedEventRecord
import com.suzy.ai.service.AnnouncerStateBridge
import com.suzy.ai.service.CallAnnouncerService
import com.suzy.ai.service.CallServiceBridge
import com.suzy.ai.tools.SuzyTools
import com.suzy.ai.util.AppPermissionsStatus
import com.suzy.ai.util.PermissionHelper
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
import kotlin.math.sqrt

enum class SuzyState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class SuzyUiState(
    val state: SuzyState = SuzyState.IDLE,
    val statusText: String = "Say \"Hey Suzy\" or Tap Start Call",
    val liveCaption: String = "",
    val lastCaption: String = "",
    val amplitude: Float = 0f,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val callDurationSec: Long = 0L,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val pingMs: Int? = null,
    val isPinging: Boolean = false,
    val timerRemainingSec: Long = 0L,
    val isTimerRunning: Boolean = false,
    val isGuardRunning: Boolean = false,
    val isWakeWordListening: Boolean = false,
    val audioBlockReason: AudioBlockReason = AudioBlockReason.NONE,
    val lastAnnouncedEvent: AnnouncedEventRecord? = null,
    val permissions: AppPermissionsStatus = AppPermissionsStatus(),
    val toolFeedback: String = "",
    val isChatSending: Boolean = false,
    val pendingNavigationRoute: String? = null,
    val isExternalMicYielded: Boolean = false,
    val externalMicReason: String = ""
)

class SuzyViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val appContext = application.applicationContext
    private val repo = SettingsRepository(appContext)
    private val omniRepo = OmniVaultRepository(appContext)
    private val audioEngine = AudioEngine(appContext, viewModelScope)
    @Volatile
    private var isRecordingVoiceprint: Boolean = false
    private val externalMicGuard = ExternalMicCoexistenceGuard(appContext, viewModelScope) { yielded, reason ->
        _uiState.update { it.copy(isExternalMicYielded = yielded, externalMicReason = reason) }
        if (yielded) {
            if (!isSessionActive() && !isRecordingVoiceprint) {
                CallAnnouncerService.stopGuard(appContext)
                _uiState.update { it.copy(isWakeWordListening = false, statusText = reason) }
            }
        } else {
            if (settings.value.backgroundGuardEnabled && !isRecordingVoiceprint) {
                CallAnnouncerService.startGuard(appContext)
                refreshGuardAndPermissions()
                _uiState.update {
                    if (!isSessionActive()) it.copy(statusText = "Say \"${settings.value.customWakePhrase}\" or Tap Start Call") else it
                }
            }
        }
    }
    private val suzyTools = SuzyTools(appContext)
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

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

    val chatMessages: StateFlow<List<ChatMessageItem>> = omniRepo.chatMessagesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<PlannerTaskItem>> = omniRepo.tasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val financeEntries: StateFlow<List<FinanceEntryItem>> = omniRepo.financeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitEntryItem>> = omniRepo.habitsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val notes: StateFlow<List<NoteVaultItem>> = omniRepo.notesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val rules: StateFlow<List<AutomationRuleItem>> = omniRepo.rulesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val securityLogs: StateFlow<List<SecurityAuditItem>> = omniRepo.securityAuditFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val parkingNote: StateFlow<String> = omniRepo.parkingNoteFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val medicalId: StateFlow<String> = omniRepo.medicalIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private val _uiState = MutableStateFlow(SuzyUiState())
    val uiState: StateFlow<SuzyUiState> = _uiState.asStateFlow()

    @Volatile
    private var sessionGeneration: Long = 0L

    @Volatile
    private var liveClient: GeminiLiveClient? = null

    @Volatile
    private var pendingWakeGreetingPrompt: String? = null

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private val sessionTurnLines = mutableListOf<String>()
    private var lastShakeTimestampMs: Long = 0L
    private var wasFaceDown: Boolean = false

    init {
        CallServiceBridge.onToggleMuteRequested = { toggleMute() }
        CallServiceBridge.onStopCallRequested = { stopSession() }

        AnnouncerStateBridge.onAudioShieldStateChanged = { reason ->
            _uiState.update {
                it.copy(
                    audioBlockReason = reason,
                    isWakeWordListening = AnnouncerStateBridge.isWakeWordListening
                )
            }
        }

        AnnouncerStateBridge.onPhoneCallIncomingDuringLiveSession = { caller ->
            if (isSessionActive() && !_uiState.value.isMuted) {
                toggleMute()
                _uiState.update { it.copy(statusText = "Incoming phone call: $caller (Mic paused)") }
            }
            refreshGuardAndPermissions()
        }

        AnnouncerStateBridge.onPhoneCallOffhookSuspendLiveSession = {
            if (isSessionActive()) {
                stopSessionInternal(statusOverride = "Paused during phone call (Privacy Shield)")
            }
            refreshGuardAndPermissions()
        }

        AnnouncerStateBridge.onInjectPromptIntoActiveLiveSession = { prompt ->
            val client = liveClient
            if (isSessionActive() && client != null && AnnouncerStateBridge.audioBlockReason != AudioBlockReason.PHONE_CALL_ACTIVE) {
                audioEngine.flushPlayback()
                client.sendReactionPrompt(prompt)
                refreshGuardAndPermissions()
                true
            } else {
                false
            }
        }

        AnnouncerStateBridge.onWakeWordDetected = { trailingQuery ->
            if (!isSessionActive() && AnnouncerStateBridge.audioBlockReason == AudioBlockReason.NONE) {
                val activeName = settings.value.activePersonaName
                if (trailingQuery.isNotBlank()) {
                    executeOmniCommand(trailingQuery, speakResponse = false)
                }
                val wakePrompt = if (trailingQuery.isNotBlank()) {
                    "User ne abhi wake word bol kar tumhe bulaya aur kaha: '$trailingQuery'. Apne natural Hinglish style mein turant jawab do!"
                } else {
                    "User ne abhi '${settings.value.customWakePhrase}' bol kar tumhe awaz di hai! Pyaar se turant 1 chhoti cute line mein bolo 'Haan bolo ${settings.value.ownerNickname}, main sun rahi hoon!'"
                }
                startSession(initialWakePrompt = wakePrompt)
            }
            refreshGuardAndPermissions()
        }

        externalMicGuard.enabled = settings.value.externalMicCoexistenceEnabled
        externalMicGuard.start()
        registerHardwareSensors()
        refreshDeviceTelemetry()
        refreshGuardAndPermissions()
    }

    private fun registerHardwareSensors() {
        try {
            sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { prox ->
                sensorManager.registerListener(this, prox, SensorManager.SENSOR_DELAY_NORMAL)
            }
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { acc ->
                sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val ev = event ?: return
        val s = settings.value

        if (ev.sensor.type == Sensor.TYPE_PROXIMITY && s.proximityEarRoutingEnabled && isSessionActive()) {
            val distance = ev.values[0]
            val nearEar = distance < ev.sensor.maximumRange && distance < 4f
            if (nearEar && _uiState.value.isSpeakerOn) {
                audioEngine.setSpeakerphone(false)
                _uiState.update { it.copy(isSpeakerOn = false) }
            } else if (!nearEar && !_uiState.value.isSpeakerOn) {
                audioEngine.setSpeakerphone(true)
                _uiState.update { it.copy(isSpeakerOn = true) }
            }
        } else if (ev.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = ev.values[0]
            val y = ev.values[1]
            val z = ev.values[2]

            // Flip face-down to mute during active call
            if (s.flipToMuteEnabled && isSessionActive()) {
                val isFaceDownNow = z < -8.2f
                if (isFaceDownNow && !wasFaceDown && !_uiState.value.isMuted) {
                    toggleMute()
                }
                wasFaceDown = isFaceDownNow
            }

            // Double-shake to toggle live call
            if (s.shakeToCallEnabled) {
                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (gForce > 2.65f && (now - lastShakeTimestampMs) > 2200L) {
                    lastShakeTimestampMs = now
                    if (isSessionActive()) stopSession() else startSession()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun consumeNavigationRoute() {
        _uiState.update { it.copy(pendingNavigationRoute = null) }
    }

    fun executeOmniCommand(rawCommand: String, speakResponse: Boolean = true): String {
        val outcome = OmniCommandRouter.processCommandChain(
            context = appContext,
            rawInput = rawCommand,
            currentSettings = settings.value,
            isOwnerVerified = !settings.value.ownerModeStrict || settings.value.ownerVoiceEnrolled
        )
        if (!outcome.handled) return ""

        viewModelScope.launch {
            outcome.securityBlockedReason?.let { reason ->
                omniRepo.logSecurityEvent(rawCommand, "BLOCKED", reason)
                if (speakResponse) {
                    speakTextInGeminiVoice("Sorry, yeh command Owner Security Lock se protected hai. Pehle Owner Passphrase ya PIN verify karo.")
                }
            }

            outcome.updatedSettings?.let { newSet ->
                repo.saveSettings(newSet)
                hotReloadIfActive(newSet)
            }

            outcome.addMemoryPair?.let { (cat, fact) ->
                repo.addMemory(cat, fact, pinned = true)
            }

            outcome.addTaskTitle?.let { title ->
                omniRepo.addTask(title)
            }

            outcome.addFinanceEntry?.let { (kind, title, amt) ->
                omniRepo.addFinanceEntry(kind, title, title, amt)
            }

            outcome.addShoppingItem?.let { item ->
                omniRepo.addNoteItem("SHOPPING", item, "Added by command")
            }

            outcome.saveParkingText?.let { spot ->
                omniRepo.saveParkingSpot(spot)
            }

            outcome.startTimerMinutes?.let { mins ->
                startCountdownTimer(mins)
            }

            outcome.toggleTorch?.let { on ->
                val res = suzyTools.toggleFlashlight(on)
                _uiState.update { it.copy(isFlashlightOn = suzyTools.isTorchOn, toolFeedback = res) }
            }

            if (outcome.triggerStealthLock && isSessionActive()) {
                stopSessionInternal("Stealth Panic Lock Active")
            }

            if (outcome.triggerBossBriefing) {
                val brief = buildLiveBossBriefingText()
                _uiState.update { it.copy(toolFeedback = brief) }
                if (speakResponse) speakTextInGeminiVoice(brief)
            } else if (speakResponse && outcome.securityBlockedReason == null && outcome.summary.isNotBlank()) {
                speakTextInGeminiVoice("Done ${settings.value.ownerNickname}! ${outcome.summary}")
            }

            _uiState.update {
                it.copy(
                    toolFeedback = outcome.summary,
                    pendingNavigationRoute = outcome.navigateToRoute ?: it.pendingNavigationRoute
                )
            }
        }
        return outcome.summary
    }

    private fun buildLiveBossBriefingText(): String {
        val s = settings.value
        val bat = suzyTools.getBatteryTelemetry()
        val pendingTasks = tasks.value.count { !it.completed }
        val totalExpense = financeEntries.value.filter { it.kind == "EXPENSE" }.sumOf { it.amountInr }.toInt()
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = String.format(Locale.US, "%.1f", stat.availableBytes / (1024.0 * 1024.0 * 1024.0))
        return "Good day ${s.ownerNickname}! Battery ${bat.percent}% hai, $pendingTasks tasks pending hain, aaj ka expense ₹$totalExpense hai, aur phone mein $freeGb GB storage khaali hai."
    }

    fun sendChatMessage(text: String, imageBase64: String? = null, attachmentLabel: String = "") {
        val cleanText = text.trim()
        if (cleanText.isBlank() && imageBase64 == null) return

        val currentSet = settings.value
        val executedSummary = executeOmniCommand(cleanText, speakResponse = false)

        viewModelScope.launch {
            omniRepo.appendChatMessage(
                role = "user",
                personaName = currentSet.ownerName,
                text = cleanText,
                attachmentLabel = attachmentLabel,
                commandExecuted = executedSummary
            )
            _uiState.update { it.copy(isChatSending = true) }

            val enriched = buildEnrichedSettings(currentSet)
            val result = GeminiChatClient.sendMessage(
                settings = currentSet,
                systemInstruction = enriched.systemPrompt,
                history = chatMessages.value,
                userMessage = if (executedSummary.isNotBlank()) {
                    "$cleanText\n[SYSTEM NOTE: App already executed: $executedSummary. Confirm naturally in your persona style.]"
                } else cleanText,
                imageBase64 = imageBase64
            )

            _uiState.update { it.copy(isChatSending = false) }
            result.fold(
                onSuccess = { reply ->
                    omniRepo.appendChatMessage(
                        role = "model",
                        personaName = currentSet.activePersonaName,
                        text = reply,
                        commandExecuted = executedSummary
                    )
                },
                onFailure = { err ->
                    val fallback = if (executedSummary.isNotBlank()) {
                        "Command execute ho gaya (${executedSummary}), lekin AI text server se connect nahi ho paya: ${err.message}"
                    } else {
                        "Connection issue: ${err.message}. Kripya Settings mein Gemini API Key check karein."
                    }
                    omniRepo.appendChatMessage(
                        role = "model",
                        personaName = currentSet.activePersonaName,
                        text = fallback
                    )
                }
            )
        }
    }

    fun speakTextInGeminiVoice(textToSpeak: String) {
        val personaName = settings.value.activePersonaName
        CallAnnouncerService.triggerGeminiVoiceAlert(
            context = appContext,
            category = "GEMINI VOICE",
            title = personaName,
            prompt = "Tum $personaName ho. Yeh message apne natural Hinglish persona andaaz mein padh kar sunao: \"$textToSpeak\""
        )
    }

    fun clearChatHistory() {
        viewModelScope.launch { omniRepo.clearChatHistory() }
    }

    fun addPlannerTask(title: String) {
        viewModelScope.launch { omniRepo.addTask(title) }
    }

    fun togglePlannerTask(id: String) {
        viewModelScope.launch { omniRepo.toggleTask(id) }
    }

    fun addFinanceEntry(kind: String, title: String, personOrCat: String, amount: Double) {
        viewModelScope.launch { omniRepo.addFinanceEntry(kind, title, personOrCat, amount) }
    }

    fun toggleFinanceSettled(id: String) {
        viewModelScope.launch { omniRepo.toggleFinanceSettled(id) }
    }

    fun toggleHabit(id: String) {
        viewModelScope.launch { omniRepo.toggleHabit(id) }
    }

    fun addHabit(name: String) {
        viewModelScope.launch { omniRepo.addHabit(name) }
    }

    fun addNoteItem(kind: String, title: String, body: String) {
        viewModelScope.launch { omniRepo.addNoteItem(kind, title, body) }
    }

    fun toggleNoteChecked(id: String) {
        viewModelScope.launch { omniRepo.toggleNoteChecked(id) }
    }

    fun addAutomationRule(kind: String, trigger: String, summary: String, target: String) {
        viewModelScope.launch { omniRepo.addAutomationRule(kind, trigger, summary, target) }
    }

    fun saveParkingSpot(spot: String) {
        viewModelScope.launch { omniRepo.saveParkingSpot(spot) }
    }

    fun saveMedicalId(info: String) {
        viewModelScope.launch { omniRepo.saveMedicalId(info) }
    }

    fun prepareMicForVoiceprintRecording(recording: Boolean) {
        isRecordingVoiceprint = recording
        if (recording) {
            CallAnnouncerService.stopGuard(appContext)
            _uiState.update { it.copy(isWakeWordListening = false) }
        } else if (settings.value.backgroundGuardEnabled && !externalMicGuard.isYieldedToExternalApp) {
            CallAnnouncerService.startGuard(appContext)
            refreshGuardAndPermissions()
        }
    }

    fun yieldMicManuallyForExternalAi(seconds: Int = 30) {
        externalMicGuard.triggerManualYield(seconds, "Mic Released for Gemini / Claude (${seconds}s)")
    }

    fun onAppMovedToBackground() {
        if (settings.value.externalMicCoexistenceEnabled && !isSessionActive() && !isRecordingVoiceprint) {
            externalMicGuard.triggerManualYield(6, "Background Mic Cooldown")
        }
    }

    fun logSecurityEventManual(command: String, status: String, reason: String) {
        viewModelScope.launch {
            omniRepo.logSecurityEvent(command, status, reason)
        }
    }

    fun refreshGuardAndPermissions() {
        val perms = PermissionHelper.checkAll(appContext)
        _uiState.update {
            it.copy(
                permissions = perms,
                isGuardRunning = AnnouncerStateBridge.isGuardRunning,
                isWakeWordListening = AnnouncerStateBridge.isWakeWordListening,
                audioBlockReason = AnnouncerStateBridge.audioBlockReason,
                lastAnnouncedEvent = AnnouncerStateBridge.lastAnnouncement
            )
        }
    }

    fun syncBackgroundGuard(enabled: Boolean) {
        externalMicGuard.enabled = settings.value.externalMicCoexistenceEnabled
        if (enabled && !externalMicGuard.isYieldedToExternalApp && !isRecordingVoiceprint) {
            CallAnnouncerService.startGuard(appContext)
        } else {
            CallAnnouncerService.stopGuard(appContext)
        }
        viewModelScope.launch {
            delay(300L)
            refreshGuardAndPermissions()
        }
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

    fun testServerPing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true) }
            val ms = suzyTools.measureGeminiPingMs()
            _uiState.update {
                it.copy(
                    isPinging = false,
                    pingMs = if (ms > 0) ms else null,
                    toolFeedback = if (ms > 0) "Gemini Live Server Ping: ${ms}ms (Ultra-Fast)" else "Ping timeout — internet check karein"
                )
            }
        }
    }

    fun startCountdownTimer(minutes: Int) {
        countdownJob?.cancel()
        val total = (minutes * 60L).coerceAtLeast(10L)
        _uiState.update { it.copy(timerRemainingSec = total, isTimerRunning = true) }
        countdownJob = viewModelScope.launch {
            var rem = total
            while (isActive && rem > 0L) {
                delay(1000L)
                rem--
                _uiState.update { it.copy(timerRemainingSec = rem) }
            }
            if (rem == 0L) {
                _uiState.update {
                    it.copy(
                        isTimerRunning = false,
                        toolFeedback = "$minutes min timer complete!"
                    )
                }
                audioEngine.playUiChime(true)
                val personaName = settings.value.activePersonaName
                CallAnnouncerService.triggerGeminiVoiceAlert(
                    context = appContext,
                    category = "TIMER ALERT",
                    title = "$minutes min Timer Complete",
                    prompt = "Tum $personaName ho. User ka $minutes minute ka timer abhi poora ho gaya hai! Apne natural Hinglish persona style mein pyar se batao ki timer khatam ho gaya hai (1 chhoti line mein)."
                )
            }
        }
    }

    fun cancelCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update { it.copy(timerRemainingSec = 0L, isTimerRunning = false) }
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
        val savedMemories = if (base.guestModeActive) emptyList() else memories.value

        val promptBuilder = StringBuilder(hardcodedBasePrompt.trim())
        promptBuilder.append("\n\n[OWNER & APP SELF-AWARENESS]:")
        promptBuilder.append("\n- Owner Name: ${base.ownerName} (Title: ${base.ownerNickname})")
        promptBuilder.append("\n- Current Persona: ${base.activePersonaName} | Voice: ${base.voiceName} | ORB Style: ${base.orbStyle} | Theme: ${base.uiThemeSkin}")
        promptBuilder.append("\n- Bond XP: ${base.bondXp} | Streak: ${base.streakDays} days | Trust Score: ${base.trustScore}/100 | Narazgi/Anger Level: ${base.angerLevel}%")
        if (base.angerLevel >= 40) {
            promptBuilder.append("\n- [ATTITUDE ALERT]: Tum abhi user se ${base.angerLevel}% naraz ho. Chhote, teekhe jawab do jab tak user sorry bol kar mana na le (kabhi gaali mat dena).")
        }
        if (base.customNote.isNotBlank()) {
            promptBuilder.append("\n- [OWNER PERSONAL INSTRUCTION]: ${base.customNote.trim()}")
        }
        if (mood != null) {
            promptBuilder.append("\n- [CURRENT LIVE MOOD - ${mood.label}]: ${mood.instruction}")
        }
        if (savedMemories.isNotEmpty()) {
            promptBuilder.append("\n\n[PERMANENT MEMORY VAULT - Hamesha yaad rakho aur naturally baat mein use karo]:")
            savedMemories.take(30).forEach { mem ->
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
            val xpGain = (elapsedSec / 10L).toInt().coerceIn(5, 80)
            repo.saveSettings(settings.value.copy(bondXp = settings.value.bondXp + xpGain))
        }
    }

    private fun createClientForGeneration(gen: Long, targetSettings: SuzySettings): GeminiLiveClient {
        return GeminiLiveClient(
            onConnected = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                val activeName = targetSettings.activePersonaName
                AnnouncerStateBridge.isLiveCallSessionActive = true
                CallAnnouncerService.syncWakeWordState(appContext)
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
                    it.copy(
                        state = SuzyState.LISTENING,
                        statusText = "$activeName sun rahi hai...",
                        isWakeWordListening = false
                    )
                }
                CallServiceBridge.updateNotification(
                    appContext,
                    activeName,
                    "$activeName sun rahi hai...",
                    _uiState.value.isMuted
                )

                val wakeGreeting = pendingWakeGreetingPrompt
                pendingWakeGreetingPrompt = null
                if (!wakeGreeting.isNullOrBlank()) {
                    viewModelScope.launch {
                        delay(220L)
                        if (gen == sessionGeneration) {
                            liveClient?.sendReactionPrompt(wakeGreeting)
                        }
                    }
                }
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
                val queryArg = args.optString("query").ifBlank { args.optString("command") }
                val omniResult = if (queryArg.isNotBlank()) executeOmniCommand(queryArg, speakResponse = false) else ""
                val result = omniResult.ifBlank { suzyTools.executeTool(name, args) }
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
                AnnouncerStateBridge.isLiveCallSessionActive = false
                audioEngine.releaseAll()
                persistCurrentTranscriptIfAny()
                stopCallTimer()
                CallServiceBridge.stopService(appContext)
                CallAnnouncerService.syncWakeWordState(appContext)
                _uiState.update {
                    it.copy(state = SuzyState.ERROR, statusText = "Error: $err", amplitude = 0f)
                }
            },
            onClosed = {
                if (gen != sessionGeneration) return@GeminiLiveClient
                AnnouncerStateBridge.isLiveCallSessionActive = false
                audioEngine.releaseAll()
                persistCurrentTranscriptIfAny()
                stopCallTimer()
                CallServiceBridge.stopService(appContext)
                CallAnnouncerService.syncWakeWordState(appContext)
                _uiState.update { cur ->
                    if (cur.state != SuzyState.ERROR) {
                        cur.copy(
                            state = SuzyState.IDLE,
                            statusText = "Say \"${settings.value.customWakePhrase}\" or Tap Start Call",
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

    fun startSession(customSettings: SuzySettings? = null, initialWakePrompt: String? = null) {
        if (AnnouncerStateBridge.audioBlockReason == AudioBlockReason.PHONE_CALL_ACTIVE) {
            _uiState.update {
                it.copy(statusText = "Phone call active — Call Privacy Shield ON")
            }
            return
        }

        val baseSettings = customSettings ?: settings.value
        val activeKey = baseSettings.apiKey.ifBlank { baseSettings.backupApiKey }
        if (activeKey.isBlank()) {
            _uiState.update {
                it.copy(state = SuzyState.ERROR, statusText = "Pehle Settings Studio mein Gemini API Key daalo")
            }
            return
        }
        val gen = ++sessionGeneration
        pendingWakeGreetingPrompt = initialWakePrompt
        AnnouncerStateBridge.isLiveCallSessionActive = true
        CallAnnouncerService.syncWakeWordState(appContext)

        liveClient?.disconnect()
        audioEngine.flushPlayback()

        val enrichedSettings = buildEnrichedSettings(baseSettings.copy(apiKey = activeKey))
        _uiState.update {
            it.copy(
                state = SuzyState.CONNECTING,
                statusText = "${baseSettings.activePersonaName} connect ho rahi hai...",
                liveCaption = "",
                lastCaption = "",
                isWakeWordListening = false
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
        stopSessionInternal(
            statusOverride = "Say \"${settings.value.customWakePhrase}\" or Tap Start Call"
        )
    }

    private fun stopSessionInternal(statusOverride: String) {
        ++sessionGeneration
        pendingWakeGreetingPrompt = null
        AnnouncerStateBridge.isLiveCallSessionActive = false
        audioEngine.playUiChime(false)
        liveClient?.disconnect()
        liveClient = null
        audioEngine.releaseAll()
        persistCurrentTranscriptIfAny()
        stopCallTimer()
        CallServiceBridge.stopService(appContext)
        CallAnnouncerService.syncWakeWordState(appContext)
        viewModelScope.launch {
            delay(350L)
            refreshGuardAndPermissions()
        }
        _uiState.update {
            it.copy(
                state = SuzyState.IDLE,
                statusText = statusOverride,
                amplitude = 0f
            )
        }
    }

    private fun hotReloadIfActive(updatedSettings: SuzySettings) {
        if (isSessionActive()) {
            startSession(updatedSettings)
        } else {
            CallAnnouncerService.syncWakeWordState(appContext)
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
        prompt: String,
        gender: String = "Female"
    ) {
        viewModelScope.launch {
            val created = repo.saveCustomPersona(name, role, voiceName, accentHex, prompt, gender)
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
        viewModelScope.launch { repo.deleteCustomPersona(id) }
    }

    fun selectMood(mood: MoodOption) {
        val updated = settings.value.copy(activeMoodId = mood.id)
        viewModelScope.launch {
            repo.saveSettings(updated)
            if (isSessionActive()) {
                liveClient?.sendReactionPrompt(
                    "Apna mood turant '${mood.label}' par shift karo: ${mood.instruction} Isi mood mein 1 chhoti line bolo."
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
        viewModelScope.launch { repo.saveSettings(updated) }
    }

    fun toggleWakeWord(enabled: Boolean) {
        val updated = settings.value.copy(wakeWordEnabled = enabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
            if (enabled) syncBackgroundGuard(true) else CallAnnouncerService.syncWakeWordState(appContext)
        }
    }

    fun toggleMediaShield(enabled: Boolean) {
        val updated = settings.value.copy(mediaShieldEnabled = enabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
            CallAnnouncerService.syncWakeWordState(appContext)
        }
    }

    fun toggleCallPrivacyShield(enabled: Boolean) {
        val updated = settings.value.copy(callPrivacyShieldEnabled = enabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
            CallAnnouncerService.syncWakeWordState(appContext)
        }
    }

    fun toggleCallAnnouncer(enabled: Boolean) {
        val updated = settings.value.copy(callAnnouncerEnabled = enabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
            if (enabled) syncBackgroundGuard(true)
        }
    }

    fun toggleNotificationReader(enabled: Boolean) {
        val updated = settings.value.copy(notificationReaderEnabled = enabled)
        viewModelScope.launch {
            repo.saveSettings(updated)
            if (enabled) syncBackgroundGuard(true)
        }
    }

    fun triggerTestCallAnnouncement() {
        CallAnnouncerService.triggerTestCallAnnouncement(appContext)
    }

    fun triggerTestNotificationAnnouncement() {
        CallAnnouncerService.triggerTestNotificationAnnouncement(appContext)
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
        viewModelScope.launch { repo.deleteMemory(id) }
    }

    fun togglePinMemory(id: String) {
        viewModelScope.launch { repo.togglePinMemory(id) }
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        audioEngine.isMuted = next
        val activeName = settings.value.activePersonaName
        val newStatus = if (next) "Mic muted hai" else "$activeName sun rahi hai..."
        _uiState.update { it.copy(isMuted = next, statusText = newStatus) }
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
            syncBackgroundGuard(resolved.backgroundGuardEnabled)
            hotReloadIfActive(resolved)
        }
    }

    override fun onCleared() {
        super.onCleared()
        externalMicGuard.stop()
        try { sensorManager?.unregisterListener(this) } catch (_: Exception) {}
        CallServiceBridge.onToggleMuteRequested = null
        CallServiceBridge.onStopCallRequested = null
        AnnouncerStateBridge.onPhoneCallIncomingDuringLiveSession = null
        AnnouncerStateBridge.onPhoneCallOffhookSuspendLiveSession = null
        AnnouncerStateBridge.onAudioShieldStateChanged = null
        AnnouncerStateBridge.onInjectPromptIntoActiveLiveSession = null
        AnnouncerStateBridge.onWakeWordDetected = null
        AnnouncerStateBridge.isLiveCallSessionActive = false
        stopSession()
    }
}
