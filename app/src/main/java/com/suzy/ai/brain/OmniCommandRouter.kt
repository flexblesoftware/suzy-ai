package com.suzy.ai.brain

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.tools.SuzyTools
import java.util.Locale

data class CommandExecutionOutcome(
    val handled: Boolean,
    val summary: String,
    val navigateToRoute: String? = null,
    val updatedSettings: SuzySettings? = null,
    val addMemoryPair: Pair<String, String>? = null,
    val addTaskTitle: String? = null,
    val addFinanceEntry: Triple<String, String, Double>? = null,
    val addShoppingItem: String? = null,
    val saveParkingText: String? = null,
    val startTimerMinutes: Int? = null,
    val toggleTorch: Boolean? = null,
    val triggerBossBriefing: Boolean = false,
    val triggerStealthLock: Boolean = false,
    val securityBlockedReason: String? = null
)

object OmniCommandRouter {

    private val rudeKeywords = listOf(
        "pagal", "bewakoof", "idiot", "stupid", "shut up", "bakwas", "chup kar", "useless", "gadha", "hate you"
    )

    private val apologyKeywords = listOf(
        "sorry", "maaf", "galti ho gayi", "maan jao", "please maaf", "forgive"
    )

    fun processCommandChain(
        context: Context,
        rawInput: String,
        currentSettings: SuzySettings,
        isOwnerVerified: Boolean,
        tools: SuzyTools = SuzyTools(context)
    ): CommandExecutionOutcome {
        val text = rawInput.trim()
        if (text.isBlank()) return CommandExecutionOutcome(false, "")

        val segments = text.split(Regex("\\s+(?:aur|and|then)\\s+|,", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var workingSettings = currentSettings
        var settingsChanged = false
        var navRoute: String? = null
        var memPair: Pair<String, String>? = null
        var taskTitle: String? = null
        var financeTriple: Triple<String, String, Double>? = null
        var shoppingItem: String? = null
        var parkingSpot: String? = null
        var timerMins: Int? = null
        var torchState: Boolean? = null
        var bossBrief = false
        var stealthLock = false
        var blockedReason: String? = null
        val summaries = mutableListOf<String>()

        for (seg in segments) {
            val single = evaluateSingleSegment(
                context = context,
                segment = seg,
                settings = workingSettings,
                isOwnerVerified = isOwnerVerified,
                tools = tools
            )
            if (single.handled) {
                summaries.add(single.summary)
                single.updatedSettings?.let {
                    workingSettings = it
                    settingsChanged = true
                }
                single.navigateToRoute?.let { navRoute = it }
                single.addMemoryPair?.let { memPair = it }
                single.addTaskTitle?.let { taskTitle = it }
                single.addFinanceEntry?.let { financeTriple = it }
                single.addShoppingItem?.let { shoppingItem = it }
                single.saveParkingText?.let { parkingSpot = it }
                single.startTimerMinutes?.let { timerMins = it }
                single.toggleTorch?.let { torchState = it }
                if (single.triggerBossBriefing) bossBrief = true
                if (single.triggerStealthLock) stealthLock = true
                single.securityBlockedReason?.let { blockedReason = it }
            }
        }

        return CommandExecutionOutcome(
            handled = summaries.isNotEmpty(),
            summary = summaries.joinToString(" | "),
            navigateToRoute = navRoute,
            updatedSettings = if (settingsChanged) workingSettings else null,
            addMemoryPair = memPair,
            addTaskTitle = taskTitle,
            addFinanceEntry = financeTriple,
            addShoppingItem = shoppingItem,
            saveParkingText = parkingSpot,
            startTimerMinutes = timerMins,
            toggleTorch = torchState,
            triggerBossBriefing = bossBrief,
            triggerStealthLock = stealthLock,
            securityBlockedReason = blockedReason
        )
    }

    private fun evaluateSingleSegment(
        context: Context,
        segment: String,
        settings: SuzySettings,
        isOwnerVerified: Boolean,
        tools: SuzyTools
    ): CommandExecutionOutcome {
        val lower = segment.lowercase(Locale.US)

        fun checkOwnerPermission(actionName: String): CommandExecutionOutcome? {
            if (!settings.ownerModeStrict) return null
            val hasPassphrase = settings.ownerPassphrase.isNotBlank() &&
                lower.contains(settings.ownerPassphrase.lowercase(Locale.US))
            if (isOwnerVerified || hasPassphrase) return null
            return CommandExecutionOutcome(
                handled = true,
                summary = "Owner Lock: '$actionName' blocked (Passphrase/PIN required)",
                securityBlockedReason = "Strict Owner Mode blocked '$actionName'"
            )
        }

        // 1. Stealth Panic Lockdown
        if (lower.contains(settings.stealthCodeword.lowercase(Locale.US)) ||
            lower.contains("stealth mode") ||
            lower.contains("panic lock")
        ) {
            return CommandExecutionOutcome(
                handled = true,
                summary = "Stealth Panic Lock Activated",
                updatedSettings = settings.copy(uiThemeSkin = "amoled", layoutMode = "zen", captionsEnabled = false),
                triggerStealthLock = true
            )
        }

        // 2. Real Flashlight / Torch Control (Direct CameraManager)
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("flash light")) {
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("bujha")
            val turnOn = !turnOff
            val msg = tools.toggleFlashlight(turnOn)
            return CommandExecutionOutcome(true, msg, toggleTorch = turnOn)
        }

        // 3. Real Do Not Disturb (DND) Mode Control
        if (lower.contains("dnd") || lower.contains("do not disturb")) {
            checkOwnerPermission("DND Mode")?.let { return it }
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("hatao")
            val msg = tools.setDndMode(!turnOff)
            return CommandExecutionOutcome(true, msg)
        }

        // 4. Real Mobile Data, Hotspot, Wi-Fi, Bluetooth, Airplane, Location
        if (lower.contains("mobile data") || lower.contains("data on") || lower.contains("data off") || lower.contains("data band") || lower.contains("data chalu") || lower.contains("internet on") || lower.contains("internet off")) {
            checkOwnerPermission("Mobile Data")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("data"))
        }
        if (lower.contains("hotspot")) {
            checkOwnerPermission("Personal Hotspot")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("hotspot"))
        }
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            checkOwnerPermission("Wi-Fi")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("wifi"))
        }
        if (lower.contains("bluetooth")) {
            checkOwnerPermission("Bluetooth")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("bluetooth"))
        }
        if (lower.contains("airplane") || lower.contains("flight mode")) {
            checkOwnerPermission("Airplane Mode")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("airplane"))
        }
        if (lower.contains("location on") || lower.contains("gps on") || lower.contains("location off")) {
            checkOwnerPermission("Location")?.let { return it }
            return CommandExecutionOutcome(true, tools.controlConnectivity("location"))
        }

        // 5. Real Accessibility Hardware & Gesture Actions
        if (lower.contains("lock screen") || lower.contains("phone lock") || lower.contains("screen lock")) {
            checkOwnerPermission("Lock Screen")?.let { return it }
            return CommandExecutionOutcome(true, tools.performAccessibilityAction("lock_screen"))
        }
        if (lower.contains("screenshot")) {
            return CommandExecutionOutcome(true, tools.performAccessibilityAction("screenshot"))
        }
        if (lower.contains("power menu") || lower.contains("restart menu")) {
            checkOwnerPermission("Power Menu")?.let { return it }
            return CommandExecutionOutcome(true, tools.performAccessibilityAction("power_menu"))
        }
        if (lower.contains("scroll down") || lower.contains("neeche scroll")) {
            return CommandExecutionOutcome(true, tools.performAccessibilityAction("scroll_down"))
        }
        if (lower.contains("scroll up") || lower.contains("upar scroll")) {
            return CommandExecutionOutcome(true, tools.performAccessibilityAction("scroll_up"))
        }
        if (lower.startsWith("click ") || lower.contains("par click")) {
            val target = segment.replace(Regex("(?i)suzy|vikram|par click karo|par click|click"), "").trim()
            if (target.isNotBlank()) {
                return CommandExecutionOutcome(true, tools.performAccessibilityAction("click_text", target))
            }
        }

        // 6. Auto-Rotate, Brightness & Volume Control
        if (lower.contains("auto rotate") || lower.contains("rotation")) {
            val enable = !lower.contains("off") && !lower.contains("band")
            return CommandExecutionOutcome(true, tools.setAutoRotate(enable))
        }
        if (lower.contains("brightness")) {
            checkOwnerPermission("Modify System Brightness")?.let { return it }
            val num = Regex("\\d+").find(lower)?.value?.toIntOrNull()?.coerceIn(5, 100) ?: 60
            return CommandExecutionOutcome(true, tools.setBrightnessPercent(num))
        }
        if (lower.contains("volume")) {
            val num = Regex("\\d+").find(lower)?.value?.toIntOrNull()?.coerceIn(0, 100)
                ?: if (lower.contains("full") || lower.contains("max")) 100 else if (lower.contains("mute") || lower.contains("zero")) 0 else 70
            val stream = when {
                lower.contains("alarm") -> "alarm"
                lower.contains("ring") -> "ring"
                else -> "media"
            }
            return CommandExecutionOutcome(true, tools.setVolumePercent(stream, num))
        }
        if (lower.contains("phone silent") || lower.contains("silent mode")) {
            checkOwnerPermission("Ringer Mode")?.let { return it }
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (_: Exception) {}
            return CommandExecutionOutcome(true, "Phone Vibrate/Silent mode par set ho gaya")
        }

        // 7. Emotional Attitude / Anger & Apology Engine (145-150)
        if (apologyKeywords.any { lower.contains(it) } && settings.angerLevel > 0) {
            val nextAnger = (settings.angerLevel - 45).coerceAtLeast(0)
            val nextTrust = (settings.trustScore + 3).coerceAtMost(100)
            return CommandExecutionOutcome(
                handled = true,
                summary = "Narazgi kam hui ($nextAnger%)",
                updatedSettings = settings.copy(
                    angerLevel = nextAnger,
                    trustScore = nextTrust,
                    activeMoodId = if (nextAnger == 0) "romantic" else settings.activeMoodId
                )
            )
        }
        if (rudeKeywords.any { lower.contains(it) }) {
            val boost = (35 * settings.angerSensitivity).toInt().coerceAtLeast(20)
            val nextAnger = (settings.angerLevel + boost).coerceAtMost(100)
            val nextTrust = (settings.trustScore - 5).coerceAtLeast(0)
            return CommandExecutionOutcome(
                handled = true,
                summary = "Narazgi / Gussa Meter: $nextAnger%",
                updatedSettings = settings.copy(
                    angerLevel = nextAnger,
                    trustScore = nextTrust,
                    activeMoodId = "naraz"
                )
            )
        }

        // 8. Persona Switching (including South Hero Vikram + all 8 Female Personas)
        for (p in SuzyCatalog.personas) {
            val pName = p.name.lowercase(Locale.US)
            if ((lower.contains(pName) && (lower.contains("lagao") || lower.contains("switch") || lower.contains("persona") || lower.contains("ban") || lower.contains("hero"))) ||
                (p.id == "vikram" && (lower.contains("south hero") || lower.contains("male persona") || lower.contains("hero mode")))
            ) {
                return CommandExecutionOutcome(
                    handled = true,
                    summary = "Persona switched to ${p.name} (${p.voiceName})",
                    updatedSettings = settings.copy(
                        activePersonaName = p.name,
                        voiceName = p.voiceName,
                        systemPrompt = p.systemPrompt,
                        temperature = p.temperature
                    )
                )
            }
        }

        // 9. 3D ORB Style & Color Morphing (109-112)
        if (lower.contains("arc reactor")) {
            return CommandExecutionOutcome(true, "ORB morphed to Arc Reactor", updatedSettings = settings.copy(orbStyle = "arc_reactor"))
        }
        if (lower.contains("quantum")) {
            return CommandExecutionOutcome(true, "ORB morphed to Quantum Rings", updatedSettings = settings.copy(orbStyle = "quantum_rings"))
        }
        if (lower.contains("eclipse") || lower.contains("solar")) {
            return CommandExecutionOutcome(true, "ORB morphed to Solar Eclipse", updatedSettings = settings.copy(orbStyle = "solar_eclipse"))
        }
        if (lower.contains("dna") || lower.contains("helix")) {
            return CommandExecutionOutcome(true, "ORB morphed to Cyber Helix", updatedSettings = settings.copy(orbStyle = "cyber_dna"))
        }
        if (lower.contains("halo") || lower.contains("breathing")) {
            return CommandExecutionOutcome(true, "ORB morphed to Breathing Halo", updatedSettings = settings.copy(orbStyle = "breathing_halo"))
        }
        if (lower.contains("plasma")) {
            return CommandExecutionOutcome(true, "ORB morphed to Plasma Sphere", updatedSettings = settings.copy(orbStyle = "plasma"))
        }

        // 10. Live UI Design & Layout Morphing (121-125)
        if (lower.contains("zen mode")) {
            val enableZen = !lower.contains("band") && !lower.contains("off")
            return CommandExecutionOutcome(
                true,
                if (enableZen) "Zen ORB-Only UI Mode ON" else "Standard 5-Tab UI restored",
                updatedSettings = settings.copy(layoutMode = if (enableZen) "zen" else "standard")
            )
        }
        if (lower.contains("amoled") || lower.contains("pure black")) {
            return CommandExecutionOutcome(true, "Pure AMOLED Black theme applied", updatedSettings = settings.copy(uiThemeSkin = "amoled"))
        }
        if (lower.contains("cyberpunk")) {
            return CommandExecutionOutcome(true, "Cyberpunk Neon UI skin applied", updatedSettings = settings.copy(uiThemeSkin = "cyberpunk"))
        }

        // 11. Boss Briefing, Timer, Memory, Finance, Navigation
        if (lower.contains("boss briefing") || lower.contains("daily briefing") || lower.contains("status report")) {
            return CommandExecutionOutcome(true, "Running Owner Boss Briefing", triggerBossBriefing = true)
        }
        if (lower.contains("timer") || lower.contains("focus")) {
            val mins = Regex("\\d+").find(lower)?.value?.toIntOrNull()?.coerceIn(1, 180) ?: 25
            return CommandExecutionOutcome(true, "Started ${mins}m Focus Timer", startTimerMinutes = mins)
        }
        if (lower.contains("yaad rakh") || lower.contains("remember that") || lower.contains("save memory")) {
            val fact = segment.replace(Regex("(?i)suzy|vikram|yaad rakhna|yaad rakho|remember that|save memory"), "").trim()
            if (fact.isNotBlank()) {
                return CommandExecutionOutcome(true, "Saved to Memory Vault: $fact", addMemoryPair = "Auto-Learned" to fact)
            }
        }
        if (lower.contains("udhaar") || lower.contains("rupaye") || lower.contains("kharcha")) {
            val amt = Regex("\\d+").find(lower)?.value?.toDoubleOrNull() ?: 0.0
            if (amt > 0.0) {
                val kind = if (lower.contains("udhaar")) "UDHAAR_LENA" else "EXPENSE"
                return CommandExecutionOutcome(true, "Logged ₹${amt.toInt()} in $kind", addFinanceEntry = Triple(kind, segment, amt))
            }
        }
        if (lower.contains("youtube")) {
            val query = segment.replace(Regex("(?i)suzy|vikram|youtube par|youtube|chalao|play|open|kholo"), "").trim()
            val intent = if (query.isBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}"))
            }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            try { context.startActivity(intent) } catch (_: Exception) {}
            return CommandExecutionOutcome(true, if (query.isBlank()) "Opened YouTube" else "YouTube search: $query")
        }

        // 12. Voice Screen Navigator (including Permission Manager & Accessibility Studio)
        val routeMap = listOf(
            "permission" to "settings_permissions",
            "accessibility" to "settings_accessibility",
            "chat" to "tab_chat",
            "persona" to "tab_personas",
            "memory" to "tab_memory",
            "hub" to "tab_hub",
            "owner" to "mod_owner",
            "voice id" to "mod_owner",
            "orb studio" to "mod_orb_ui",
            "finance" to "mod_finance",
            "udhaar" to "mod_finance",
            "planner" to "mod_planner",
            "vision" to "mod_vision",
            "radar" to "mod_tactical",
            "attitude" to "mod_attitude",
            "sanctuary" to "mod_sanctuary",
            "settings" to "settings_hub"
        )
        if (lower.contains("kholo") || lower.contains("open") || lower.contains("dikhao") || lower.contains("screen")) {
            for ((key, route) in routeMap) {
                if (lower.contains(key)) {
                    return CommandExecutionOutcome(true, "Navigated to $key", navigateToRoute = route)
                }
            }
        }

        return CommandExecutionOutcome(false, "")
    }
}
