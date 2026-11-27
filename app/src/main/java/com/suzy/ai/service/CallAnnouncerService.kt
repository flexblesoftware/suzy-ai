package com.suzy.ai.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.suzy.ai.MainActivity
import com.suzy.ai.audio.AudioBlockReason
import com.suzy.ai.audio.AudioEnvironmentGuard
import com.suzy.ai.audio.WakeWordEngine
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.network.GeminiLiveClient
import com.suzy.ai.receiver.IncomingCallReceiver
import com.suzy.ai.util.NotificationEvalResult
import com.suzy.ai.util.NotificationSecurityGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnnouncedEventRecord(
    val category: String,
    val title: String,
    val time: String,
    val summary: String
)

object AnnouncerStateBridge {
    @Volatile
    var isGuardRunning: Boolean = false

    @Volatile
    var isWakeWordListening: Boolean = false

    @Volatile
    var isLiveCallSessionActive: Boolean = false

    @Volatile
    var audioBlockReason: AudioBlockReason = AudioBlockReason.NONE

    @Volatile
    var lastAnnouncement: AnnouncedEventRecord? = null

    @Volatile
    var onPhoneCallIncomingDuringLiveSession: ((String) -> Unit)? = null

    @Volatile
    var onPhoneCallOffhookSuspendLiveSession: (() -> Unit)? = null

    @Volatile
    var onAudioShieldStateChanged: ((AudioBlockReason) -> Unit)? = null

    @Volatile
    var onInjectPromptIntoActiveLiveSession: ((String) -> Boolean)? = null

    @Volatile
    var onWakeWordDetected: ((String) -> Unit)? = null
}

class CallAnnouncerService : Service() {

    companion object {
        const val CHANNEL_ID = "suzy_call_guard_channel"
        const val NOTIFICATION_ID = 3802

        const val ACTION_START_GUARD = "com.suzy.ai.action.START_GUARD"
        const val ACTION_SYNC_WAKE_WORD = "com.suzy.ai.action.SYNC_WAKE_WORD"
        const val ACTION_RINGING = "com.suzy.ai.action.RINGING"
        const val ACTION_OFFHOOK = "com.suzy.ai.action.OFFHOOK"
        const val ACTION_CALL_IDLE = "com.suzy.ai.action.CALL_IDLE"
        const val ACTION_NOTIFICATION_EVENT = "com.suzy.ai.action.NOTIFICATION_EVENT"
        const val ACTION_TEST_CALL = "com.suzy.ai.action.TEST_CALL"
        const val ACTION_TEST_NOTIFICATION = "com.suzy.ai.action.TEST_NOTIFICATION"
        const val ACTION_TEST_WAKE_WORD = "com.suzy.ai.action.TEST_WAKE_WORD"
        const val ACTION_GEMINI_ALERT = "com.suzy.ai.action.GEMINI_ALERT"

        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_VISIBILITY = "extra_visibility"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_PROMPT = "extra_prompt"

        fun startGuard(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_START_GUARD
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun syncWakeWordState(context: Context) {
            if (!AnnouncerStateBridge.isGuardRunning) return
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_SYNC_WAKE_WORD
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun stopGuard(context: Context) {
            try {
                context.stopService(Intent(context, CallAnnouncerService::class.java))
            } catch (_: Exception) {
            }
        }

        fun onPhoneRinging(context: Context, number: String?) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_RINGING
                putExtra(EXTRA_NUMBER, number ?: "")
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun onPhoneAnsweredOffhook(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_OFFHOOK
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun onPhoneCallIdle(context: Context) {
            if (!AnnouncerStateBridge.isGuardRunning) return
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_CALL_IDLE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun onNotificationArrived(
            context: Context,
            packageName: String,
            appName: String,
            visibility: Int,
            senderTitle: String,
            messageBody: String
        ) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_NOTIFICATION_EVENT
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_VISIBILITY, visibility)
                putExtra(EXTRA_TITLE, senderTitle)
                putExtra(EXTRA_BODY, messageBody)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun triggerTestCallAnnouncement(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_TEST_CALL
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun triggerTestNotificationAnnouncement(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_TEST_NOTIFICATION
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun triggerTestWakeWord(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_TEST_WAKE_WORD
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun triggerGeminiVoiceAlert(context: Context, category: String, title: String, prompt: String) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_GEMINI_ALERT
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROMPT, prompt)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dynamicReceiver: IncomingCallReceiver? = null
    private var ringDebounceJob: Job? = null
    private var activeAnnouncementJob: Job? = null
    private var activeOneShotClient: GeminiLiveClient? = null
    private var activeOneShotTrack: AudioTrack? = null

    @Volatile
    private var activeRingingResolved: Boolean = false

    @Volatile
    private var isCellularCallActive: Boolean = false

    @Volatile
    private var mediaShieldFlag: Boolean = true

    @Volatile
    private var callShieldFlag: Boolean = true

    private var environmentGuard: AudioEnvironmentGuard? = null
    private var wakeWordEngine: WakeWordEngine? = null

    override fun onCreate() {
        super.onCreate()
        AnnouncerStateBridge.isGuardRunning = true
        createChannel()
        startForegroundCompat(buildNotification("Gemini AI Voice & Privacy Guard Active"))
        registerDynamicReceiver()

        environmentGuard = AudioEnvironmentGuard(
            context = applicationContext,
            isSuzyLiveSessionActive = { AnnouncerStateBridge.isLiveCallSessionActive },
            isMediaShieldEnabled = { mediaShieldFlag },
            isCallShieldEnabled = { callShieldFlag },
            onStateChanged = { reason ->
                AnnouncerStateBridge.audioBlockReason = reason
                AnnouncerStateBridge.onAudioShieldStateChanged?.invoke(reason)

                if (reason != AudioBlockReason.NONE) {
                    wakeWordEngine?.suspendTemporarily()
                    if (reason == AudioBlockReason.PHONE_CALL_ACTIVE && AnnouncerStateBridge.isLiveCallSessionActive) {
                        AnnouncerStateBridge.onPhoneCallOffhookSuspendLiveSession?.invoke()
                    }
                } else {
                    wakeWordEngine?.resumeIfPermitted()
                }
            }
        ).also { it.startMonitoring() }

        wakeWordEngine = WakeWordEngine(
            context = applicationContext,
            canListenNow = {
                val block = environmentGuard?.evaluateNow() ?: AudioBlockReason.NONE
                block == AudioBlockReason.NONE && !AnnouncerStateBridge.isLiveCallSessionActive && !isCellularCallActive
            },
            onWakeWordDetected = { trailingQuery ->
                handleWakeWordTriggered(trailingQuery)
            },
            onListeningStateChanged = { listening ->
                AnnouncerStateBridge.isWakeWordListening = listening
            }
        )
        refreshWakeWordEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(buildNotification("Gemini AI Voice & Privacy Guard Active"))

        when (intent?.action) {
            ACTION_START_GUARD, ACTION_SYNC_WAKE_WORD -> {
                refreshWakeWordEngine()
            }
            ACTION_RINGING -> {
                isCellularCallActive = true
                environmentGuard?.setTelephonyCallBusy(true)
                wakeWordEngine?.suspendTemporarily()
                val rawNum = intent.getStringExtra(EXTRA_NUMBER).orEmpty().trim()
                handleIncomingRinging(rawNum)
            }
            ACTION_OFFHOOK -> {
                isCellularCallActive = true
                activeRingingResolved = false
                ringDebounceJob?.cancel()
                stopOneShotPlayback()
                environmentGuard?.setTelephonyCallBusy(true)
                wakeWordEngine?.suspendTemporarily()
                AnnouncerStateBridge.onPhoneCallOffhookSuspendLiveSession?.invoke()
            }
            ACTION_CALL_IDLE -> {
                isCellularCallActive = false
                activeRingingResolved = false
                ringDebounceJob?.cancel()
                stopOneShotPlayback()
                serviceScope.launch {
                    delay(1800L)
                    if (!isCellularCallActive) {
                        environmentGuard?.setTelephonyCallBusy(false)
                        refreshWakeWordEngine()
                    }
                }
            }
            ACTION_NOTIFICATION_EVENT -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
                val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty().ifBlank { "App" }
                val visibility = intent.getIntExtra(EXTRA_VISIBILITY, Notification.VISIBILITY_PUBLIC)
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                handleIncomingNotification(pkg, appName, visibility, title, body)
            }
            ACTION_TEST_CALL -> {
                serviceScope.launch {
                    val repo = SettingsRepository(applicationContext)
                    val s = repo.settingsFlow.first()
                    val prompt = buildCallAnnouncementPrompt(
                        personaName = s.activePersonaName,
                        callerLabel = "Best Friend",
                        style = s.announcerStyle,
                        includeName = true,
                        repeatCount = 1
                    )
                    dispatchGeminiVoiceAnnouncement(
                        category = "CALL TEST",
                        displayTitle = "Best Friend",
                        displaySummary = "Gemini AI Voice Call Alert (${s.activePersonaName} • ${s.voiceName})",
                        prompt = prompt,
                        bypassCallCheck = true
                    )
                }
            }
            ACTION_TEST_NOTIFICATION -> {
                serviceScope.launch {
                    val repo = SettingsRepository(applicationContext)
                    val s = repo.settingsFlow.first()
                    val eval = NotificationSecurityGuard.evaluate(
                        context = applicationContext,
                        personaName = s.activePersonaName,
                        packageName = "com.whatsapp",
                        appName = "WhatsApp",
                        visibility = Notification.VISIBILITY_PUBLIC,
                        senderTitle = "Aarav",
                        messageBody = "Bhai aaj shaam ko milte hain, free ho kya?",
                        readContentEnabled = s.readNotificationContent,
                        blockSensitive = s.blockSensitiveNotifications,
                        hideWhenLocked = s.hideNotificationOnLockScreen,
                        trustedAppsOnly = false
                    )
                    if (eval is NotificationEvalResult.SafeToAnnounce) {
                        dispatchGeminiVoiceAnnouncement(
                            category = "SAFE NOTIF TEST",
                            displayTitle = eval.displayTitle,
                            displaySummary = eval.displaySummary,
                            prompt = eval.geminiPrompt,
                            bypassCallCheck = true
                        )
                    }
                }
            }
            ACTION_TEST_WAKE_WORD -> {
                handleWakeWordTriggered("")
            }
            ACTION_GEMINI_ALERT -> {
                val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty().ifBlank { "ALERT" }
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Suzy Alert" }
                val prompt = intent.getStringExtra(EXTRA_PROMPT).orEmpty()
                if (prompt.isNotBlank()) {
                    dispatchGeminiVoiceAnnouncement(
                        category = category,
                        displayTitle = title,
                        displaySummary = title,
                        prompt = prompt,
                        bypassCallCheck = false
                    )
                }
            }
        }
        return START_STICKY
    }

    private fun refreshWakeWordEngine() {
        serviceScope.launch {
            val repo = SettingsRepository(applicationContext)
            val s = repo.settingsFlow.first()
            mediaShieldFlag = s.mediaShieldEnabled
            callShieldFlag = s.callPrivacyShieldEnabled

            val block = environmentGuard?.evaluateNow() ?: AudioBlockReason.NONE
            val shouldRun = s.backgroundGuardEnabled &&
                s.wakeWordEnabled &&
                !AnnouncerStateBridge.isLiveCallSessionActive &&
                !isCellularCallActive &&
                block == AudioBlockReason.NONE

            if (shouldRun) {
                wakeWordEngine?.start(s.activePersonaName)
            } else if (s.backgroundGuardEnabled && s.wakeWordEnabled) {
                wakeWordEngine?.suspendTemporarily()
            } else {
                wakeWordEngine?.stop()
            }
        }
    }

    private fun handleWakeWordTriggered(trailingQuery: String) {
        // Double-check media/call privacy before starting
        val block = environmentGuard?.evaluateNow() ?: AudioBlockReason.NONE
        if (block != AudioBlockReason.NONE || isCellularCallActive) {
            wakeWordEngine?.suspendTemporarily()
            return
        }

        wakeWordEngine?.stop()
        playWakeWordChime()

        val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        AnnouncerStateBridge.lastAnnouncement = AnnouncedEventRecord(
            category = "WAKE WORD",
            title = "Hey Suzy Activated",
            time = ts,
            summary = if (trailingQuery.isNotBlank()) "Query: \"$trailingQuery\"" else "Hands-free live Gemini voice started"
        )
        startForegroundCompat(buildNotification("Wake Word Triggered ($ts)"))

        val liveHandler = AnnouncerStateBridge.onWakeWordDetected
        if (liveHandler != null) {
            liveHandler.invoke(trailingQuery)
        } else {
            serviceScope.launch {
                val repo = SettingsRepository(applicationContext)
                val s = repo.settingsFlow.first()
                val prompt = if (trailingQuery.isNotBlank()) {
                    "Tum ${s.activePersonaName} ho. User ne abhi wake word bol kar kaha: '$trailingQuery'. Apne natural Hinglish style mein turant jawab do."
                } else {
                    "Tum ${s.activePersonaName} ho. User ne abhi 'Hey ${s.activePersonaName}' bol kar tumhe awaz di hai! Pyaar se 1 chhoti cute line mein bolo 'Haan bolo jaan, main sun rahi hoon!'"
                }
                dispatchGeminiVoiceAnnouncement(
                    category = "WAKE VOICE",
                    displayTitle = "Hey ${s.activePersonaName}",
                    displaySummary = "Real Gemini Voice Response",
                    prompt = prompt,
                    bypassCallCheck = false
                )
            }
        }
    }

    private fun playWakeWordChime() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            serviceScope.launch {
                delay(240L)
                try { tg.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        }
    }

    private fun handleIncomingRinging(rawNumber: String) {
        if (rawNumber.isBlank() && !activeRingingResolved) {
            ringDebounceJob?.cancel()
            ringDebounceJob = serviceScope.launch {
                delay(420L)
                if (!activeRingingResolved && isCellularCallActive) {
                    activeRingingResolved = true
                    executeIncomingCallAnnouncement("")
                }
            }
            return
        }

        if (rawNumber.isNotBlank() && !activeRingingResolved && isCellularCallActive) {
            ringDebounceJob?.cancel()
            activeRingingResolved = true
            executeIncomingCallAnnouncement(rawNumber)
        }
    }

    private fun executeIncomingCallAnnouncement(number: String) {
        serviceScope.launch {
            val repo = SettingsRepository(applicationContext)
            val s = repo.settingsFlow.first()
            if (!s.callAnnouncerEnabled) return@launch

            val callerName = if (s.announceCallerName && number.isNotBlank()) {
                lookupContactName(number) ?: formatSpokenNumber(number)
            } else {
                ""
            }

            val label = callerName.ifBlank { "Unknown Caller" }
            AnnouncerStateBridge.onPhoneCallIncomingDuringLiveSession?.invoke(label)

            val prompt = buildCallAnnouncementPrompt(
                personaName = s.activePersonaName,
                callerLabel = label,
                style = s.announcerStyle,
                includeName = s.announceCallerName && callerName.isNotBlank(),
                repeatCount = s.announcerRepeatCount.coerceIn(1, 3)
            )
            dispatchGeminiVoiceAnnouncement(
                category = "INCOMING CALL",
                displayTitle = label,
                displaySummary = "Incoming call announced via Gemini ${s.voiceName}",
                prompt = prompt,
                bypassCallCheck = true
            )
        }
    }

    private fun handleIncomingNotification(
        packageName: String,
        appName: String,
        visibility: Int,
        senderTitle: String,
        messageBody: String
    ) {
        serviceScope.launch {
            // Do not read notifications aloud while user is talking on a phone/VoIP call
            if (isCellularCallActive || environmentGuard?.evaluateNow() == AudioBlockReason.PHONE_CALL_ACTIVE) {
                return@launch
            }

            val repo = SettingsRepository(applicationContext)
            val s = repo.settingsFlow.first()
            if (!s.notificationReaderEnabled) return@launch

            when (
                val eval = NotificationSecurityGuard.evaluate(
                    context = applicationContext,
                    personaName = s.activePersonaName,
                    packageName = packageName,
                    appName = appName,
                    visibility = visibility,
                    senderTitle = senderTitle,
                    messageBody = messageBody,
                    readContentEnabled = s.readNotificationContent,
                    blockSensitive = s.blockSensitiveNotifications,
                    hideWhenLocked = s.hideNotificationOnLockScreen,
                    trustedAppsOnly = s.trustedAppsOnly
                )
            ) {
                is NotificationEvalResult.Ignored -> return@launch
                is NotificationEvalResult.BlockedSensitive -> {
                    val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    AnnouncerStateBridge.lastAnnouncement = AnnouncedEventRecord(
                        category = "SECURITY SHIELD",
                        title = "$appName (Protected)",
                        time = ts,
                        summary = eval.reason
                    )
                    return@launch
                }
                is NotificationEvalResult.SafeToAnnounce -> {
                    dispatchGeminiVoiceAnnouncement(
                        category = appName.uppercase(),
                        displayTitle = eval.displayTitle,
                        displaySummary = eval.displaySummary,
                        prompt = eval.geminiPrompt,
                        bypassCallCheck = false
                    )
                }
            }
        }
    }

    private fun buildCallAnnouncementPrompt(
        personaName: String,
        callerLabel: String,
        style: String,
        includeName: Boolean,
        repeatCount: Int
    ): String {
        val repeatNote = if (repeatCount > 1) {
            "Is baat ko pyaar se $repeatCount baar repeat karke bolo taaki user turant sun le."
        } else {
            "Sirf 1-2 chhoti, natural lines mein bolo."
        }

        return when (style) {
            "Jarvis" -> {
                if (includeName) {
                    "Tum $personaName ho. Smart, warm aur crisp Hinglish-English mein user ko alert do ki '$callerLabel' ka incoming phone call aa raha hai. $repeatNote"
                } else {
                    "Tum $personaName ho. Smart, warm aur crisp Hinglish-English mein alert do ki phone par incoming call aa raha hai. $repeatNote"
                }
            }
            "Bestie" -> {
                if (includeName) {
                    "Tum $personaName ho. Full energetic aur bindass Indian bestie style mein Hinglish mein bolo: 'Oye sun! $callerLabel ka call aa raha hai, jaldi phone uthao!' $repeatNote"
                } else {
                    "Tum $personaName ho. Full energetic Indian bestie style mein Hinglish mein bolo ki phone baj raha hai, kisi ka call aa raha hai, jaldi uthao! $repeatNote"
                }
            }
            else -> {
                if (includeName) {
                    "Tum $personaName ho. Apne pyaare, soft aur affectionate Indian girlfriend andaaz mein Hinglish mein bolo: 'Suno jaan, $callerLabel ka call aa raha hai, phone uthao na!' $repeatNote"
                } else {
                    "Tum $personaName ho. Apne pyaare, soft aur affectionate Indian girlfriend andaaz mein Hinglish mein bolo: 'Suno jaan, tumhara phone baj raha hai, call aa rahi hai!' $repeatNote"
                }
            }
        }
    }

    private fun dispatchGeminiVoiceAnnouncement(
        category: String,
        displayTitle: String,
        displaySummary: String,
        prompt: String,
        bypassCallCheck: Boolean
    ) {
        if (!bypassCallCheck && environmentGuard?.evaluateNow() == AudioBlockReason.PHONE_CALL_ACTIVE) {
            return
        }

        val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        AnnouncerStateBridge.lastAnnouncement = AnnouncedEventRecord(
            category = category,
            title = displayTitle,
            time = ts,
            summary = displaySummary
        )
        startForegroundCompat(buildNotification("$category: $displayTitle ($ts)"))

        if (AnnouncerStateBridge.isLiveCallSessionActive) {
            val injected = AnnouncerStateBridge.onInjectPromptIntoActiveLiveSession?.invoke(prompt) ?: false
            if (injected) return
        }

        wakeWordEngine?.suspendTemporarily()
        environmentGuard?.setAnnouncing(true)
        activeAnnouncementJob?.cancel()
        stopOneShotPlayback()

        activeAnnouncementJob = serviceScope.launch {
            val repo = SettingsRepository(applicationContext)
            val s = repo.settingsFlow.first()
            if (s.apiKey.isBlank()) {
                environmentGuard?.setAnnouncing(false)
                startForegroundCompat(buildNotification("Gemini API Key missing in AI Config"))
                refreshWakeWordEngine()
                return@launch
            }

            val wakeLock = acquireShortWakeLock()
            try {
                runOneShotGeminiVoice(s, prompt)
            } finally {
                stopOneShotPlayback()
                environmentGuard?.setAnnouncing(false)
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (_: Exception) {
                }
                refreshWakeWordEngine()
            }
        }
    }

    private suspend fun runOneShotGeminiVoice(
        baseSettings: com.suzy.ai.data.SuzySettings,
        prompt: String
    ) {
        val hardcodedPersona = SuzyCatalog.resolveHardcodedPrompt(
            baseSettings.activePersonaName,
            baseSettings.systemPrompt
        )
        val announcementSettings = baseSettings.copy(
            systemPrompt = "$hardcodedPersona\n\n[ANNOUNCEMENT MODE]: Tum abhi user ke phone par aaye live alert ko apni asli aawaz mein announce kar rahi ho. Seedha aur natural Hinglish mein bolo."
        )

        val sampleRate = 24000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2400)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeOneShotTrack = track
        track.play()

        val pcmChannel = Channel<ByteArray>(Channel.UNLIMITED)
        val doneSignal = Channel<Unit>(1)

        var firstChunkTimeMs = 0L
        var totalBytesQueued = 0L

        val writerJob = serviceScope.launch {
            for (chunk in pcmChannel) {
                if (!isActive) break
                try {
                    track.write(chunk, 0, chunk.size)
                } catch (_: Exception) {
                }
            }
        }

        val client = GeminiLiveClient(
            onConnected = {
                activeOneShotClient?.sendReactionPrompt(prompt)
            },
            onAudioOutput = { pcm ->
                if (firstChunkTimeMs == 0L) {
                    firstChunkTimeMs = System.currentTimeMillis()
                }
                totalBytesQueued += pcm.size.toLong()
                pcmChannel.trySend(pcm)
            },
            onTextOutput = { },
            onToolCall = { _, _, _ -> },
            onInterrupted = { },
            onTurnComplete = {
                doneSignal.trySend(Unit)
            },
            onError = {
                doneSignal.trySend(Unit)
            },
            onClosed = {
                doneSignal.trySend(Unit)
            }
        )

        activeOneShotClient = client
        client.connect(announcementSettings)

        withTimeoutOrNull(14000L) {
            doneSignal.receive()
        }

        if (firstChunkTimeMs > 0L && totalBytesQueued > 0L) {
            val expectedDurationMs = (totalBytesQueued / 48L) + 450L
            val elapsedMs = System.currentTimeMillis() - firstChunkTimeMs
            val remainingMs = (expectedDurationMs - elapsedMs).coerceIn(0L, 10000L)
            if (remainingMs > 0L) {
                delay(remainingMs)
            }
        }

        pcmChannel.close()
        writerJob.cancel()
        client.disconnect()
    }

    private fun stopOneShotPlayback() {
        try {
            activeOneShotClient?.disconnect()
        } catch (_: Exception) {
        }
        activeOneShotClient = null

        try {
            activeOneShotTrack?.pause()
            activeOneShotTrack?.flush()
            activeOneShotTrack?.stop()
            activeOneShotTrack?.release()
        } catch (_: Exception) {
        }
        activeOneShotTrack = null
    }

    private fun acquireShortWakeLock(): PowerManager.WakeLock? {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SuzyAI::GeminiAnnounceLock").apply {
                setReferenceCounted(false)
                acquire(25_000L)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun lookupContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() }
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatSpokenNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 10) {
            digits.takeLast(10).toCharArray().joinToString(" ")
        } else {
            "unknown number"
        }
    }

    private fun registerDynamicReceiver() {
        try {
            val receiver = IncomingCallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            dynamicReceiver = receiver
        } catch (_: Exception) {
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val types = if (hasMic) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(NOTIFICATION_ID, notification, types)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(subtitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Suzy Gemini Privacy & Voice Guard")
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Suzy Gemini Privacy & Voice Guard",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Smart Media-Aware Wake Word, Call Privacy Lock, and Safe Notification Reader"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        AnnouncerStateBridge.isGuardRunning = false
        AnnouncerStateBridge.isWakeWordListening = false
        environmentGuard?.stopMonitoring()
        environmentGuard = null
        wakeWordEngine?.destroy()
        wakeWordEngine = null
        ringDebounceJob?.cancel()
        activeAnnouncementJob?.cancel()
        stopOneShotPlayback()
        try {
            dynamicReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
