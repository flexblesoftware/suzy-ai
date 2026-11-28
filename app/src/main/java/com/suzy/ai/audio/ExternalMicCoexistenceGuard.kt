package com.suzy.ai.audio

import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExternalMicCoexistenceGuard(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onExternalMicStateChanged: (isYielded: Boolean, activeAppLabel: String) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isYieldedToExternalApp: Boolean = false
        private set

    @Volatile
    var activeExternalReason: String = ""
        private set

    @Volatile
    var isSuzyActivelyRecording: Boolean = false

    @Volatile
    private var cooldownUntilMs: Long = 0L

    @Volatile
    var enabled: Boolean = true

    private var monitorJob: Job? = null

    private val externalVoicePackages = setOf(
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.bard",
        "com.anthropic.claude",
        "com.openai.chatgpt",
        "com.microsoft.copilot",
        "ai.perplexity.app.android",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.android.soundrecorder",
        "com.sec.android.app.voicenote",
        "com.oneplus.soundrecorder",
        "com.miui.voicerecorder"
    )

    private val recordingCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>?) {
                val count = configs?.size ?: 0
                val externalActive = if (isSuzyActivelyRecording) count > 1 else count > 0
                if (externalActive) {
                    cooldownUntilMs = System.currentTimeMillis() + 14_000L
                    setYieldState(true, "External App Using Mic (Gemini/Claude Safe)")
                }
            }
        }
    } else null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingCallback != null) {
            try {
                audioManager?.registerAudioRecordingCallback(recordingCallback, mainHandler)
            } catch (_: Exception) {
            }
        }
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                evaluateCoexistence()
                delay(750L)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingCallback != null) {
            try {
                audioManager?.unregisterAudioRecordingCallback(recordingCallback)
            } catch (_: Exception) {
            }
        }
    }

    fun triggerManualYield(seconds: Int = 20, label: String = "Manual Mic Yield") {
        cooldownUntilMs = System.currentTimeMillis() + (seconds * 1000L)
        setYieldState(true, label)
    }

    private fun evaluateCoexistence() {
        if (!enabled) {
            if (isYieldedToExternalApp) setYieldState(false, "")
            return
        }

        val now = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val count = try {
                audioManager?.activeRecordingConfigurations?.size ?: 0
            } catch (_: Exception) { 0 }
            val externalActive = if (isSuzyActivelyRecording) count > 1 else count > 0
            if (externalActive) {
                cooldownUntilMs = now + 14_000L
                setYieldState(true, "External App Mic Active")
                return
            }
        }

        val fgPkg = detectForegroundPackage()
        if (fgPkg != null && fgPkg != context.packageName && externalVoicePackages.contains(fgPkg)) {
            cooldownUntilMs = now + 5_000L
            val niceName = when {
                fgPkg.contains("bard") || fgPkg.contains("quicksearchbox") -> "Google Gemini"
                fgPkg.contains("claude") -> "Claude AI"
                fgPkg.contains("chatgpt") -> "ChatGPT"
                fgPkg.contains("whatsapp") -> "WhatsApp"
                else -> "Voice App"
            }
            setYieldState(true, "Mic Yielded to $niceName")
            return
        }

        if (now < cooldownUntilMs) {
            if (!isYieldedToExternalApp) {
                setYieldState(true, "Mic Cooldown for External AI")
            }
            return
        }

        if (isYieldedToExternalApp) {
            setYieldState(false, "")
        }
    }

    private fun detectForegroundPackage(): String? {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val end = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 10_000L, end)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (_: Exception) {
            null
        }
    }

    private fun setYieldState(yielded: Boolean, reason: String) {
        if (isYieldedToExternalApp != yielded || activeExternalReason != reason) {
            isYieldedToExternalApp = yielded
            activeExternalReason = reason
            onExternalMicStateChanged(yielded, reason)
        }
    }
}
