package com.suzy.ai.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

enum class AudioBlockReason {
    NONE,
    MEDIA_PLAYING,
    PHONE_CALL_ACTIVE,
    ANNOUNCING
}

class AudioEnvironmentGuard(
    private val context: Context,
    private val isSuzyLiveSessionActive: () -> Boolean,
    private val isMediaShieldEnabled: () -> Boolean,
    private val isCallShieldEnabled: () -> Boolean,
    private val onStateChanged: (AudioBlockReason) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var telephonyBusyFromReceiver: Boolean = false

    @Volatile
    private var isAnnouncingAudio: Boolean = false

    @Volatile
    private var activeMediaConfigPlaying: Boolean = false

    @Volatile
    private var lastMediaStoppedMs: Long = 0L

    @Volatile
    var currentReason: AudioBlockReason = AudioBlockReason.NONE
        private set

    private var playbackCallback: Any? = null
    private var pollRunnable: Runnable? = null

    fun startMonitoring() {
        registerPlaybackCallback()
        startPolling()
        evaluateNow()
    }

    fun stopMonitoring() {
        unregisterPlaybackCallback()
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        pollRunnable = null
    }

    fun setTelephonyCallBusy(busy: Boolean) {
        telephonyBusyFromReceiver = busy
        evaluateNow()
    }

    fun setAnnouncing(announcing: Boolean) {
        isAnnouncingAudio = announcing
        evaluateNow()
    }

    fun evaluateNow(): AudioBlockReason {
        val next = computeBlockReason()
        if (next != currentReason) {
            currentReason = next
            mainHandler.post { onStateChanged(next) }
        }
        return next
    }

    private fun computeBlockReason(): AudioBlockReason {
        if (isAnnouncingAudio) {
            return AudioBlockReason.ANNOUNCING
        }

        if (isCallShieldEnabled() && isAnyPhoneOrVoipCallActive()) {
            return AudioBlockReason.PHONE_CALL_ACTIVE
        }

        if (isMediaShieldEnabled() && isExternalMediaPlaying()) {
            return AudioBlockReason.MEDIA_PLAYING
        }

        return AudioBlockReason.NONE
    }

    private fun isAnyPhoneOrVoipCallActive(): Boolean {
        if (telephonyBusyFromReceiver) return true

        // Check cellular call state safely
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                @Suppress("DEPRECATION")
                val callState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
                if (callState == TelephonyManager.CALL_STATE_RINGING ||
                    callState == TelephonyManager.CALL_STATE_OFFHOOK
                ) {
                    return true
                }
            } catch (_: Exception) {
            }
        }

        // Check audio routing mode (Cellular call or 3rd-party VoIP like WhatsApp/Telegram)
        return try {
            val mode = audioManager.mode
            if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_RINGTONE) {
                true
            } else if (mode == AudioManager.MODE_IN_COMMUNICATION && !isSuzyLiveSessionActive()) {
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isExternalMediaPlaying(): Boolean {
        if (isSuzyLiveSessionActive()) return false

        val musicActive = try {
            audioManager.isMusicActive
        } catch (_: Exception) {
            false
        }

        val playingNow = musicActive || activeMediaConfigPlaying
        if (playingNow) {
            lastMediaStoppedMs = System.currentTimeMillis()
            return true
        }

        // 1.5s cooldown after video/audio pauses so trailing speaker audio never triggers mic
        val sinceStopped = System.currentTimeMillis() - lastMediaStoppedMs
        return lastMediaStoppedMs > 0L && sinceStopped < 1500L
    }

    private fun registerPlaybackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val cb = object : AudioManager.AudioPlaybackCallback() {
                    override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                        var mediaDetected = false
                        configs?.forEach { cfg ->
                            val usage = cfg.audioAttributes.usage
                            if (usage == AudioAttributes.USAGE_MEDIA ||
                                usage == AudioAttributes.USAGE_GAME ||
                                usage == AudioAttributes.USAGE_UNKNOWN
                            ) {
                                mediaDetected = true
                            }
                        }
                        if (activeMediaConfigPlaying && !mediaDetected) {
                            lastMediaStoppedMs = System.currentTimeMillis()
                        }
                        activeMediaConfigPlaying = mediaDetected
                        evaluateNow()
                    }
                }
                playbackCallback = cb
                audioManager.registerAudioPlaybackCallback(cb, mainHandler)
            } catch (_: Exception) {
            }
        }
    }

    private fun unregisterPlaybackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                (playbackCallback as? AudioManager.AudioPlaybackCallback)?.let {
                    audioManager.unregisterAudioPlaybackCallback(it)
                }
            } catch (_: Exception) {
            }
        }
        playbackCallback = null
    }

    private fun startPolling() {
        val r = object : Runnable {
            override fun run() {
                evaluateNow()
                mainHandler.postDelayed(this, 700L)
            }
        }
        pollRunnable = r
        mainHandler.postDelayed(r, 700L)
    }
}
