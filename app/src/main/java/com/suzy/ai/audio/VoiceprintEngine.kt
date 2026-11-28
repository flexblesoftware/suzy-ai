package com.suzy.ai.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class VoiceprintFeatures(
    val pitchHz: Float,
    val pitchSpreadHz: Float,
    val zeroCrossingRate: Float,
    val lowBandRatio: Float,
    val midBandRatio: Float,
    val highBandRatio: Float,
    val voicedFramesCount: Int
)

data class VoiceVerificationResult(
    val matchPercent: Int,
    val isMatch: Boolean,
    val detectedPitchHz: Float,
    val reason: String
)

object VoiceprintEngine {

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    fun getSampleFile(context: Context, slotIndex: Int): File {
        val dir = File(context.filesDir, "owner_voiceprints")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "owner_sample_$slotIndex.wav")
    }

    fun isSampleRecorded(context: Context, slotIndex: Int): Boolean {
        val f = getSampleFile(context, slotIndex)
        return f.exists() && f.length() > 4096L
    }

    fun deleteSample(context: Context, slotIndex: Int) {
        val f = getSampleFile(context, slotIndex)
        if (f.exists()) f.delete()
    }

    fun deleteAllSamples(context: Context) {
        for (i in 1..3) deleteSample(context, i)
    }

    @SuppressLint("MissingPermission")
    suspend fun recordVoiceSample(
        context: Context,
        slotIndex: Int?,
        durationMs: Long = 3500L,
        onLiveTelemetry: (amplitudeNorm: Float, dbLevel: Float, progress: Float) -> Unit
    ): Result<VoiceprintFeatures> = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        if (minBuf <= 0) {
            return@withContext Result.failure(IllegalStateException("AudioRecord buffer initialization failed"))
        }

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_IN,
                ENCODING,
                max(minBuf * 2, 4096)
            )
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            try { recorder.release() } catch (_: Exception) {}
            return@withContext Result.failure(IllegalStateException("Microphone busy or permission denied"))
        }

        val totalSamplesTarget = ((SAMPLE_RATE * durationMs) / 1000L).toInt()
        val capturedSamples = ShortArray(totalSamplesTarget)
        val chunkBuf = ShortArray(1024)
        var offset = 0

        try {
            recorder.startRecording()
            while (offset < totalSamplesTarget) {
                val toRead = min(chunkBuf.size, totalSamplesTarget - offset)
                val readCount = recorder.read(chunkBuf, 0, toRead)
                if (readCount > 0) {
                    System.arraycopy(chunkBuf, 0, capturedSamples, offset, readCount)
                    offset += readCount

                    var sumSq = 0.0
                    for (i in 0 until readCount) {
                        val s = chunkBuf[i].toDouble()
                        sumSq += s * s
                    }
                    val rms = sqrt(sumSq / readCount)
                    val norm = (rms / 9000.0).toFloat().coerceIn(0f, 1f)
                    val db = if (rms > 1.0) (20.0 * log10(rms / 32768.0)).toFloat().coerceIn(-60f, 0f) else -60f
                    val progress = (offset.toFloat() / totalSamplesTarget.toFloat()).coerceIn(0f, 1f)
                    onLiveTelemetry(norm, db, progress)
                } else if (readCount < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            try { recorder.release() } catch (_: Exception) {}
        }

        val validSamples = if (offset > 0) capturedSamples.copyOf(offset) else ShortArray(0)
        val features = extractAcousticFeatures(validSamples)
            ?: return@withContext Result.failure(IllegalStateException("Aawaz bahut dheemi thi — thoda paas se saaf bolein"))

        if (slotIndex != null) {
            saveWavFile(getSampleFile(context, slotIndex), validSamples)
        }

        Result.success(features)
    }

    suspend fun playSample(context: Context, slotIndex: Int, onFinished: () -> Unit) = withContext(Dispatchers.IO) {
        val file = getSampleFile(context, slotIndex)
        if (!file.exists() || file.length() <= 44L) {
            withContext(Dispatchers.Main) { onFinished() }
            return@withContext
        }

        try {
            val bytes = file.readBytes()
            val pcmData = bytes.copyOfRange(44, bytes.size)
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                ENCODING
            )
            @Suppress("DEPRECATION")
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                ENCODING,
                max(minBuf, pcmData.size),
                AudioTrack.MODE_STATIC
            )
            track.write(pcmData, 0, pcmData.size)
            track.play()
            val durationMs = ((pcmData.size / 2L) * 1000L) / SAMPLE_RATE
            delay(durationMs + 150L)
            track.stop()
            track.release()
        } catch (_: Exception) {
        } finally {
            withContext(Dispatchers.Main) { onFinished() }
        }
    }

    fun computeMasterSignature(context: Context): VoiceprintFeatures? {
        val list = mutableListOf<VoiceprintFeatures>()
        for (slot in 1..3) {
            val f = getSampleFile(context, slot)
            if (f.exists() && f.length() > 4096L) {
                val bytes = f.readBytes()
                val pcmBytes = bytes.copyOfRange(44, bytes.size)
                val shorts = ShortArray(pcmBytes.size / 2)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                extractAcousticFeatures(shorts)?.let { list.add(it) }
            }
        }
        if (list.isEmpty()) return null
        return VoiceprintFeatures(
            pitchHz = list.map { it.pitchHz }.average().toFloat(),
            pitchSpreadHz = list.map { it.pitchSpreadHz }.average().toFloat(),
            zeroCrossingRate = list.map { it.zeroCrossingRate }.average().toFloat(),
            lowBandRatio = list.map { it.lowBandRatio }.average().toFloat(),
            midBandRatio = list.map { it.midBandRatio }.average().toFloat(),
            highBandRatio = list.map { it.highBandRatio }.average().toFloat(),
            voicedFramesCount = list.sumOf { it.voicedFramesCount }
        )
    }

    fun compareWithOwner(
        enrolled: VoiceprintFeatures,
        live: VoiceprintFeatures,
        thresholdPercent: Int
    ): VoiceVerificationResult {
        val pitchDiff = abs(enrolled.pitchHz - live.pitchHz)
        val pitchSim = exp(-(pitchDiff * pitchDiff) / (2.0 * 26.0 * 26.0)).toFloat()

        val dot = (enrolled.lowBandRatio * live.lowBandRatio) +
            (enrolled.midBandRatio * live.midBandRatio) +
            (enrolled.highBandRatio * live.highBandRatio)
        val magA = sqrt(
            enrolled.lowBandRatio * enrolled.lowBandRatio +
                enrolled.midBandRatio * enrolled.midBandRatio +
                enrolled.highBandRatio * enrolled.highBandRatio
        )
        val magB = sqrt(
            live.lowBandRatio * live.lowBandRatio +
                live.midBandRatio * live.midBandRatio +
                live.highBandRatio * live.highBandRatio
        )
        val spectralSim = if (magA > 0f && magB > 0f) (dot / (magA * magB)).coerceIn(0f, 1f) else 0f

        val zcrDiff = abs(enrolled.zeroCrossingRate - live.zeroCrossingRate)
        val zcrSim = (1f - (zcrDiff / 0.18f)).coerceIn(0f, 1f)

        val weightedScore = ((pitchSim * 0.48f) + (spectralSim * 0.37f) + (zcrSim * 0.15f)) * 100f
        val percent = weightedScore.toInt().coerceIn(5, 99)
        val passed = percent >= thresholdPercent

        val reason = if (passed) {
            "Owner Voice Verified (${percent}% match • ${live.pitchHz.toInt()} Hz)"
        } else {
            "Voice Mismatch (${percent}% < ${thresholdPercent}% • Detected ${live.pitchHz.toInt()} Hz vs Owner ${enrolled.pitchHz.toInt()} Hz)"
        }
        return VoiceVerificationResult(
            matchPercent = percent,
            isMatch = passed,
            detectedPitchHz = live.pitchHz,
            reason = reason
        )
    }

    private fun extractAcousticFeatures(samples: ShortArray): VoiceprintFeatures? {
        if (samples.size < SAMPLE_RATE / 2) return null
        val frameSize = 512
        val pitches = mutableListOf<Float>()
        val zcrs = mutableListOf<Float>()
        var lowSum = 0.0
        var midSum = 0.0
        var highSum = 0.0
        var voicedFrames = 0

        var idx = 0
        while (idx + frameSize <= samples.size) {
            var rmsSum = 0.0
            var zeroCrossings = 0
            for (i in 0 until frameSize) {
                val s = samples[idx + i].toDouble()
                rmsSum += s * s
                if (i > 0) {
                    val prev = samples[idx + i - 1].toDouble()
                    if ((s >= 0 && prev < 0) || (s < 0 && prev >= 0)) zeroCrossings++
                }
            }
            val rms = sqrt(rmsSum / frameSize)
            if (rms > 420.0) {
                voicedFrames++
                zcrs.add(zeroCrossings.toFloat() / frameSize.toFloat())

                val minLag = SAMPLE_RATE / 310
                val maxLag = SAMPLE_RATE / 75
                var bestLag = -1
                var bestCorr = 0.0
                for (lag in minLag..maxLag) {
                    var corr = 0.0
                    for (k in 0 until (frameSize - lag)) {
                        corr += samples[idx + k].toDouble() * samples[idx + k + lag].toDouble()
                    }
                    if (corr > bestCorr) {
                        bestCorr = corr
                        bestLag = lag
                    }
                }
                if (bestLag > 0) {
                    pitches.add(SAMPLE_RATE.toFloat() / bestLag.toFloat())
                }

                lowSum += goertzelEnergy(samples, idx, frameSize, 220.0)
                midSum += goertzelEnergy(samples, idx, frameSize, 1000.0)
                highSum += goertzelEnergy(samples, idx, frameSize, 3200.0)
            }
            idx += frameSize
        }

        if (voicedFrames < 6 || pitches.isEmpty()) return null

        val sortedPitches = pitches.sorted()
        val medianPitch = sortedPitches[sortedPitches.size / 2]
        val variance = pitches.map { (it - medianPitch) * (it - medianPitch) }.average()
        val spread = sqrt(variance).toFloat()

        val totalBand = (lowSum + midSum + highSum).coerceAtLeast(1.0)
        return VoiceprintFeatures(
            pitchHz = medianPitch,
            pitchSpreadHz = spread,
            zeroCrossingRate = zcrs.average().toFloat(),
            lowBandRatio = (lowSum / totalBand).toFloat(),
            midBandRatio = (midSum / totalBand).toFloat(),
            highBandRatio = (highSum / totalBand).toFloat(),
            voicedFramesCount = voicedFrames
        )
    }

    private fun goertzelEnergy(samples: ShortArray, offset: Int, len: Int, targetFreq: Double): Double {
        val k = (0.5 + (len * targetFreq) / SAMPLE_RATE).toInt()
        val omega = (2.0 * Math.PI * k) / len
        val coeff = 2.0 * cos(omega)
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until len) {
            q0 = coeff * q1 - q2 + (samples[offset + i] / 32768.0)
            q2 = q1
            q1 = q0
        }
        return max(0.0, q1 * q1 + q2 * q2 - q1 * q2 * coeff)
    }

    private fun saveWavFile(file: File, samples: ShortArray) {
        val byteLen = samples.size * 2
        val totalDataLen = byteLen + 36
        val byteRate = SAMPLE_RATE * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(byteLen)
        }.array()

        val pcmBuffer = ByteBuffer.allocate(byteLen).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { pcmBuffer.putShort(it) }

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBuffer.array())
            out.flush()
        }
    }
}
