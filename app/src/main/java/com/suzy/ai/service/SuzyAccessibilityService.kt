package com.suzy.ai.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SuzyAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: SuzyAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        @Volatile
        var bankingSafetyGuardEnabled: Boolean = true

        @Volatile
        var lastExecutedAccessibilityAction: String = "Idle"

        private val protectedFinancialPackages = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",                        // PhonePe
            "net.one97.paytm",                        // Paytm
            "in.org.npci.upiapp",                     // BHIM UPI
            "com.sbi.lotusintouch",                   // YONO SBI
            "com.csam.icici.bank.imobile",            // iMobile Pay
            "com.snapwork.hdfc",                      // HDFC MobileBanking
            "com.axis.mobile"                         // Axis Mobile
        )

        fun isConnected(): Boolean = instance != null

        fun performLockScreen(): Boolean {
            val svc = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lastExecutedAccessibilityAction = "Lock Screen"
                return svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            return false
        }

        fun performTakeScreenshot(): Boolean {
            val svc = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lastExecutedAccessibilityAction = "Take Screenshot"
                return svc.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
            return false
        }

        fun performPowerDialog(): Boolean {
            val svc = instance ?: return false
            lastExecutedAccessibilityAction = "Open Power Menu"
            return svc.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }

        fun performHome(): Boolean {
            val svc = instance ?: return false
            lastExecutedAccessibilityAction = "Go Home"
            return svc.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun performBack(): Boolean {
            val svc = instance ?: return false
            lastExecutedAccessibilityAction = "Go Back"
            return svc.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun performRecents(): Boolean {
            val svc = instance ?: return false
            lastExecutedAccessibilityAction = "Open Recents"
            return svc.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun performOpenQuickSettings(): Boolean {
            val svc = instance ?: return false
            lastExecutedAccessibilityAction = "Open Quick Settings"
            return svc.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }

        fun performScroll(down: Boolean): Boolean {
            val svc = instance ?: return false
            if (svc.isProtectedBankingAppActive()) return false
            val dm = svc.resources.displayMetrics
            val cx = dm.widthPixels / 2f
            val startY = if (down) dm.heightPixels * 0.72f else dm.heightPixels * 0.30f
            val endY = if (down) dm.heightPixels * 0.28f else dm.heightPixels * 0.75f
            val path = Path().apply {
                moveTo(cx, startY)
                lineTo(cx, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 260L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            lastExecutedAccessibilityAction = if (down) "Scroll Down" else "Scroll Up"
            return svc.dispatchGesture(gesture, null, null)
        }

        fun clickNodeByText(target: String): Boolean {
            val svc = instance ?: return false
            if (svc.isProtectedBankingAppActive()) return false
            val root = svc.rootInActiveWindow ?: return false
            val clicked = svc.findAndClickRecursive(root, target.trim())
            if (clicked) {
                lastExecutedAccessibilityAction = "Clicked '$target'"
            }
            return clicked
        }

        /**
         * Opens Android Quick Settings Shade, searches for the target tile
         * (e.g. "Mobile data", "Hotspot", "Wi-Fi", "Bluetooth", "Airplane", "Location"),
         * clicks it, and collapses the shade.
         */
        fun toggleQuickSettingsTile(keywords: List<String>, labelForLog: String): Boolean {
            val svc = instance ?: return false
            val handler = Handler(Looper.getMainLooper())
            svc.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

            // Attempt 1 after 450ms (first QS page)
            handler.postDelayed({
                val root = svc.rootInActiveWindow
                var tapped = false
                if (root != null) {
                    for (kw in keywords) {
                        if (svc.findAndClickRecursive(root, kw)) {
                            tapped = true
                            break
                        }
                    }
                }
                if (tapped) {
                    lastExecutedAccessibilityAction = "Toggled $labelForLog"
                    handler.postDelayed({
                        svc.performGlobalAction(GLOBAL_ACTION_BACK)
                        handler.postDelayed({ svc.performGlobalAction(GLOBAL_ACTION_BACK) }, 220L)
                    }, 350L)
                } else {
                    // Swipe QS left to check page 2 of tiles
                    val dm = svc.resources.displayMetrics
                    val p = Path().apply {
                        moveTo(dm.widthPixels * 0.85f, dm.heightPixels * 0.30f)
                        lineTo(dm.widthPixels * 0.15f, dm.heightPixels * 0.30f)
                    }
                    val g = GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(p, 0L, 220L))
                        .build()
                    svc.dispatchGesture(g, null, null)

                    handler.postDelayed({
                        val root2 = svc.rootInActiveWindow
                        if (root2 != null) {
                            for (kw in keywords) {
                                if (svc.findAndClickRecursive(root2, kw)) {
                                    lastExecutedAccessibilityAction = "Toggled $labelForLog (Page 2)"
                                    break
                                }
                            }
                        }
                        handler.postDelayed({
                            svc.performGlobalAction(GLOBAL_ACTION_BACK)
                            handler.postDelayed({ svc.performGlobalAction(GLOBAL_ACTION_BACK) }, 220L)
                        }, 350L)
                    }, 420L)
                }
            }, 460L)

            return true
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Lightweight event hook; active queries happen on demand
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        instance = null
        _isServiceConnected.value = false
        super.onDestroy()
    }

    private fun isProtectedBankingAppActive(): Boolean {
        if (!bankingSafetyGuardEnabled) return false
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        return protectedFinancialPackages.contains(pkg)
    }

    private fun findAndClickRecursive(node: AccessibilityNodeInfo?, keyword: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()

        if (text.contains(keyword, ignoreCase = true) || desc.contains(keyword, ignoreCase = true)) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            var depth = 0
            while (parent != null && depth < 5) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
                depth++
            }
        }

        for (i in 0 until node.childCount) {
            if (findAndClickRecursive(node.getChild(i), keyword)) {
                return true
            }
        }
        return false
    }
}
