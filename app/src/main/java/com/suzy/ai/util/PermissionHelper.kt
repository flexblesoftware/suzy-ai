package com.suzy.ai.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class AppPermissionsStatus(
    val micGranted: Boolean = false,
    val phoneStateGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryUnrestricted: Boolean = false
) {
    val grantedCount: Int
        get() = listOf(
            micGranted,
            phoneStateGranted,
            contactsGranted,
            notificationsGranted,
            batteryUnrestricted
        ).count { it }

    val allGranted: Boolean
        get() = grantedCount == 5
}

object PermissionHelper {

    fun checkAll(context: Context): AppPermissionsStatus {
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val phone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val contacts = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val battery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        return AppPermissionsStatus(
            micGranted = mic,
            phoneStateGranted = phone,
            contactsGranted = contacts,
            notificationsGranted = notif,
            batteryUnrestricted = battery
        )
    }

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
