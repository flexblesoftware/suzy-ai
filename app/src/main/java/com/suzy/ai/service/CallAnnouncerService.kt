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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.suzy.ai.MainActivity
import com.suzy.ai.data.SettingsRepository
import com.suzy.ai.receiver.IncomingCallReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnnouncedCallRecord(
    val callerName: String,
    val time: String,
    val spokenText: String
)

object AnnouncerStateBridge {
    @Volatile
    var isGuardRunning: Boolean = false

    @Volatile
    var lastAnnouncement: AnnouncedCallRecord? = null

    @Volatile
    var onPhoneCallIncomingDuringLiveSession: ((String) -> Unit)? = null
}

class CallAnnouncerService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "suzy_call_guard_channel"
        const val NOTIFICATION_ID = 3802
        const val ACTION_START_GUARD = "com.suzy.ai.action.START_GUARD"
        const val ACTION_RINGING = "com.suzy.ai.action.RINGING"
        const val ACTION_STOP_SPEAKING = "com.suzy.ai.action.STOP_SPEAKING"
        const val ACTION_TEST_ANNOUNCE = "com.suzy.ai.action.TEST_ANNOUNCE"
        const val ACTION_SPEAK_CUSTOM = "com.suzy.ai.action.SPEAK_CUSTOM"
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_TEXT = "extra_text"

        fun startGuard(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_START_GUARD
            }
            try {
                ContextCompat.startForegroundService(context, intent)
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

        fun onPhoneEndedOrAnswered(context: Context) {
            if (!AnnouncerStateBridge.isGuardRunning) return
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_STOP_SPEAKING
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun triggerTestAnnouncement(context: Context) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_TEST_ANNOUNCE
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun speakCustomMessage(context: Context, message: String) {
            val intent = Intent(context, CallAnnouncerService::class.java).apply {
                action = ACTION_SPEAK_CUSTOM
                putExtra(EXTRA_TEXT, message)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var pendingSpeech: String? = null
    private var dynamicReceiver: IncomingCallReceiver? = null
    private var ringDebounceJob: Job? = null
    private var activeRingingResolved: Boolean = false

    override fun onCreate() {
        super.onCreate()
        AnnouncerStateBridge.isGuardRunning = true
        createChannel()
        startForegroundCompat(buildNotification("Call Announcer & Screen-Off Guard Active"))
        tts = TextToSpeech(applicationContext, this)
        registerDynamicReceiver()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hiLocale = Locale("hi", "IN")
            val enInLocale = Locale("en", "IN")
            val res = tts?.setLanguage(hiLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(enInLocale)
            }
            tts?.setPitch(1.12f)
            tts?.setSpeechRate(1.02f)
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            ttsReady = true
            pendingSpeech?.let {
                pendingSpeech = null
                speakNow(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(buildNotification("Call Announcer & Screen-Off Guard Active"))

        when (intent?.action) {
            ACTION_RINGING -> {
                val rawNum = intent.getStringExtra(EXTRA_NUMBER).orEmpty().trim()
                handleIncomingRinging(rawNum)
            }
            ACTION_STOP_SPEAKING -> {
                ringDebounceJob?.cancel()
                activeRingingResolved = false
                try {
                    tts?.stop()
                } catch (_: Exception) {
                }
            }
            ACTION_TEST_ANNOUNCE -> {
                serviceScope.launch {
                    val repo = SettingsRepository(applicationContext)
                    val s = repo.settingsFlow.first()
                    val msg = buildAnnouncementLine(
                        callerLabel = "Best Friend",
                        style = s.announcerStyle,
                        includeName = true
                    )
                    recordAndSpeak(callerLabel = "Best Friend (Test)", spokenText = msg, repeatCount = 1)
                }
            }
            ACTION_SPEAK_CUSTOM -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                if (text.isNotBlank()) {
                    speakNow(text)
                }
            }
        }
        return START_STICKY
    }

    private fun handleIncomingRinging(rawNumber: String) {
        if (rawNumber.isBlank() && !activeRingingResolved) {
            ringDebounceJob?.cancel()
            ringDebounceJob = serviceScope.launch {
                delay(420L)
                if (!activeRingingResolved) {
                    activeRingingResolved = true
                    executeIncomingAnnouncement("")
                }
            }
            return
        }

        if (rawNumber.isNotBlank() && !activeRingingResolved) {
            ringDebounceJob?.cancel()
            activeRingingResolved = true
            executeIncomingAnnouncement(rawNumber)
        }
    }

    private fun executeIncomingAnnouncement(number: String) {
        serviceScope.launch {
            val repo = SettingsRepository(applicationContext)
            val s = repo.settingsFlow.first()
            if (!s.callAnnouncerEnabled) return@launch

            val callerName = if (s.announceCallerName && number.isNotBlank()) {
                lookupContactName(number) ?: formatSpokenNumber(number)
            } else {
                ""
            }

            AnnouncerStateBridge.onPhoneCallIncomingDuringLiveSession?.invoke(
                callerName.ifBlank { "Incoming Call" }
            )

            val spoken = buildAnnouncementLine(
                callerLabel = callerName,
                style = s.announcerStyle,
                includeName = s.announceCallerName && callerName.isNotBlank()
            )
            recordAndSpeak(
                callerLabel = callerName.ifBlank { "Unknown Caller" },
                spokenText = spoken,
                repeatCount = s.announcerRepeatCount.coerceIn(1, 3)
            )
        }
    }

    private fun buildAnnouncementLine(
        callerLabel: String,
        style: String,
        includeName: Boolean
    ): String {
        return when (style) {
            "Jarvis" -> {
                if (includeName) "Attention. Incoming call from $callerLabel."
                else "Attention. You have an incoming phone call."
            }
            "Bestie" -> {
                if (includeName) "Oye sun! $callerLabel ka call aa raha hai, jaldi phone uthao!"
                else "Oye sun! Phone baj raha hai, kisi ka call aa raha hai!"
            }
            else -> {
                if (includeName) "Suno jaan, $callerLabel ka call aa raha hai. Phone uthao na!"
                else "Suno jaan, tumhara phone baj raha hai, call aa rahi hai!"
            }
        }
    }

    private fun recordAndSpeak(callerLabel: String, spokenText: String, repeatCount: Int) {
        val ts = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        AnnouncerStateBridge.lastAnnouncement = AnnouncedCallRecord(
            callerName = callerLabel,
            time = ts,
            spokenText = spokenText
        )
        startForegroundCompat(buildNotification("Last call: $callerLabel ($ts)"))

        if (!ttsReady) {
            pendingSpeech = spokenText
            return
        }

        try {
            tts?.stop()
            val params = Bundle()
            for (i in 0 until repeatCount) {
                val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(spokenText, queueMode, params, "suzy_call_announce_$i")
                if (i < repeatCount - 1) {
                    tts?.playSilentUtterance(1100L, TextToSpeech.QUEUE_ADD, "suzy_pause_$i")
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun speakNow(text: String) {
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "suzy_custom_utterance")
        } catch (_: Exception) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Suzy Background Guard")
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
                "Suzy Background Call Guard",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps incoming call voice announcer ready in background"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        AnnouncerStateBridge.isGuardRunning = false
        try {
            dynamicReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
