package com.suzy.ai.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SuzyNotificationListenerService : NotificationListenerService() {

    private var lastSignature: String = ""
    private var lastTimestampMs: Long = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val item = sbn ?: return
        val pkg = item.packageName ?: return

        // Ignore our own notifications and system/android internal alerts
        if (pkg == applicationContext.packageName || pkg == "android" || pkg == "com.android.systemui") {
            return
        }

        val notif = item.notification ?: return
        val flags = notif.flags

        // Ignore ongoing background services, group summaries, and progress bars
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (notif.category == Notification.CATEGORY_SERVICE ||
            notif.category == Notification.CATEGORY_PROGRESS ||
            notif.category == Notification.CATEGORY_TRANSPORT
        ) {
            return
        }

        val extras = notif.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val body = bigText.ifBlank { normalText }

        if (title.isBlank() && body.isBlank()) return

        // Deduplicate rapid duplicate posts from messaging apps
        val signature = "$pkg|$title|$body"
        val now = System.currentTimeMillis()
        if (signature == lastSignature && (now - lastTimestampMs) < 4500L) {
            return
        }
        lastSignature = signature
        lastTimestampMs = now

        val appName = resolveAppName(pkg)
        CallAnnouncerService.onNotificationArrived(
            context = applicationContext,
            appName = appName,
            senderTitle = title,
            messageBody = body
        )
    }

    private fun resolveAppName(pkg: String): String {
        return try {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() } ?: pkg
        } catch (_: Exception) {
            when {
                pkg.contains("whatsapp") -> "WhatsApp"
                pkg.contains("instagram") -> "Instagram"
                pkg.contains("telegram") -> "Telegram"
                pkg.contains("messaging") || pkg.contains("mms") -> "Messages"
                pkg.contains("gm") -> "Gmail"
                else -> "App"
            }
        }
    }
}
