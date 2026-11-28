package com.suzy.ai.tools

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.suzy.ai.service.SuzyAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

data class BatteryTelemetry(
    val percent: Int,
    val isCharging: Boolean,
    val tempCelsius: Float
)

class SuzyTools(private val context: Context) {

    @Volatile
    var isTorchOn: Boolean = false
        private set

    fun toggleFlashlight(enable: Boolean): String {
        return try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return "CameraManager unavailable"
            var targetCamId: String? = null
            for (id in camManager.cameraIdList) {
                val chars = camManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCamId = id
                    break
                }
                if (hasFlash && targetCamId == null) {
                    targetCamId = id
                }
            }
            val camId = targetCamId ?: return "Flashlight hardware not found"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                camManager.setTorchMode(camId, enable)
                isTorchOn = enable
                if (enable) "Flashlight ON kar di gayi hai" else "Flashlight OFF kar di gayi hai"
            } else {
                "Android 6+ required for Torch"
            }
        } catch (e: Exception) {
            "Flashlight error: ${e.message}"
        }
    }

    fun setDndMode(enable: Boolean, level: String = "PRIORITY"): String {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return "NotificationManager unavailable"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!nm.isNotificationPolicyAccessGranted) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return "DND permission missing — DND Access settings khol diya hai, please allow karein"
                }
                val filter = if (!enable) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                } else if (level.equals("TOTAL", ignoreCase = true) || level.equals("SILENT", ignoreCase = true)) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                }
                nm.setInterruptionFilter(filter)
                if (enable) "Do Not Disturb (DND) ON kar diya gaya hai" else "Do Not Disturb (DND) OFF kar diya gaya hai"
            } else {
                "Android 6+ required for DND"
            }
        } catch (e: Exception) {
            "DND error: ${e.message}"
        }
    }

    fun setBrightnessPercent(percent: Int): String {
        val clamped = percent.coerceIn(5, 100)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Modify System Settings permission khol diya hai — allow karke phir bolein"
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val rawVal = ((clamped / 100f) * 255f).toInt().coerceIn(8, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, rawVal)
            "Screen Brightness $clamped% set kar di gayi hai"
        } catch (e: Exception) {
            "Brightness error: ${e.message}"
        }
    }

    fun setAutoRotate(enable: Boolean): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Write Settings permission required for Auto-Rotate"
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enable) 1 else 0
            )
            if (enable) "Auto-Rotate ON kar diya gaya hai" else "Auto-Rotate OFF kar diya gaya hai"
        } catch (e: Exception) {
            "Auto-Rotate error: ${e.message}"
        }
    }

    fun setVolumePercent(streamName: String, percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val stream = when (streamName.lowercase()) {
                "alarm" -> AudioManager.STREAM_ALARM
                "ring", "ringer" -> AudioManager.STREAM_RING
                "call", "voice" -> AudioManager.STREAM_VOICE_CALL
                else -> AudioManager.STREAM_MUSIC
            }
            val max = am.getStreamMaxVolume(stream)
            val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
            am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
            "${streamName.uppercase()} volume $clamped% set ho gaya"
        } catch (e: Exception) {
            "Volume error: ${e.message}"
        }
    }

    fun controlConnectivity(target: String, desiredState: String = "toggle"): String {
        val key = target.lowercase().trim()
        val hasAcc = SuzyAccessibilityService.isConnected()

        return when {
            key.contains("data") || key.contains("internet") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Mobile data", "Cellular data", "Data", "Internet"),
                        "Mobile Data"
                    )
                    "Accessibility Commander se Mobile Data toggle kar diya gaya hai"
                } else {
                    openConnectivityPanel("data")
                    "Mobile Data panel khol diya hai (Direct auto-toggle ke liye Settings > Permission Manager mein Accessibility ON karein)"
                }
            }

            key.contains("hotspot") || key.contains("tether") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Hotspot", "Personal hotspot", "Mobile Hotspot", "Wi-Fi hotspot"),
                        "Personal Hotspot"
                    )
                    "Accessibility Commander se Hotspot toggle kar diya gaya hai"
                } else {
                    openConnectivityPanel("hotspot")
                    "Hotspot settings khol diya hai (Direct auto-toggle ke liye Accessibility Service ON karein)"
                }
            }

            key.contains("wifi") || key.contains("wi-fi") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Wi-Fi", "WLAN", "Internet"),
                        "Wi-Fi"
                    )
                    "Accessibility Commander se Wi-Fi toggle kar diya gaya hai"
                } else {
                    openConnectivityPanel("wifi")
                    "Wi-Fi quick panel khol diya hai (Direct auto-toggle ke liye Accessibility Service ON karein)"
                }
            }

            key.contains("bluetooth") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Bluetooth"),
                        "Bluetooth"
                    )
                    "Accessibility Commander se Bluetooth toggle kar diya gaya hai"
                } else {
                    openConnectivityPanel("bluetooth")
                    "Bluetooth settings khol diya hai"
                }
            }

            key.contains("airplane") || key.contains("flight") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Airplane", "Flight mode", "Aeroplane"),
                        "Airplane Mode"
                    )
                    "Accessibility Commander se Airplane Mode toggle kar diya hai"
                } else {
                    openConnectivityPanel("airplane")
                    "Airplane Mode settings khol diya hai"
                }
            }

            key.contains("location") || key.contains("gps") -> {
                if (hasAcc) {
                    SuzyAccessibilityService.toggleQuickSettingsTile(
                        listOf("Location", "GPS"),
                        "Location"
                    )
                    "Accessibility Commander se Location toggle kar diya hai"
                } else {
                    openConnectivityPanel("location")
                    "Location settings khol diya hai"
                }
            }

            else -> {
                openConnectivityPanel("wifi")
                "Connectivity settings opened"
            }
        }
    }

    private fun openConnectivityPanel(kind: String) {
        try {
            val intent = when (kind) {
                "wifi" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_WIFI)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
                "data" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                } else {
                    Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                }
                "hotspot" -> Intent().apply {
                    component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
                }
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
            }
        }
    }

    fun performAccessibilityAction(action: String, textTarget: String = ""): String {
        if (!SuzyAccessibilityService.isConnected()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
            return "Suzy Accessibility Service OFF hai — Accessibility Settings khol diya hai, please ON karein"
        }

        val key = action.lowercase().trim()
        return when {
            key.contains("lock") -> {
                if (SuzyAccessibilityService.performLockScreen()) "Phone screen lock kar di gayi hai"
                else "Lock screen failed (Android 9+ required)"
            }
            key.contains("screenshot") -> {
                if (SuzyAccessibilityService.performTakeScreenshot()) "Screenshot le liya gaya hai"
                else "Screenshot failed"
            }
            key.contains("power") -> {
                SuzyAccessibilityService.performPowerDialog()
                "Power menu khol diya hai"
            }
            key.contains("scroll_down") || key.contains("down") -> {
                SuzyAccessibilityService.performScroll(down = true)
                "Screen neeche scroll kar di hai"
            }
            key.contains("scroll_up") || key.contains("up") -> {
                SuzyAccessibilityService.performScroll(down = false)
                "Screen upar scroll kar di hai"
            }
            key.contains("home") -> {
                SuzyAccessibilityService.performHome()
                "Home screen par aa gaye hain"
            }
            key.contains("back") -> {
                SuzyAccessibilityService.performBack()
                "Back button press kar diya hai"
            }
            key.contains("recent") -> {
                SuzyAccessibilityService.performRecents()
                "Recent apps khol diye hain"
            }
            key.contains("quick") -> {
                SuzyAccessibilityService.performOpenQuickSettings()
                "Quick Settings panel khol diya hai"
            }
            key.contains("click") && textTarget.isNotBlank() -> {
                val ok = SuzyAccessibilityService.clickNodeByText(textTarget)
                if (ok) "'$textTarget' par click kar diya hai" else "'$textTarget' button screen par nahi mila"
            }
            else -> "Accessibility action '$action' executed"
        }
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = context.registerReceiver(null, ifilter)
            val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 75
            val chargePlug = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = chargePlug == BatteryManager.BATTERY_STATUS_CHARGING ||
                chargePlug == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
            BatteryTelemetry(pct, charging, tempTenths / 10f)
        } catch (_: Exception) {
            BatteryTelemetry(75, false, 32.0f)
        }
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

    fun executeTool(name: String, args: JSONObject): String {
        val toolName = name.lowercase().trim()
        return when (toolName) {
            "control_flashlight", "toggle_flashlight" -> {
                val enable = if (args.has("enable")) args.optBoolean("enable", true) else !isTorchOn
                toggleFlashlight(enable)
            }
            "control_dnd_mode", "toggle_dnd" -> {
                val enable = args.optBoolean("enable", true)
                val level = args.optString("level", "PRIORITY")
                setDndMode(enable, level)
            }
            "control_connectivity" -> {
                val target = args.optString("target", "wifi")
                val state = args.optString("state", "toggle")
                controlConnectivity(target, state)
            }
            "control_volume_brightness" -> {
                val setting = args.optString("setting", "brightness").lowercase()
                val percent = args.optInt("percent", 60)
                if (setting.contains("bright")) {
                    setBrightnessPercent(percent)
                } else if (setting.contains("rotate")) {
                    setAutoRotate(percent > 0)
                } else {
                    setVolumePercent(setting, percent)
                }
            }
            "accessibility_action" -> {
                val action = args.optString("action", "lock_screen")
                val targetText = args.optString("target_text", "")
                performAccessibilityAction(action, targetText)
            }
            "get_battery_status" -> {
                val bat = getBatteryTelemetry()
                "Battery ${bat.percent}%, ${if (bat.isCharging) "Charging" else "Discharging"}, Temp ${bat.tempCelsius}°C"
            }
            "open_youtube_or_url" -> {
                val query = args.optString("query", "")
                val url = if (query.startsWith("http")) query
                else if (query.isNotBlank()) "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                else "https://www.youtube.com"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    "Opened $url"
                } catch (e: Exception) {
                    "Failed to open URL: ${e.message}"
                }
            }
            else -> "Executed $name"
        }
    }
}
