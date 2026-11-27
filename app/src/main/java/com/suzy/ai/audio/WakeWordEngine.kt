package com.suzy.ai.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

class WakeWordEngine(
    private val context: Context,
    private val canListenNow: () -> Boolean,
    private val onWakeWordDetected: (trailingQuery: String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile
    private var userEnabled: Boolean = false

    @Volatile
    private var isCurrentlyListening: Boolean = false

    @Volatile
    private var activePersona: String = "Suzy"

    private var restartRunnable: Runnable? = null

    fun start(personaName: String) {
        activePersona = personaName.ifBlank { "Suzy" }
        userEnabled = true
        mainHandler.post {
            if (!canListenNow()) {
                suspendTemporarily()
            } else if (!isCurrentlyListening) {
                beginListeningInternal()
            }
        }
    }

    fun suspendTemporarily() {
        mainHandler.post {
            cancelPendingRestart()
            destroyRecognizer()
            if (isCurrentlyListening) {
                isCurrentlyListening = false
                onListeningStateChanged(false)
            } else {
                onListeningStateChanged(false)
            }
        }
    }

    fun resumeIfPermitted() {
        mainHandler.post {
            if (!userEnabled) return@post
            if (!canListenNow()) {
                suspendTemporarily()
                return@post
            }
            if (!isCurrentlyListening && restartRunnable == null) {
                scheduleRestart(900L)
            }
        }
    }

    fun stop() {
        userEnabled = false
        mainHandler.post {
            cancelPendingRestart()
            destroyRecognizer()
            isCurrentlyListening = false
            onListeningStateChanged(false)
        }
    }

    fun destroy() {
        stop()
    }

    private fun cancelPendingRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun scheduleRestart(delayMs: Long) {
        cancelPendingRestart()
        if (!userEnabled) return
        val runnable = Runnable {
            restartRunnable = null
            if (!userEnabled) return@Runnable
            if (!canListenNow()) {
                suspendTemporarily()
                return@Runnable
            }
            beginListeningInternal()
        }
        restartRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun beginListeningInternal() {
        if (!userEnabled || !canListenNow()) {
            suspendTemporarily()
            return
        }

        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic || !SpeechRecognizer.isRecognitionAvailable(context)) {
            isCurrentlyListening = false
            onListeningStateChanged(false)
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            isCurrentlyListening = true
            onListeningStateChanged(true)
        } catch (_: Exception) {
            isCurrentlyListening = false
            onListeningStateChanged(false)
            destroyRecognizer()
            scheduleRestart(2500L)
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (userEnabled && canListenNow()) {
                    isCurrentlyListening = true
                    onListeningStateChanged(true)
                } else {
                    suspendTemporarily()
                }
            }

            override fun onBeginningOfSpeech() {
                if (!canListenNow()) {
                    suspendTemporarily()
                }
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isCurrentlyListening = false
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                onListeningStateChanged(false)
                if (!userEnabled) return
                if (!canListenNow()) {
                    suspendTemporarily()
                    return
                }
                val backoff = when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        destroyRecognizer()
                        2200L
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        userEnabled = false
                        return
                    }
                    else -> 1200L
                }
                scheduleRestart(backoff)
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                if (!canListenNow()) {
                    suspendTemporarily()
                    return
                }
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (handleTranscripts(matches)) return
                scheduleRestart(1000L)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!canListenNow()) {
                    suspendTemporarily()
                    return
                }
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                handleTranscripts(matches)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleTranscripts(candidates: List<String>): Boolean {
        if (!userEnabled || !canListenNow() || candidates.isEmpty()) return false

        val personaLower = activePersona.lowercase(Locale.US).trim()
        val wakeAliases = buildList {
            add("hey suzy")
            add("hi suzy")
            add("hello suzy")
            add("oye suzy")
            add("ok suzy")
            add("suzy")
            add("suzi")
            add("susi")
            add("soozy")
            if (personaLower.isNotBlank() && personaLower != "suzy") {
                add("hey $personaLower")
                add("hi $personaLower")
                add("oye $personaLower")
                add(personaLower)
            }
        }

        for (raw in candidates) {
            val normalized = raw.lowercase(Locale.US).trim()
            for (alias in wakeAliases) {
                val idx = normalized.indexOf(alias)
                if (idx >= 0) {
                    val trailing = raw.substring((idx + alias.length).coerceAtMost(raw.length))
                        .trim(' ', ',', '.', '!', '?')
                    stop()
                    onWakeWordDetected(trailing)
                    return true
                }
            }
        }
        return false
    }
}
