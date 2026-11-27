package com.suzy.ai.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryTelemetry(
    val percent: Int,
    val isCharging: Boolean
)

class SuzyTools(private val context: Context) {

    var isTorchOn: Boolean = false
        private set

    fun toggleFlashlight(enable: Boolean): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "Is phone mein flashlight available nahi hai."

            cm.setTorchMode(cameraId, enable)
            isTorchOn = enable
            if (enable) "Flashlight ON kar di hai." else "Flashlight OFF kar di hai."
        } catch (e: Exception) {
            "Flashlight error: ${e.localizedMessage}"
        }
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryTelemetry(percent = pct, isCharging = charging)
    }

    suspend fun measureGeminiPingMs(): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("generativelanguage.googleapis.com", 443), 3000)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            -1
        }
    }

    fun getDeviceStatus(): String {
        val bat = getBatteryTelemetry()
        val now = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
        return JSONObject().apply {
            put("battery_percent", bat.percent)
            put("is_charging", bat.isCharging)
            put("current_time", now)
            put("flashlight_on", isTorchOn)
        }.toString()
    }

    fun setVolume(percent: Int): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val clamped = percent.coerceIn(0, 100)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, target, 0)
            "Volume $clamped percent set kar diya hai."
        } catch (e: Exception) {
            "Volume change nahi ho paya: ${e.localizedMessage}"
        }
    }

    fun webSearch(query: String): String {
        return try {
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Google par '$query' search open kar diya hai."
        } catch (e: Exception) {
            "Browser open nahi ho paya."
        }
    }

    fun executeTool(name: String, args: JSONObject): String {
        return when (name) {
            "toggle_flashlight" -> toggleFlashlight(args.optBoolean("enable", true))
            "get_device_status" -> getDeviceStatus()
            "set_volume" -> setVolume(args.optInt("level_percent", 70))
            "web_search" -> webSearch(args.optString("query", ""))
            else -> "Unknown tool: $name"
        }
    }
}
