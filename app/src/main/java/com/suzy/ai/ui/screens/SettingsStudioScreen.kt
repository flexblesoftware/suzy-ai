package com.suzy.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.BuildConfig
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassBackground
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import com.suzy.ai.util.PermissionHelper
import java.util.Locale

private data class StudioCategory(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun SettingsScreen(
    current: SuzySettings,
    onSave: (SuzySettings) -> Unit,
    onBack: () -> Unit,
    initialStudioSection: Int = 0
) {
    val context = LocalContext.current
    var activeSection by remember { mutableIntStateOf(initialStudioSection) }

    // Draft state synchronized with current settings
    var apiKey by remember(current) { mutableStateOf(current.apiKey) }
    var backupApiKey by remember(current) { mutableStateOf(current.backupApiKey) }
    var modelId by remember(current) { mutableStateOf(current.modelId) }
    var showKey by remember { mutableStateOf(false) }

    var voiceName by remember(current) { mutableStateOf(current.voiceName) }
    var personaName by remember(current) { mutableStateOf(current.activePersonaName) }
    var customNote by remember(current) { mutableStateOf(current.customNote) }
    var temperature by remember(current) { mutableFloatStateOf(current.temperature) }
    var bargeIn by remember(current) { mutableStateOf(current.bargeInEnabled) }
    var captions by remember(current) { mutableStateOf(current.captionsEnabled) }
    var proximityEar by remember(current) { mutableStateOf(current.proximityEarRoutingEnabled) }
    var loudnessBoost by remember(current) { mutableIntStateOf(current.loudnessBoostDb) }

    var orbStyle by remember(current) { mutableStateOf(current.orbStyle) }
    var orbGlow by remember(current) { mutableFloatStateOf(current.orbGlowIntensity) }
    var orbSpin by remember(current) { mutableFloatStateOf(current.orbSpinSpeed) }
    var moodAutoOrb by remember(current) { mutableStateOf(current.moodAutoOrbSync) }
    var uiThemeSkin by remember(current) { mutableStateOf(current.uiThemeSkin) }
    var layoutMode by remember(current) { mutableStateOf(current.layoutMode) }
    var cornerRadius by remember(current) { mutableIntStateOf(current.cornerRadiusDp) }

    var wakeWord by remember(current) { mutableStateOf(current.wakeWordEnabled) }
    var customWakePhrase by remember(current) { mutableStateOf(current.customWakePhrase) }
    var shakeToCall by remember(current) { mutableStateOf(current.shakeToCallEnabled) }
    var flipToMute by remember(current) { mutableStateOf(current.flipToMuteEnabled) }

    var callAnnouncer by remember(current) { mutableStateOf(current.callAnnouncerEnabled) }
    var announceCallerName by remember(current) { mutableStateOf(current.announceCallerName) }
    var announcerStyle by remember(current) { mutableStateOf(current.announcerStyle) }
    var notifReader by remember(current) { mutableStateOf(current.notificationReaderEnabled) }
    var readNotifContent by remember(current) { mutableStateOf(current.readNotificationContent) }
    var blockSensitive by remember(current) { mutableStateOf(current.blockSensitiveNotifications) }
    var hideLockScreen by remember(current) { mutableStateOf(current.hideNotificationOnLockScreen) }

    var ownerName by remember(current) { mutableStateOf(current.ownerName) }
    var ownerNickname by remember(current) { mutableStateOf(current.ownerNickname) }
    var ownerPassphrase by remember(current) { mutableStateOf(current.ownerPassphrase) }
    var ownerPin by remember(current) { mutableStateOf(current.ownerPin) }
    var ownerStrict by remember(current) { mutableStateOf(current.ownerModeStrict) }
    var mediaShield by remember(current) { mutableStateOf(current.mediaShieldEnabled) }
    var callShield by remember(current) { mutableStateOf(current.callPrivacyShieldEnabled) }
    var stealthCodeword by remember(current) { mutableStateOf(current.stealthCodeword) }

    var chargerReaction by remember(current) { mutableStateOf(current.chargerReactionEnabled) }
    var lowBatThreshold by remember(current) { mutableIntStateOf(current.lowBatteryThreshold) }
    var thermalAlert by remember(current) { mutableStateOf(current.thermalAlertEnabled) }
    var btAutoMode by remember(current) { mutableStateOf(current.bluetoothAutoMode) }

    fun commitAll() {
        val cleanName = personaName.ifBlank { "Suzy" }
        onSave(
            current.copy(
                apiKey = apiKey,
                backupApiKey = backupApiKey,
                modelId = modelId,
                voiceName = voiceName,
                activePersonaName = cleanName,
                customNote = customNote,
                systemPrompt = SuzyCatalog.resolveHardcodedPrompt(cleanName, current.systemPrompt),
                temperature = temperature,
                bargeInEnabled = bargeIn,
                captionsEnabled = captions,
                proximityEarRoutingEnabled = proximityEar,
                loudnessBoostDb = loudnessBoost,
                orbStyle = orbStyle,
                orbGlowIntensity = orbGlow,
                orbSpinSpeed = orbSpin,
                moodAutoOrbSync = moodAutoOrb,
                uiThemeSkin = uiThemeSkin,
                layoutMode = layoutMode,
                cornerRadiusDp = cornerRadius,
                wakeWordEnabled = wakeWord,
                customWakePhrase = customWakePhrase,
                shakeToCallEnabled = shakeToCall,
                flipToMuteEnabled = flipToMute,
                callAnnouncerEnabled = callAnnouncer,
                announceCallerName = announceCallerName,
                announcerStyle = announcerStyle,
                notificationReaderEnabled = notifReader,
                readNotificationContent = readNotifContent,
                blockSensitiveNotifications = blockSensitive,
                hideNotificationOnLockScreen = hideLockScreen,
                ownerName = ownerName,
                ownerNickname = ownerNickname,
                ownerPassphrase = ownerPassphrase,
                ownerPin = ownerPin,
                ownerModeStrict = ownerStrict,
                mediaShieldEnabled = mediaShield,
                callPrivacyShieldEnabled = callShield,
                stealthCodeword = stealthCodeword,
                chargerReactionEnabled = chargerReaction,
                lowBatteryThreshold = lowBatThreshold,
                thermalAlertEnabled = thermalAlert,
                bluetoothAutoMode = btAutoMode
            )
        )
    }

    val studios = listOf(
        StudioCategory(1, "1. Gemini Engine & API Studio", "Primary + Backup API Keys, Live 3.8/2.5 Model", Icons.Rounded.AutoAwesome, NeonCyan),
        StudioCategory(2, "2. Voice & Audio Pipeline Tuning", "12 Female + 4 Male Hero Voices, Ear Sensor, Boost", Icons.Rounded.RecordVoiceOver, NeonPink),
        StudioCategory(3, "3. Persona & Behavior Studio", "Suzy, Vikram South Hero, Zara, Tara & Custom Note", Icons.Rounded.Face, NeonPurple),
        StudioCategory(4, "4. ORB, Theme & HUD Customizer", "6 ORB Shaders, 6 UI Skins, Zen Mode & Corners", Icons.Rounded.Palette, Color(0xFFF59E0B)),
        StudioCategory(5, "5. Wake-Word & Sensors Lab", "Custom Wake Phrase, Shake-to-Call & Flip-to-Mute", Icons.Rounded.Hearing, Color(0xFF34D399)),
        StudioCategory(6, "6. Call & Notification Alerts Studio", "Gemini Call Voice, OTP Shield & Lock-Screen Guard", Icons.Rounded.NotificationsActive, NeonCyan),
        StudioCategory(7, "7. Owner Mode & System Permissions", "Owner Passphrase/PIN, Media/Call Shields, WRITE_SETTINGS", Icons.Rounded.Security, Color(0xFFEF4444)),
        StudioCategory(8, "8. Smart Triggers, Diagnostics & About", "Charger/Battery Rules, Thermal Alert, Version Info", Icons.Rounded.BugReport, NeonPurple)
    )

    val modelPresets = listOf(
        "Gemini 3.8 Live" to "models/gemini-3.8-live",
        "Gemini 2.5 Live" to "models/gemini-2.5-flash-native-audio-preview-12-2025"
    )

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    if (activeSection != 0) activeSection = 0 else onBack()
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeSection == 0) "SETTINGS STUDIO (8 MODULES)" else studios.first { it.id == activeSection }.title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeSection == 0) "Har setting category ka dedicated full-screen studio" else "Save dabate hi live app & voice mein apply hoga",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            if (activeSection == 0) {
                studios.forEach { st ->
                    GlassCard(
                        accentBorder = st.tint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSection = st.id }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(st.icon, contentDescription = null, tint = st.tint, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(st.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(st.subtitle, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            } else {
                when (activeSection) {
                    1 -> {
                        GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("Primary Gemini API Key") },
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = backupApiKey,
                                    onValueChange = { backupApiKey = it },
                                    label = { Text("Backup API Key (Auto-Failover)") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Live Voice Model", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    modelPresets.forEach { (label, id) ->
                                        FilterChip(selected = modelId.equals(id, true), onClick = { modelId = id }, label = { Text(label, fontSize = 11.sp) })
                                    }
                                }
                                OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Custom Model ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    2 -> {
                        GlassCard(accentBorder = NeonPink, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Female Voices (12)", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    SuzyCatalog.femaleVoices.forEach { v ->
                                        FilterChip(selected = voiceName.equals(v.id, true), onClick = { voiceName = v.id }, label = { Text("${v.id} (${v.style})", fontSize = 11.sp) })
                                    }
                                }
                                Text("South Movie Male Hero Voices (4)", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    SuzyCatalog.maleHeroVoices.forEach { v ->
                                        FilterChip(selected = voiceName.equals(v.id, true), onClick = { voiceName = v.id }, label = { Text("${v.id} (${v.style})", fontSize = 11.sp) })
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Proximity Ear-Sensor Auto-Routing", color = TextPrimary, fontSize = 13.sp)
                                        Text("Phone kaan par lagate hi earpiece par shift hoga", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = proximityEar, onCheckedChange = { proximityEar = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Live Interruption (Barge-In)", color = TextPrimary, fontSize = 13.sp)
                                        Text("Bolte waqt beech mein rokne ki suvidha", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = bargeIn, onCheckedChange = { bargeIn = it })
                                }
                                Text("Audio Loudness Booster: +${loudnessBoost / 100} dB", color = NeonCyan, fontSize = 12.sp)
                                Slider(value = loudnessBoost.toFloat(), onValueChange = { loudnessBoost = it.toInt() }, valueRange = 0f..1500f)
                            }
                        }
                    }

                    3 -> {
                        GlassCard(accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Active Persona (Female + Vikram South Hero)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    SuzyCatalog.personas.forEach { p ->
                                        FilterChip(
                                            selected = personaName.equals(p.name, true),
                                            onClick = { personaName = p.name; voiceName = p.voiceName; temperature = p.temperature },
                                            label = { Text("${p.name} (${p.gender})", fontSize = 11.sp) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = customNote,
                                    onValueChange = { customNote = it },
                                    label = { Text("Global Personal Instruction") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Expressiveness: ${String.format(Locale.US, "%.2f", temperature)}", color = TextPrimary, fontSize = 12.sp)
                                Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0.3f..1.4f)
                            }
                        }
                    }

                    4 -> {
                        GlassCard(accentBorder = Color(0xFFF59E0B), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("3D ORB Geometry Style", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    SuzyCatalog.orbStyles.forEach { st ->
                                        FilterChip(selected = orbStyle == st.id, onClick = { orbStyle = st.id }, label = { Text(st.label, fontSize = 11.sp) })
                                    }
                                }
                                Text("UI Design Theme Skin", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    SuzyCatalog.uiThemeSkins.forEach { sk ->
                                        FilterChip(selected = uiThemeSkin == sk.id, onClick = { uiThemeSkin = sk.id }, label = { Text(sk.label, fontSize = 11.sp) })
                                    }
                                }
                                Text("Layout Density Mode", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("standard" to "Standard 5-Tab", "zen" to "Zen ORB-Only").forEach { (id, lbl) ->
                                        FilterChip(selected = layoutMode == id, onClick = { layoutMode = id }, label = { Text(lbl, fontSize = 11.sp) })
                                    }
                                }
                                Text("Card Corner Radius: ${cornerRadius}dp", color = TextPrimary, fontSize = 12.sp)
                                Slider(value = cornerRadius.toFloat(), onValueChange = { cornerRadius = it.toInt() }, valueRange = 6f..30f)
                                Text("ORB Glow Intensity: ${String.format(Locale.US, "%.1f", orbGlow)}x", color = TextPrimary, fontSize = 12.sp)
                                Slider(value = orbGlow, onValueChange = { orbGlow = it }, valueRange = 0.4f..2.0f)
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Live Transcript HUD", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = captions, onCheckedChange = { captions = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Mood-to-ORB Auto Color Sync", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = moodAutoOrb, onCheckedChange = { moodAutoOrb = it })
                                }
                            }
                        }
                    }

                    5 -> {
                        GlassCard(accentBorder = Color(0xFF34D399), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Hands-Free Wake Word", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = wakeWord, onCheckedChange = { wakeWord = it })
                                }
                                OutlinedTextField(value = customWakePhrase, onValueChange = { customWakePhrase = it }, label = { Text("Custom Wake Phrase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Double-Shake to Start/End Call", color = TextPrimary, fontSize = 13.sp)
                                        Text("Phone hila kar bina screen dekhe call connect karein", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = shakeToCall, onCheckedChange = { shakeToCall = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Flip Face-Down to Instant Mute", color = TextPrimary, fontSize = 13.sp)
                                        Text("Table par ulta rakhte hi mic mute ho jayega", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = flipToMute, onCheckedChange = { flipToMute = it })
                                }
                            }
                        }
                    }

                    6 -> {
                        GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Gemini Incoming Call Announcer", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = callAnnouncer, onCheckedChange = { callAnnouncer = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Speak Saved Caller Name", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = announceCallerName, onCheckedChange = { announceCallerName = it })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Girlfriend", "Bestie", "Jarvis").forEach { st ->
                                        FilterChip(selected = announcerStyle == st, onClick = { announcerStyle = st }, label = { Text(st, fontSize = 11.sp) })
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Gemini Notification Reader", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = notifReader, onCheckedChange = { notifReader = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Read Message Content Aloud", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = readNotifContent, onCheckedChange = { readNotifContent = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("OTP & Banking Security Blocker", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = blockSensitive, onCheckedChange = { blockSensitive = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Lock-Screen Privacy Guard", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = hideLockScreen, onCheckedChange = { hideLockScreen = it })
                                }
                            }
                        }
                    }

                    7 -> {
                        GlassCard(accentBorder = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = ownerNickname, onValueChange = { ownerNickname = it }, label = { Text("Boss / Owner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = ownerPassphrase, onValueChange = { ownerPassphrase = it }, label = { Text("Secret Voice Passphrase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = ownerPin, onValueChange = { ownerPin = it.take(6) }, label = { Text("Master Owner PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = stealthCodeword, onValueChange = { stealthCodeword = it }, label = { Text("Stealth Panic Codeword") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Strict Owner-Only Command Lock", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Sensitive commands sirf Owner Passphrase/PIN se chalenge", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = ownerStrict, onCheckedChange = { ownerStrict = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("YouTube & Media Playback Blocker", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = mediaShield, onCheckedChange = { mediaShield = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Phone & VoIP Call Privacy Lock", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = callShield, onCheckedChange = { callShield = it })
                                }
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                            context.startActivity(i)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("GRANT SYSTEM WRITE_SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { PermissionHelper.openNotificationListenerSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("OPEN NOTIFICATION LISTENER SETTINGS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    8 -> {
                        GlassCard(accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Charger Plug-In Voice Reaction", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = chargerReaction, onCheckedChange = { chargerReaction = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Overheat (>42°C) Thermal Alert", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = thermalAlert, onCheckedChange = { thermalAlert = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Bluetooth Earbuds Auto-Companion", color = TextPrimary, fontSize = 13.sp)
                                    Switch(checked = btAutoMode, onCheckedChange = { btAutoMode = it })
                                }
                                Text("Low Battery Voice Alert: ${lowBatThreshold}%", color = NeonCyan, fontSize = 12.sp)
                                Slider(value = lowBatThreshold.toFloat(), onValueChange = { lowBatThreshold = it.toInt() }, valueRange = 5f..40f)
                                Text("Suzy AI v${BuildConfig.VERSION_NAME} • Omni-OS Edition (150 Modules)", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        commitAll()
                        activeSection = 0
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("SAVE & APPLY LIVE", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
