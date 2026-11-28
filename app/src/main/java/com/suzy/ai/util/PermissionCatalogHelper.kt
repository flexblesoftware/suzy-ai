package com.suzy.ai.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.suzy.ai.service.SuzyAccessibilityService

data class PermissionAuditEntry(
    val id: String,
    val group: String,
    val title: String,
    val roleDescription: String,
    val isGranted: Boolean,
    val isRuntime: Boolean,
    val runtimePermission: String? = null
)

object PermissionCatalogHelper {

    fun auditAllPermissions(context: Context): List<PermissionAuditEntry> {
        val list = mutableListOf<PermissionAuditEntry>()

        fun hasRuntime(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        // Group 1: Core Voice, Camera & Phone
        list.add(
            PermissionAuditEntry(
                id = "mic",
                group = "Core Voice & Vision",
                title = "Microphone (RECORD_AUDIO)",
                roleDescription = "Live Gemini call, Wake Word & Owner Voiceprint DSP",
                isGranted = hasRuntime(Manifest.permission.RECORD_AUDIO),
                isRuntime = true,
                runtimePermission = Manifest.permission.RECORD_AUDIO
            )
        )
        list.add(
            PermissionAuditEntry(
                id = "camera",
                group = "Core Voice & Vision",
                title = "Camera & Torch (CAMERA)",
                roleDescription = "Real Flashlight ON/OFF, SOS Strobe & Vision Eye",
                isGranted = hasRuntime(Manifest.permission.CAMERA),
                isRuntime = true,
                runtimePermission = Manifest.permission.CAMERA
            )
        )
        list.add(
            PermissionAuditEntry(
                id = "phone_state",
                group = "Core Voice & Vision",
                title = "Phone Call State (READ_PHONE_STATE)",
                roleDescription = "Incoming Call Announcer & Call Privacy Mic Shield",
                isGranted = hasRuntime(Manifest.permission.READ_PHONE_STATE),
                isRuntime = true,
                runtimePermission = Manifest.permission.READ_PHONE_STATE
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(
                PermissionAuditEntry(
                    id = "answer_calls",
                    group = "Core Voice & Vision",
                    title = "Hands-Free Call Control (ANSWER_PHONE_CALLS)",
                    roleDescription = "Voice command se incoming call pick/reject karna",
                    isGranted = hasRuntime(Manifest.permission.ANSWER_PHONE_CALLS),
                    isRuntime = true,
                    runtimePermission = Manifest.permission.ANSWER_PHONE_CALLS
                )
            )
        }
        list.add(
            PermissionAuditEntry(
                id = "contacts",
                group = "Core Voice & Vision",
                title = "Contacts Access (READ_CONTACTS)",
                roleDescription = "Caller naam bolna, VIP Dialer & Birthday Radar",
                isGranted = hasRuntime(Manifest.permission.READ_CONTACTS),
                isRuntime = true,
                runtimePermission = Manifest.permission.READ_CONTACTS
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionAuditEntry(
                    id = "notifications",
                    group = "Core Voice & Vision",
                    title = "Post Notifications (POST_NOTIFICATIONS)",
                    roleDescription = "Background Guard, Timer Alerts & Live Call HUD",
                    isGranted = hasRuntime(Manifest.permission.POST_NOTIFICATIONS),
                    isRuntime = true,
                    runtimePermission = Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(
                PermissionAuditEntry(
                    id = "bluetooth",
                    group = "Core Voice & Vision",
                    title = "Bluetooth Connect (BLUETOOTH_CONNECT)",
                    roleDescription = "Earbuds auto-companion & audio routing",
                    isGranted = hasRuntime(Manifest.permission.BLUETOOTH_CONNECT),
                    isRuntime = true,
                    runtimePermission = Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
        list.add(
            PermissionAuditEntry(
                id = "location",
                group = "Core Voice & Vision",
                title = "Precise Location (ACCESS_FINE_LOCATION)",
                roleDescription = "Weather radar, Parking spot & SOS location link",
                isGranted = hasRuntime(Manifest.permission.ACCESS_FINE_LOCATION),
                isRuntime = true,
                runtimePermission = Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        // Group 2: Deep Hardware & System Special Permissions
        val canWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
        list.add(
            PermissionAuditEntry(
                id = "write_settings",
                group = "Deep Hardware & OS Control",
                title = "Modify System Settings (WRITE_SETTINGS)",
                roleDescription = "Real System Brightness %, Auto-Rotate & Screen Timeout",
                isGranted = canWriteSettings,
                isRuntime = false
            )
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val hasDndPolicy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm?.isNotificationPolicyAccessGranted == true
        } else true
        list.add(
            PermissionAuditEntry(
                id = "dnd_policy",
                group = "Deep Hardware & OS Control",
                title = "Do Not Disturb Access (NOTIFICATION_POLICY)",
                roleDescription = "Voice command se real DND Mode & Silent Mode ON/OFF",
                isGranted = hasDndPolicy,
                isRuntime = false
            )
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am?.canScheduleExactAlarms() == true
        } else true
        list.add(
            PermissionAuditEntry(
                id = "exact_alarm",
                group = "Deep Hardware & OS Control",
                title = "Exact Alarms & Timers (SCHEDULE_EXACT_ALARM)",
                roleDescription = "Exact second par bajne wale voice alarms & reminders",
                isGranted = hasExactAlarm,
                isRuntime = false
            )
        )

        // Group 3: Background Survival, Overlay & Accessibility Service
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
        list.add(
            PermissionAuditEntry(
                id = "battery_opt",
                group = "Background Survival & Accessibility",
                title = "Unrestricted Background Battery",
                roleDescription = "Screen off hone par bhi 24x7 Wake Word & Call Guard",
                isGranted = ignoringBattery,
                isRuntime = false
            )
        )

        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        list.add(
            PermissionAuditEntry(
                id = "overlay",
                group = "Background Survival & Accessibility",
                title = "Display Over Other Apps (SYSTEM_ALERT_WINDOW)",
                roleDescription = "Floating Mini-ORB & Lock-Screen HUD",
                isGranted = canOverlay,
                isRuntime = false
            )
        )

        val notifListenerEnabled = isNotificationListenerEnabled(context)
        list.add(
            PermissionAuditEntry(
                id = "notif_listener",
                group = "Background Survival & Accessibility",
                title = "Notification Listener Service",
                roleDescription = "WhatsApp/Telegram message reader (OTP safe-blocked)",
                isGranted = notifListenerEnabled,
                isRuntime = false
            )
        )

        list.add(
            PermissionAuditEntry(
                id = "accessibility",
                group = "Background Survival & Accessibility",
                title = "Suzy Accessibility Commander Service",
                roleDescription = "Mobile Data, Hotspot, Wi-Fi toggle, Lock Screen, Screenshot & Scroll",
                isGranted = SuzyAccessibilityService.isConnected(),
                isRuntime = false
            )
        )

        return list
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.contains(context.packageName)
    }

    fun openSpecialPermissionScreen(context: Context, id: String) {
        val intent = when (id) {
            "write_settings" -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            "dnd_policy" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            "exact_alarm" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
            "battery_opt" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            "notif_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            } catch (_: Exception) {
            }
        }
    }
}
