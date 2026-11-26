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
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isSpeakerOn: Boolean = true

    companion object {
        const val INPUT_SAMPLE_RATE = 16000
        const val OUTPUT_SAMPLE_RATE = 24000
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
            val buffer = ByteArray(1600)
            while (isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (isMuted) {
                        onAmplitude(0f)
                        continue
                    }
                    val chunk = buffer.copyOf(read)
                    onAmplitude(computeRms(chunk))
                    onAudioChunk(chunk)
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

        audioTrack = AudioTrack.Builder()
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
            .build()

        audioTrack?.play()
    }

    fun playPcmChunk(pcmBytes: ByteArray, onAmplitude: (Float) -> Unit) {
        onAmplitude(computeRms(pcmBytes))
        audioTrack?.write(pcmBytes, 0, pcmBytes.size)
    }

    fun flushPlayback() {
        try {
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
        return (rms * 4.5).toFloat().coerceIn(0f, 1f)
    }
}
