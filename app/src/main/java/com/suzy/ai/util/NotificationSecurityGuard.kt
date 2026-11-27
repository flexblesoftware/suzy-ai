package com.suzy.ai.util

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import java.util.Locale

sealed class NotificationEvalResult {
    data object Ignored : NotificationEvalResult()
    data class BlockedSensitive(val reason: String) : NotificationEvalResult()
    data class SafeToAnnounce(
        val displayTitle: String,
        val displaySummary: String,
        val geminiPrompt: String
    ) : NotificationEvalResult()
}

object NotificationSecurityGuard {

    private val trustedMessagingPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.instagram.android",
        "org.telegram.messenger",
        "org.telegram.plus",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.google.android.gm"
    )

    private val sensitiveAppKeywords = listOf(
        "paisa", "phonepe", "paytm", "upi", "bhim", "sbi", "hdfc", "icici",
        "axis", "kotak", "bank", "cred", "zerodha", "groww", "upstox",
        "authenticator", "authy", "wallet", "digilocker"
    )

    private val sensitiveTextKeywords = listOf(
        "otp", "one time password", "one-time password", "verification code",
        "security code", "2fa", "cvv", "mpin", "upi pin", "atm pin",
        "password", "passcode", "secret code", "aadhaar", "pan card",
        "debited", "credited", "a/c", "acct", "available balance", "ifsc",
        "txn id", "transaction id", "do not share"
    )

    private val otpCodeRegex = Regex("\\b\\d{4,8}\\b")

    fun evaluate(
        context: Context,
        personaName: String,
        packageName: String,
        appName: String,
        visibility: Int,
        senderTitle: String,
        messageBody: String,
        readContentEnabled: Boolean,
        blockSensitive: Boolean,
        hideWhenLocked: Boolean,
        trustedAppsOnly: Boolean
    ): NotificationEvalResult {
        val pkgLower = packageName.lowercase(Locale.US)

        // 1. Respect Android OS Secret Notification visibility
        if (visibility == Notification.VISIBILITY_SECRET) {
            return NotificationEvalResult.BlockedSensitive("Secret visibility notification skipped")
        }

        // 2. Trusted Apps Only filter
        if (trustedAppsOnly && !trustedMessagingPackages.contains(pkgLower)) {
            return NotificationEvalResult.Ignored
        }

        val combinedLower = "${senderTitle.lowercase(Locale.US)} ${messageBody.lowercase(Locale.US)}"

        // 3. OTP, Banking & Financial Security Filter (NEVER leaks sensitive data to cloud)
        if (blockSensitive) {
            val isFinancialApp = sensitiveAppKeywords.any { pkgLower.contains(it) }
            val hasSensitiveKeyword = sensitiveTextKeywords.any { combinedLower.contains(it) }
            val hasOtpWithDigits = otpCodeRegex.containsMatchIn(combinedLower) &&
                (combinedLower.contains("code") || combinedLower.contains("otp") || combinedLower.contains("pin") || combinedLower.contains("bank"))

            if (isFinancialApp || hasSensitiveKeyword || hasOtpWithDigits) {
                return NotificationEvalResult.BlockedSensitive("OTP / Banking alert protected by Security Shield")
            }
        }

        // 4. Lock-Screen Privacy Guard: hide message body when phone screen is locked
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceLocked = keyguard?.isKeyguardLocked == true
        val allowReadingBody = readContentEnabled && !(hideWhenLocked && isDeviceLocked)

        val safeSender = senderTitle.replace("\"", "'").take(50).trim()
        val safeBody = messageBody.replace("\"", "'").take(140).trim()

        val displayTitle = if (safeSender.isNotBlank()) "$appName • $safeSender" else appName
        val displaySummary = when {
            allowReadingBody && safeBody.isNotBlank() -> safeBody
            hideWhenLocked && isDeviceLocked && readContentEnabled -> "Protected on lock screen • Sender only"
            safeSender.isNotBlank() -> "New message from $safeSender"
            else -> "New notification on $appName"
        }

        val prompt = if (allowReadingBody && safeBody.isNotBlank()) {
            val fromPart = if (safeSender.isNotBlank()) " '$safeSender' ka" else ""
            "Tum $personaName ho. Abhi user ke phone par $appName par$fromPart notification aaya hai aur message likha hai: \"$safeBody\". Apne natural Hinglish persona style mein user ko pyar se batao ki $appName par kiska notification aaya hai aur yeh message padh kar sunao. Jawab chhota aur clear (1-2 lines) rakho."
        } else {
            val fromPart = if (safeSender.isNotBlank()) " '$safeSender' se" else ""
            "Tum $personaName ho. Abhi user ke phone par $appName par$fromPart naya notification aaya hai. Apne natural Hinglish persona style mein 1 chhoti line mein user ko batao ki $appName par$fromPart notification aaya hai (message text mat padhna)."
        }

        return NotificationEvalResult.SafeToAnnounce(
            displayTitle = displayTitle,
            displaySummary = displaySummary,
            geminiPrompt = prompt
        )
    }
}
