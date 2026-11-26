package com.suzy.ai.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

class AudioEngine(
    context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private var playbackChannel: Channel<ByteArray>? = null

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isSpeakerOn: Boolean = true

    companion object {
        const val INPUT_SAMPLE_RATE = 16000
        const val OUTPUT_SAMPLE_RATE = 24000
        const val MIC_GAIN = 1.15f
    }

    fun playUiChime(connected: Boolean) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 55)
            val tone = if (connected) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_NACK
            tg.startTone(tone, 140)
            scope.launch(Dispatchers.IO) {
                delay(220)
                try { tg.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        }
    }

    fun setSpeakerphone(enabled: Boolean) {
        isSpeakerOn = enabled
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val targetType = if (enabled) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                val device = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == targetType
                }
                if (device != null) {
                    audioManager.setCommunicationDevice(device)
                }
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = enabled
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onAudioChunk: (ByteArray) -> Unit,
        onAmplitude: (Float) -> Unit
    ) {
        stopRecording()

        val minBuf = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(3200)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply {
                enabled = true
            }
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(recorder.audioSessionId)?.apply {
                enabled = true
            }
        }

        audioRecord = recorder
        recorder.startRecording()

        recordJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1600) // 50ms chunks at 16kHz 16-bit mono
            val silenceChunk = ByteArray(1600)
            val preBuffer = ArrayDeque<ByteArray>(4)

            var ambientNoiseFloor = 0.015f
            var voicedFrameStreak = 0
            var gateOpen = false
            var hangoverFrames = 0

            while (isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (isMuted) {
                        gateOpen = false
                        voicedFrameStreak = 0
                        preBuffer.clear()
                        onAmplitude(0f)
                        onAudioChunk(silenceChunk)
                        continue
                    }

                    val chunk = applyMicGain(buffer, read, MIC_GAIN)
                    val rms = computeRms(chunk)
                    val zcr = computeZeroCrossingRate(chunk)

                    // Close-mic primary voice threshold (blocks distant voices & fan/TV noise)
                    val dynamicThreshold = max(0.055f, ambientNoiseFloor * 2.8f)

                    // Human voiced speech has lower ZCR (0.01..0.28); sneezes/coughs/claps have high ZCR or brief spikes
                    val isLikelyHumanVoice = rms > dynamicThreshold && zcr in 0.01f..0.28f

                    if (!gateOpen) {
                        if (isLikelyHumanVoice) {
                            voicedFrameStreak++
                            if (preBuffer.size >= 3) preBuffer.removeFirst()
                            preBuffer.addLast(chunk)

                            // Require 3 consecutive voiced frames (~150ms) to reject sudden coughs/sneezes
                            if (voicedFrameStreak >= 3) {
                                gateOpen = true
                                hangoverFrames = 14 // ~700ms smooth tail so sentences never clip
                                while (preBuffer.isNotEmpty()) {
                                    onAudioChunk(preBuffer.removeFirst())
                                }
                                onAmplitude(rms)
                            } else {
                                onAmplitude(0f)
                                onAudioChunk(silenceChunk)
                            }
                        } else {
                            voicedFrameStreak = 0
                            preBuffer.clear()
                            // Slowly adapt background noise floor when quiet
                            if (rms < 0.08f) {
                                ambientNoiseFloor = (ambientNoiseFloor * 0.96f) + (rms * 0.04f)
                            }
                            onAmplitude(0f)
                            onAudioChunk(silenceChunk)
                        }
                    } else {
                        if (rms > dynamicThreshold * 0.7f && zcr <= 0.34f) {
                            hangoverFrames = 14
                        } else {
                            hangoverFrames--
                        }

                        if (hangoverFrames > 0) {
                            onAmplitude(rms)
                            onAudioChunk(chunk)
                        } else {
                            gateOpen = false
                            voicedFrameStreak = 0
                            onAmplitude(0f)
                            onAudioChunk(silenceChunk)
                        }
                    }
                }
            }
        }
    }

    fun startPlayback() {
        stopPlayback()
        setSpeakerphone(isSpeakerOn)

        val minBuf = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4800)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack = track
        track.play()

        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        playbackChannel = channel
        playbackJob = scope.launch(Dispatchers.IO) {
            for (chunk in channel) {
                if (!isActive) break
                try {
                    track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun playPcmChunk(pcmBytes: ByteArray, onAmplitude: (Float) -> Unit) {
        onAmplitude(computeRms(pcmBytes))
        playbackChannel?.trySend(pcmBytes)
    }

    fun flushPlayback() {
        try {
            while (playbackChannel?.tryReceive()?.isSuccess == true) {
                // Drain queued audio immediately for instant interruption
            }
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (_: Exception) {
        }
    }

    fun stopRecording() {
        recordJob?.cancel()
        recordJob = null
        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        echoCanceler = null
        noiseSuppressor = null
        audioRecord = null
    }

    fun stopPlayback() {
        playbackChannel?.close()
        playbackChannel = null
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }

    fun releaseAll() {
        stopRecording()
        stopPlayback()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    private fun applyMicGain(src: ByteArray, len: Int, gain: Float): ByteArray {
        val out = ByteArray(len)
        var i = 0
        while (i < len - 1) {
            val sample = ((src[i + 1].toInt() shl 8) or (src[i].toInt() and 0xFF)).toShort().toInt()
            val boosted = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i] = (boosted and 0xFF).toByte()
            out[i + 1] = ((boosted shr 8) and 0xFF).toByte()
            i += 2
        }
        return out
    }

    private fun computeRms(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f
        var sum = 0.0
        var count = 0
        var i = 0
        while (i < pcm.size - 1) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toDouble()
            sum += sample * sample
            count++
            i += 2
        }
        if (count == 0) return 0f
        val rms = sqrt(sum / count) / 32768.0
        return (rms * 4.2).toFloat().coerceIn(0f, 1f)
    }

    private fun computeZeroCrossingRate(pcm: ByteArray): Float {
        if (pcm.size < 4) return 0f
        var crossings = 0
        var prevSample = ((pcm[1].toInt() shl 8) or (pcm[0].toInt() and 0xFF)).toShort().toInt()
        var count = 1
        var i = 2
        while (i < pcm.size - 1) {
            val currSample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toInt()
            if ((prevSample >= 0 && currSample < 0) || (prevSample < 0 && currSample >= 0)) {
                crossings++
            }
            prevSample = currSample
            count++
            i += 2
        }
        return if (count > 0) crossings.toFloat() / count.toFloat() else 0f
    }
}
