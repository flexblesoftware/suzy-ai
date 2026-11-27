package com.suzy.ai.brain

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
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
        isOwnerVerified: Boolean
    ): CommandExecutionOutcome {
        val text = rawInput.trim()
        if (text.isBlank()) return CommandExecutionOutcome(false, "")

        // Split chained commands on " aur ", " and ", " then ", or ","
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
                isOwnerVerified = isOwnerVerified
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
        isOwnerVerified: Boolean
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

        // 2. Emotional Attitude / Anger & Apology Engine (145-150)
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

        // 3. Persona Switching (including South Hero Vikram + all 8 Female Personas)
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

        // 4. 3D ORB Style & Color Morphing (109-112)
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
        if (lower.contains("orb") && lower.contains("gold")) {
            return CommandExecutionOutcome(true, "ORB color set to Hero Gold", updatedSettings = settings.copy(orbPrimaryHex = 0xFFF59E0BL, orbSecondaryHex = 0xFFEC4899L))
        }
        if (lower.contains("orb") && lower.contains("cyan")) {
            return CommandExecutionOutcome(true, "ORB color set to Matrix Cyan", updatedSettings = settings.copy(orbPrimaryHex = 0xFF22D3EEL, orbSecondaryHex = 0xFFA855F7L))
        }

        // 5. Live UI Design & Layout Morphing (121-125)
        if (lower.contains("zen mode")) {
            val enableZen = !lower.contains("band") && !lower.contains("off")
            return CommandExecutionOutcome(
                true,
                if (enableZen) "Zen ORB-Only UI Mode ON" else "Standard 5-Tab UI restored",
                updatedSettings = settings.copy(layoutMode = if (enableZen) "zen" else "standard")
            )
        }
        if (lower.contains("standard ui") || lower.contains("normal ui")) {
            return CommandExecutionOutcome(true, "Standard UI restored", updatedSettings = settings.copy(layoutMode = "standard"))
        }
        if (lower.contains("amoled") || lower.contains("pure black")) {
            return CommandExecutionOutcome(true, "Pure AMOLED Black theme applied", updatedSettings = settings.copy(uiThemeSkin = "amoled"))
        }
        if (lower.contains("emerald") || lower.contains("matrix theme")) {
            return CommandExecutionOutcome(true, "Emerald Matrix UI skin applied", updatedSettings = settings.copy(uiThemeSkin = "emerald"))
        }
        if (lower.contains("rose gold")) {
            return CommandExecutionOutcome(true, "Royal Rose Gold UI skin applied", updatedSettings = settings.copy(uiThemeSkin = "rose_gold"))
        }
        if (lower.contains("cyberpunk")) {
            return CommandExecutionOutcome(true, "Cyberpunk Neon UI skin applied", updatedSettings = settings.copy(uiThemeSkin = "cyberpunk"))
        }
        if (lower.contains("sharp corner") || lower.contains("tactical corner")) {
            return CommandExecutionOutcome(true, "Sharp Tactical 8dp corners applied", updatedSettings = settings.copy(cornerRadiusDp = 8))
        }
        if (lower.contains("round corner") || lower.contains("soft corner")) {
            return CommandExecutionOutcome(true, "Soft Pill 24dp corners applied", updatedSettings = settings.copy(cornerRadiusDp = 24))
        }

        // 6. System Hardware & Android Settings Modification (20, 26, 27)
        if (lower.contains("brightness")) {
            checkOwnerPermission("Modify System Brightness")?.let { return it }
            val num = Regex("\\d+").find(lower)?.value?.toIntOrNull()?.coerceIn(5, 100) ?: 60
            applySystemBrightness(context, num)
            return CommandExecutionOutcome(true, "System Brightness set to $num%")
        }
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            return CommandExecutionOutcome(true, if (turnOn) "Flashlight ON" else "Flashlight OFF", toggleTorch = turnOn)
        }
        if (lower.contains("bluetooth")) {
            checkOwnerPermission("Open Bluetooth Settings")?.let { return it }
            openSystemAction(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            return CommandExecutionOutcome(true, "Bluetooth panel opened")
        }
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            checkOwnerPermission("Open Wi-Fi Settings")?.let { return it }
            openSystemAction(context, Settings.ACTION_WIFI_SETTINGS)
            return CommandExecutionOutcome(true, "Wi-Fi settings opened")
        }
        if (lower.contains("volume full") || lower.contains("awaaz badhao")) {
            setMediaVolumePercent(context, 100)
            return CommandExecutionOutcome(true, "Media Volume set to 100%")
        }
        if (lower.contains("phone silent") || lower.contains("silent mode")) {
            checkOwnerPermission("Ringer Mode")?.let { return it }
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (_: Exception) {}
            return CommandExecutionOutcome(true, "Phone set to Vibrate/Silent")
        }

        // 7. Boss Briefing (30)
        if (lower.contains("boss briefing") || lower.contains("daily briefing") || lower.contains("status report")) {
            return CommandExecutionOutcome(true, "Running Owner Boss Briefing", triggerBossBriefing = true)
        }

        // 8. Timer & Focus Pomodoro (13)
        if (lower.contains("timer") || lower.contains("focus")) {
            val mins = Regex("\\d+").find(lower)?.value?.toIntOrNull()?.coerceIn(1, 180) ?: 25
            return CommandExecutionOutcome(true, "Started ${mins}m Focus Timer", startTimerMinutes = mins)
        }

        // 9. Autonomous Memory Save (33)
        if (lower.contains("yaad rakh") || lower.contains("remember that") || lower.contains("save memory")) {
            val fact = segment.replace(Regex("(?i)suzy|vikram|yaad rakhna|yaad rakho|remember that|save memory"), "").trim()
            if (fact.isNotBlank()) {
                return CommandExecutionOutcome(true, "Saved to Memory Vault: $fact", addMemoryPair = "Auto-Learned" to fact)
            }
        }

        // 10. Planner Task, Expense / Udhaar, Shopping, Parking (4, 50, 84, 91, 94)
        if (lower.contains("task add") || lower.contains("planner mein")) {
            val task = segment.replace(Regex("(?i)suzy|vikram|task add karo|task add|planner mein"), "").trim()
            if (task.isNotBlank()) {
                return CommandExecutionOutcome(true, "Added task: $task", addTaskTitle = task)
            }
        }
        if (lower.contains("udhaar") || lower.contains("rupaye") || lower.contains("kharcha")) {
            val amt = Regex("\\d+").find(lower)?.value?.toDoubleOrNull() ?: 0.0
            if (amt > 0.0) {
                val kind = if (lower.contains("udhaar")) "UDHAAR_LENA" else "EXPENSE"
                return CommandExecutionOutcome(true, "Logged ₹${amt.toInt()} in $kind", addFinanceEntry = Triple(kind, segment, amt))
            }
        }
        if (lower.contains("shopping list")) {
            val item = segment.replace(Regex("(?i)suzy|vikram|shopping list mein|add karo|add"), "").trim()
            if (item.isNotBlank()) {
                return CommandExecutionOutcome(true, "Added to Shopping List: $item", addShoppingItem = item)
            }
        }
        if (lower.contains("parking")) {
            return CommandExecutionOutcome(true, "Parking spot saved", saveParkingText = segment)
        }

        // 11. Maps Navigation & YouTube Launcher (22, 99, 103)
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
        if (lower.contains("navigate") || lower.contains("rasta")) {
            val dest = segment.replace(Regex("(?i)suzy|vikram|navigate to|navigate|ka rasta|rasta dikhao|chalo"), "").trim().ifBlank { "Home" }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(dest)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
            return CommandExecutionOutcome(true, "Navigation started to $dest")
        }

        // 12. Voice Screen Navigator (32) — Jump to any dedicated screen by voice
        val routeMap = listOf(
            "chat" to "tab_chat",
            "persona" to "tab_personas",
            "memory" to "tab_memory",
            "hub" to "tab_hub",
            "owner" to "mod_owner",
            "voice id" to "mod_owner",
            "orb studio" to "mod_orb_ui",
            "ui design" to "mod_orb_ui",
            "theme" to "mod_orb_ui",
            "finance" to "mod_finance",
            "udhaar" to "mod_finance",
            "hisab" to "mod_finance",
            "planner" to "mod_planner",
            "alarm" to "mod_planner",
            "habit" to "mod_planner",
            "vision" to "mod_vision",
            "camera" to "mod_vision",
            "radar" to "mod_tactical",
            "compass" to "mod_tactical",
            "wifi" to "mod_tactical",
            "github" to "mod_tactical",
            "attitude" to "mod_attitude",
            "gussa" to "mod_attitude",
            "sanctuary" to "mod_sanctuary",
            "sleep" to "mod_sanctuary",
            "audio lab" to "mod_audio_lab",
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

    private fun applySystemBrightness(context: Context, percent: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                val value = ((percent / 100f) * 255f).toInt().coerceIn(10, 255)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }

    private fun setMediaVolumePercent(context: Context, percent: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = ((percent / 100f) * max).toInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
        }
    }

    private fun openSystemAction(context: Context, action: String) {
        try {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
