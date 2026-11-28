package com.suzy.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.BuildConfig
import com.suzy.ai.data.AutomationRuleItem
import com.suzy.ai.data.FinanceEntryItem
import com.suzy.ai.data.HabitEntryItem
import com.suzy.ai.data.NoteVaultItem
import com.suzy.ai.data.PlannerTaskItem
import com.suzy.ai.data.SavedTranscript
import com.suzy.ai.data.SecurityAuditItem
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import com.suzy.ai.viewmodel.SuzyUiState
import java.util.Locale

data class DedicatedHubModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun HubAndModulesScreen(
    activeModuleId: String?,
    onSelectModule: (String?) -> Unit,
    uiState: SuzyUiState,
    settings: SuzySettings,
    tasks: List<PlannerTaskItem>,
    financeEntries: List<FinanceEntryItem>,
    habits: List<HabitEntryItem>,
    notes: List<NoteVaultItem>,
    rules: List<AutomationRuleItem>,
    securityLogs: List<SecurityAuditItem>,
    parkingNote: String,
    medicalId: String,
    transcripts: List<SavedTranscript>,
    updateStatus: String,
    onUpdateSettings: (SuzySettings) -> Unit,
    onRunCommand: (String) -> Unit,
    onSpeakInGeminiVoice: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddFinance: (String, String, String, Double) -> Unit,
    onToggleFinanceSettled: (String) -> Unit,
    onToggleHabit: (String) -> Unit,
    onAddHabit: (String) -> Unit,
    onAddNote: (String, String, String) -> Unit,
    onToggleNote: (String) -> Unit,
    onAddRule: (String, String, String, String) -> Unit,
    onSaveParking: (String) -> Unit,
    onSaveMedical: (String) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleMediaShield: (Boolean) -> Unit,
    onToggleCallPrivacyShield: (Boolean) -> Unit,
    onToggleCallAnnouncer: (Boolean) -> Unit,
    onToggleNotificationReader: (Boolean) -> Unit,
    onTestCallAnnouncement: () -> Unit,
    onTestNotificationAnnouncement: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onTestServerPing: () -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onCheckUpdate: () -> Unit,
    onPrepareMicForVoiceprint: (Boolean) -> Unit,
    onYieldMicManually: () -> Unit,
    onLogSecurityEvent: (String, String, String) -> Unit,
    onOpenSettingsStudio: (Int) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val modules = listOf(
        DedicatedHubModule("mod_owner", "Owner Voice ID & Lock", "Voiceprint, PIN, Stealth Panic & Security Log", "25-30, 115", Icons.Rounded.Fingerprint, Color(0xFFEF4444)),
        DedicatedHubModule("mod_orb_ui", "ORB & UI Morpher", "6 3D ORBs, 6 UI Skins, Zen Mode & Corners", "109-125", Icons.Rounded.Palette, NeonCyan),
        DedicatedHubModule("mod_attitude", "Bond, Gussa & Attitude", "Affection XP, Narazgi Meter, Roast & Drama", "2, 145-150", Icons.Rounded.Favorite, NeonPink),
        DedicatedHubModule("mod_alerts", "Wake-Word & Safe Alerts", "Hey Suzy, Call Announcer, OTP & Media Shield", "ALERTS", Icons.Rounded.NotificationsActive, Color(0xFF34D399)),
        DedicatedHubModule("mod_finance", "Finance, Udhaar & GST", "Expense ₹, Udhaar Ledger, Bills, Jars & Calc", "15, 50, 83, 91", Icons.Rounded.AccountBalanceWallet, Color(0xFFF59E0B)),
        DedicatedHubModule("mod_planner", "Planner, Habits & Health", "Tasks, Alarms, Medicine, Calendar & Focus", "4, 13, 49, 93", Icons.Rounded.Alarm, NeonPurple),
        DedicatedHubModule("mod_vision", "Vision Eye & Media Hub", "Camera Analyzer, Clipboard/PDF, Translator & QR", "1, 16, 18, 51", Icons.Rounded.CameraAlt, NeonCyan),
        DedicatedHubModule("mod_comm", "Direct WhatsApp & Dialer", "VIP Speed-Dial, WhatsApp Sender & SOS Card", "7, 14, 21, 96", Icons.Rounded.PhoneInTalk, Color(0xFF10B981)),
        DedicatedHubModule("mod_tactical", "Jarvis Tactical Radar", "Compass, Wi-Fi, Decibel, Steps, Battery & Storage", "53-57, 70, 102", Icons.Rounded.Explore, Color(0xFF38BDF8)),
        DedicatedHubModule("mod_automation", "Macros, Webhooks & GitHub", "Command Simulator, Termux Webhooks & Build Radar", "19, 23, 36, 61", Icons.Rounded.Terminal, NeonPink),
        DedicatedHubModule("mod_sanctuary", "Sanctuary & Audio Lab", "Night Sleep Mixer, Loudness Boost & Driving HUD", "5, 17, 60", Icons.Rounded.NightlightRound, Color(0xFF818CF8)),
        DedicatedHubModule("mod_settings", "8 Settings Studios", "API, Voices, Personas, Sensors & Permissions", "37-48", Icons.Rounded.Settings, NeonPurple)
    )

    val filteredModules = remember(searchQuery) {
        if (searchQuery.isBlank()) modules
        else modules.filter {
            it.title.contains(searchQuery, true) || it.subtitle.contains(searchQuery, true) || it.badge.contains(searchQuery, true)
        }
    }

    if (activeModuleId == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OMNI-OS MASTER HUB", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("150 Dedicated Features • Har section apni dedicated screen mein", color = TextSecondary, fontSize = 11.sp)
                }
                GlassCard(shape = RoundedCornerShape(50), accentBorder = NeonPink, modifier = Modifier.clickable { onRunCommand("boss briefing do") }) {
                    Text("BOSS BRIEF", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 150 features (Owner, ORB, Udhaar, Vision, Radar...)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredModules, key = { it.id }) { mod ->
                    GlassCard(
                        accentBorder = mod.tint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (mod.id == "mod_settings") {
                                    onOpenSettingsStudio(0)
                                } else {
                                    onSelectModule(mod.id)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(13.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(mod.tint.copy(alpha = 0.22f))
                                ) {
                                    Icon(mod.icon, contentDescription = null, tint = mod.tint, modifier = Modifier.size(18.dp))
                                }
                                Text(mod.badge, color = mod.tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(mod.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(mod.subtitle, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    } else {
        val currentMod = modules.firstOrNull { it.id == activeModuleId }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onSelectModule(null) }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to Hub", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentMod?.title ?: "Dedicated Screen", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(currentMod?.subtitle ?: "", color = TextSecondary, fontSize = 11.sp)
                }
            }

            when (activeModuleId) {
                "mod_owner" -> {
                    OwnerVoiceIdScreen(
                        settings = settings,
                        securityLogs = securityLogs,
                        isExternalMicYielded = uiState.isExternalMicYielded,
                        externalMicReason = uiState.externalMicReason,
                        onUpdateSettings = onUpdateSettings,
                        onPrepareMicForVoiceprint = onPrepareMicForVoiceprint,
                        onYieldMicManually = onYieldMicManually,
                        onLogSecurityEvent = onLogSecurityEvent,
                        onRunCommand = onRunCommand
                    )
                }

                "mod_orb_ui" -> {
                    GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("3D ORB GEOMETRY MORPHER", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            SuzyCatalog.orbStyles.forEach { st ->
                                val selected = settings.orbStyle == st.id
                                GlassCard(
                                    accentBorder = if (selected) NeonPink else null,
                                    modifier = Modifier.fillMaxWidth().clickable { onUpdateSettings(settings.copy(orbStyle = st.id)) }
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Column {
                                            Text(st.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(st.subtitle, color = TextSecondary, fontSize = 11.sp)
                                        }
                                        if (selected) Text("ACTIVE", color = NeonPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("LIVE UI THEME SKINS", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                SuzyCatalog.uiThemeSkins.forEach { sk ->
                                    FilterChip(
                                        selected = settings.uiThemeSkin == sk.id,
                                        onClick = { onUpdateSettings(settings.copy(uiThemeSkin = sk.id)) },
                                        label = { Text(sk.label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Zen ORB-Only Minimal Layout", color = TextPrimary, fontSize = 13.sp)
                                Switch(
                                    checked = settings.layoutMode == "zen",
                                    onCheckedChange = { onUpdateSettings(settings.copy(layoutMode = if (it) "zen" else "standard")) }
                                )
                            }
                        }
                    }
                }

                "mod_attitude" -> {
                    val stage = when {
                        settings.bondXp >= 1000 -> "Soulmate / Partner"
                        settings.bondXp >= 500 -> "Bestie / Close Crush"
                        else -> "Growing Bond"
                    }
                    GlassCard(accentBorder = NeonPink, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("BOND LEVEL: $stage (${settings.bondXp} XP • ${settings.streakDays}d Streak)", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            LinearProgressIndicator(
                                progress = { ((settings.bondXp % 500) / 500f).coerceIn(0.1f, 1f) },
                                color = NeonPink,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                            )
                            Text("Narazgi / Gussa Meter: ${settings.angerLevel}% | Trust Score: ${settings.trustScore}/100", color = TextPrimary, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onUpdateSettings(settings.copy(angerLevel = (settings.angerLevel + 35).coerceAtMost(100), activeMoodId = "naraz")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("TRIGGER NARAZGI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { onUpdateSettings(settings.copy(angerLevel = 0, activeMoodId = "romantic", trustScore = (settings.trustScore + 2).coerceAtMost(100))) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("SORRY / MANA LO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            Text("Roast Intensity", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Light", "Sharp", "Savage").forEach { r ->
                                    FilterChip(selected = settings.roastIntensity == r, onClick = { onUpdateSettings(settings.copy(roastIntensity = r)) }, label = { Text(r, fontSize = 11.sp) })
                                }
                            }
                        }
                    }
                }

                "mod_alerts" -> {
                    GlassCard(accentBorder = Color(0xFF34D399), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Hands-Free Wake Word (\"${settings.customWakePhrase}\")", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Switch(checked = settings.wakeWordEnabled, onCheckedChange = onToggleWakeWord)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("YouTube & Media Playback Shield", color = TextPrimary, fontSize = 13.sp)
                                Switch(checked = settings.mediaShieldEnabled, onCheckedChange = onToggleMediaShield)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Phone & VoIP Call Privacy Lock", color = TextPrimary, fontSize = 13.sp)
                                Switch(checked = settings.callPrivacyShieldEnabled, onCheckedChange = onToggleCallPrivacyShield)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Gemini Incoming Call Announcer", color = TextPrimary, fontSize = 13.sp)
                                Switch(checked = settings.callAnnouncerEnabled, onCheckedChange = onToggleCallAnnouncer)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Safe Gemini Notification Reader", color = TextPrimary, fontSize = 13.sp)
                                Switch(checked = settings.notificationReaderEnabled, onCheckedChange = onToggleNotificationReader)
                            }
                            Button(onClick = onTestCallAnnouncement, colors = ButtonDefaults.buttonColors(containerColor = NeonPink), modifier = Modifier.fillMaxWidth()) {
                                Text("TEST GEMINI CALL VOICE", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = onTestNotificationAnnouncement, colors = ButtonDefaults.buttonColors(containerColor = NeonPurple), modifier = Modifier.fillMaxWidth()) {
                                Text("TEST SAFE NOTIFICATION READER", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "mod_finance" -> {
                    var entryTitle by remember { mutableStateOf("") }
                    var entryAmt by remember { mutableStateOf("") }
                    var entryKind by remember { mutableStateOf("EXPENSE") }
                    var gstBase by remember { mutableStateOf("10000") }

                    val totalExpense = financeEntries.filter { it.kind == "EXPENSE" }.sumOf { it.amountInr }
                    val totalUdhaar = financeEntries.filter { it.kind == "UDHAAR_LENA" && !it.settled }.sumOf { it.amountInr }

                    GlassCard(accentBorder = Color(0xFFF59E0B), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TODAY EXPENSE: ₹${totalExpense.toInt()}  |  UDHAAR LENA: ₹${totalUdhaar.toInt()}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                listOf("EXPENSE", "UDHAAR_LENA", "SAVINGS", "BILL").forEach { k ->
                                    FilterChip(selected = entryKind == k, onClick = { entryKind = k }, label = { Text(k, fontSize = 10.sp) })
                                }
                            }
                            OutlinedTextField(value = entryTitle, onValueChange = { entryTitle = it }, label = { Text("Title / Person Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = entryAmt, onValueChange = { entryAmt = it }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    val amt = entryAmt.toDoubleOrNull() ?: 0.0
                                    if (entryTitle.isNotBlank() && amt > 0) {
                                        onAddFinance(entryKind, entryTitle, entryTitle, amt)
                                        entryTitle = ""; entryAmt = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("ADD ENTRY", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }

                    GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("INSTANT GST & INR CONVERTER", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = gstBase, onValueChange = { gstBase = it }, label = { Text("Amount in ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            val baseVal = gstBase.toDoubleOrNull() ?: 0.0
                            Text("18% GST: ₹${String.format(Locale.US, "%.2f", baseVal * 0.18)} | Total: ₹${String.format(Locale.US, "%.2f", baseVal * 1.18)} | USD ≈ $${String.format(Locale.US, "%.2f", baseVal / 84.0)}", color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    financeEntries.forEach { f ->
                        GlassCard(modifier = Modifier.fillMaxWidth().clickable { onToggleFinanceSettled(f.id) }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${f.kind} • ${f.title}", color = if (f.settled) TextSecondary else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(f.dueDateOrNote, color = TextSecondary, fontSize = 10.sp)
                                }
                                Text("₹${f.amountInr.toInt()}${if (f.settled) " ✓" else ""}", color = if (f.settled) Color(0xFF34D399) else Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "mod_planner" -> {
                    var newTask by remember { mutableStateOf("") }
                    GlassCard(accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DAILY TASKS & FOCUS POMODORO", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = newTask, onValueChange = { newTask = it }, placeholder = { Text("Add new task...") }, singleLine = true, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { if (newTask.isNotBlank()) { onAddTask(newTask); newTask = "" } }, colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 25, 45).forEach { m ->
                                    Button(onClick = { onStartTimer(m) }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.weight(1f)) {
                                        Text("${m}m Focus", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    tasks.forEach { t ->
                        GlassCard(modifier = Modifier.fillMaxWidth().clickable { onToggleTask(t.id) }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(t.title, color = if (t.completed) TextSecondary else TextPrimary, fontSize = 13.sp)
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (t.completed) Color(0xFF34D399) else TextSecondary)
                            }
                        }
                    }
                    Text("DAILY HABIT STREAKS", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    habits.forEach { h ->
                        GlassCard(modifier = Modifier.fillMaxWidth().clickable { onToggleHabit(h.id) }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("${h.name} (${h.streak}d streak)", color = TextPrimary, fontSize = 13.sp)
                                Text(if (h.doneToday) "DONE ✓" else "MARK", color = if (h.doneToday) Color(0xFF34D399) else NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                "mod_vision" -> {
                    var clipText by remember { mutableStateOf("") }
                    var translateInput by remember { mutableStateOf("") }
                    GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("CLIPBOARD & DOCUMENT VOICE READER", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipText = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                    if (clipText.isNotBlank()) onSpeakInGeminiVoice(clipText.take(300))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("PASTE & READ CLIPBOARD IN GEMINI VOICE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            if (clipText.isNotBlank()) {
                                Text(clipText, color = TextPrimary, fontSize = 12.sp)
                            }

                            Text("LIVE HINGLISH <-> ENGLISH TRANSLATOR", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = translateInput, onValueChange = { translateInput = it }, label = { Text("Text to translate & speak") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (translateInput.isNotBlank()) {
                                        onSpeakInGeminiVoice("Isko natural fluent English mein translate karke bol kar sunao: $translateInput")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("TRANSLATE & SPEAK ALOUD", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                "mod_comm" -> {
                    var waNumber by remember { mutableStateOf("") }
                    var waMsg by remember { mutableStateOf("Main 10 minute mein pahunch raha hoon") }
                    GlassCard(accentBorder = Color(0xFF10B981), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DIRECT WHATSAPP & SPEED-DIAL", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = waNumber, onValueChange = { waNumber = it }, label = { Text("Phone Number (with +91)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = waMsg, onValueChange = { waMsg = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    val cleanNum = waNumber.filter { it.isDigit() }
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNum&text=${Uri.encode(waMsg)}")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("SEND DIRECT WHATSAPP", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                "mod_tactical" -> {
                    val stat = StatFs(Environment.getDataDirectory().path)
                    val freeGb = (stat.availableBytes / (1024.0 * 1024.0 * 1024.0))
                    val totalGb = (stat.totalBytes / (1024.0 * 1024.0 * 1024.0))
                    var parkingInput by remember { mutableStateOf("") }

                    GlassCard(accentBorder = Color(0xFF38BDF8), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("JARVIS TELEMETRY & STORAGE RADAR", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Battery: ${uiState.batteryPercent}% ${if (uiState.isCharging) "(Charging)" else ""}", color = TextPrimary, fontSize = 13.sp)
                            Text("Internal Storage: ${String.format(Locale.US, "%.1f", freeGb)} GB Free / ${String.format(Locale.US, "%.1f", totalGb)} GB", color = TextPrimary, fontSize = 13.sp)
                            Text("Server Ping: ${uiState.pingMs?.let { "${it} ms" } ?: "Tap Test"}", color = NeonCyan, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onTestServerPing, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.weight(1f)) {
                                    Text("PING RADAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(onClick = onToggleFlashlight, colors = ButtonDefaults.buttonColors(containerColor = NeonPurple), modifier = Modifier.weight(1f)) {
                                    Text(if (uiState.isFlashlightOn) "TORCH OFF" else "TORCH ON", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            Text("SAVED PARKING SPOT: $parkingNote", color = NeonPink, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = parkingInput, onValueChange = { parkingInput = it }, placeholder = { Text("e.g. Basement 2 Pillar B4") }, singleLine = true, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { if (parkingInput.isNotBlank()) { onSaveParking(parkingInput); parkingInput = "" } }) { Text("SAVE") }
                            }
                        }
                    }
                }

                "mod_automation" -> {
                    var simCmd by remember { mutableStateOf("") }
                    GlassCard(accentBorder = NeonPink, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LIVE COMMAND SIMULATOR & CHAIN TESTER", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = simCmd,
                                onValueChange = { simCmd = it },
                                placeholder = { Text("e.g. Arc reactor lagao aur 15 min timer lagao") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { if (simCmd.isNotBlank()) { onRunCommand(simCmd); simCmd = "" } },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("EXECUTE COMMAND CHAIN", fontWeight = FontWeight.Bold) }
                        }
                    }
                    rules.forEach { r ->
                        GlassCard(modifier = Modifier.fillMaxWidth().clickable { onRunCommand(r.triggerPhrase) }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${r.kind} • \"${r.triggerPhrase}\"", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(r.actionSummary, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                "mod_sanctuary" -> {
                    var boostVal by remember { mutableIntStateOf(settings.loudnessBoostDb) }
                    GlassCard(accentBorder = Color(0xFF818CF8), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("SANCTUARY SLEEP MODE & AUDIO LAB", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Outdoor Voice Loudness Booster: +${boostVal / 100} dB", color = TextPrimary, fontSize = 13.sp)
                            Slider(
                                value = boostVal.toFloat(),
                                onValueChange = {
                                    boostVal = it.toInt()
                                    onUpdateSettings(settings.copy(loudnessBoostDb = boostVal))
                                },
                                valueRange = 0f..1500f
                            )
                            Button(
                                onClick = {
                                    onUpdateSettings(settings.copy(activePersonaName = "Naina", voiceName = "Despina", uiThemeSkin = "amoled", orbStyle = "breathing_halo"))
                                    onStartTimer(30)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("ACTIVATE 30-MIN SLEEP SANCTUARY (NAINA)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
