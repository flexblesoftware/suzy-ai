package com.suzy.ai.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun getDeviceStatus(): String {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0) (level * 100) / scale else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val now = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
        return JSONObject().apply {
            put("battery_percent", pct)
            put("is_charging", charging)
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

    fun openApp(appName: String): String {
        val query = appName.lowercase(Locale.getDefault()).trim()
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "telegram" to "org.telegram.messenger",
            "spotify" to "com.spotify.music",
            "settings" to "com.android.settings"
        )

        val pm = context.packageManager
        val directPkg = knownPackages.entries.firstOrNull { query.contains(it.key) }?.value

        val launchIntent = if (directPkg != null) {
            pm.getLaunchIntentForPackage(directPkg)
        } else {
            val installed = pm.getInstalledApplications(0)
            val match = installed.firstOrNull { app ->
                pm.getApplicationLabel(app).toString().lowercase(Locale.getDefault()).contains(query)
            }
            match?.let { pm.getLaunchIntentForPackage(it.packageName) }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "$appName open kar diya hai."
        } else {
            "$appName phone mein install nahi mila."
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
            "open_app" -> openApp(args.optString("app_name", ""))
            "web_search" -> webSearch(args.optString("query", ""))
            else -> "Unknown tool: $name"
        }
    }
}
