package com.suzy.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.audio.AudioBlockReason
import com.suzy.ai.data.AutomationRuleItem
import com.suzy.ai.data.ChatMessageItem
import com.suzy.ai.data.FinanceEntryItem
import com.suzy.ai.data.HabitEntryItem
import com.suzy.ai.data.MemoryItem
import com.suzy.ai.data.MoodOption
import com.suzy.ai.data.NoteVaultItem
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.PlannerTaskItem
import com.suzy.ai.data.SavedTranscript
import com.suzy.ai.data.SecurityAuditItem
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.ActiveUiStyleHolder
import com.suzy.ai.ui.components.GlassBackground
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.components.SuzyOrb
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import com.suzy.ai.viewmodel.SuzyState
import com.suzy.ai.viewmodel.SuzyUiState
import java.util.Locale

private data class EmotionChip(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val prompt: String
)

private data class NavTab(
    val label: String,
    val icon: ImageVector
)

private fun formatDuration(totalSeconds: Long): String {
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

@Composable
fun HomeScreen(
    uiState: SuzyUiState,
    settings: SuzySettings,
    memories: List<MemoryItem>,
    customPersonas: List<PersonaProfile>,
    transcripts: List<SavedTranscript>,
    chatMessages: List<ChatMessageItem>,
    tasks: List<PlannerTaskItem>,
    financeEntries: List<FinanceEntryItem>,
    habits: List<HabitEntryItem>,
    notes: List<NoteVaultItem>,
    rules: List<AutomationRuleItem>,
    securityLogs: List<SecurityAuditItem>,
    parkingNote: String,
    medicalId: String,
    updateStatus: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCaptions: () -> Unit,
    onSelectMood: (MoodOption) -> Unit,
    onSelectPersona: (PersonaProfile) -> Unit,
    onCreateCustomPersona: (String, String, String, Long, String, String) -> Unit,
    onDeleteCustomPersona: (String) -> Unit,
    onSelectVoice: (String) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onTogglePinMemory: (String) -> Unit,
    onSendChatMessage: (String, String?, String) -> Unit,
    onSpeakInGeminiVoice: (String) -> Unit,
    onClearChat: () -> Unit,
    onUpdateSettings: (SuzySettings) -> Unit,
    onRunOmniCommand: (String) -> Unit,
    onConsumeNavRoute: () -> Unit,
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
    onQuickReaction: (String) -> Unit,
    onInterruptClick: () -> Unit,
    onOpenSettings: (Int) -> Unit
) {
    ActiveUiStyleHolder.skinId = settings.uiThemeSkin
    ActiveUiStyleHolder.cornerRadiusDp = settings.cornerRadiusDp
    ActiveUiStyleHolder.backdropPreset = settings.backdropPreset

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeHubModuleId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.pendingNavigationRoute) {
        val route = uiState.pendingNavigationRoute ?: return@LaunchedEffect
        when {
            route == "tab_orb" -> selectedTab = 0
            route == "tab_chat" -> selectedTab = 1
            route == "tab_personas" -> selectedTab = 2
            route == "tab_memory" -> selectedTab = 3
            route == "tab_hub" -> { selectedTab = 4; activeHubModuleId = null }
            route == "settings_hub" -> onOpenSettings(0)
            route.startsWith("mod_") -> {
                selectedTab = 4
                activeHubModuleId = route
            }
        }
        onConsumeNavRoute()
    }

    val tabs = listOf(
        NavTab("ORB", Icons.Rounded.GraphicEq),
        NavTab("CHAT", Icons.Rounded.ChatBubble),
        NavTab("PERSONAS", Icons.Rounded.Face),
        NavTab("MEMORY", Icons.Rounded.Psychology),
        NavTab("HUB", Icons.Rounded.Dashboard)
    )

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (selectedTab) {
                    0 -> OrbTab(
                        uiState = uiState,
                        settings = settings,
                        memoryCount = memories.size,
                        onStartClick = onStartClick,
                        onStopClick = onStopClick,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleCaptions = onToggleCaptions,
                        onSelectMood = onSelectMood,
                        onExitZenMode = { onUpdateSettings(settings.copy(layoutMode = "standard")) },
                        onQuickReaction = onQuickReaction,
                        onInterruptClick = onInterruptClick,
                        onOpenSettings = { onOpenSettings(0) }
                    )
                    1 -> ChatScreen(
                        settings = settings,
                        messages = chatMessages,
                        isSending = uiState.isChatSending,
                        onSendMessage = onSendChatMessage,
                        onSpeakInGeminiVoice = onSpeakInGeminiVoice,
                        onClearChat = onClearChat
                    )
                    2 -> PersonasTab(
                        settings = settings,
                        customPersonas = customPersonas,
                        onSelectPersona = {
                            onSelectPersona(it)
                            selectedTab = 0
                        },
                        onCreateCustomPersona = { name, role, voice, colorHex, prompt, gender ->
                            onCreateCustomPersona(name, role, voice, colorHex, prompt, gender)
                            selectedTab = 0
                        },
                        onDeleteCustomPersona = onDeleteCustomPersona,
                        onSelectVoice = onSelectVoice
                    )
                    3 -> MemoryTab(
                        memories = memories,
                        isGuestMode = settings.guestModeActive,
                        onAddMemory = onAddMemory,
                        onDeleteMemory = onDeleteMemory,
                        onTogglePin = onTogglePinMemory
                    )
                    4 -> HubAndModulesScreen(
                        activeModuleId = activeHubModuleId,
                        onSelectModule = { activeHubModuleId = it },
                        uiState = uiState,
                        settings = settings,
                        tasks = tasks,
                        financeEntries = financeEntries,
                        habits = habits,
                        notes = notes,
                        rules = rules,
                        securityLogs = securityLogs,
                        parkingNote = parkingNote,
                        medicalId = medicalId,
                        transcripts = transcripts,
                        updateStatus = updateStatus,
                        onUpdateSettings = onUpdateSettings,
                        onRunCommand = onRunOmniCommand,
                        onSpeakInGeminiVoice = onSpeakInGeminiVoice,
                        onAddTask = onAddTask,
                        onToggleTask = onToggleTask,
                        onAddFinance = onAddFinance,
                        onToggleFinanceSettled = onToggleFinanceSettled,
                        onToggleHabit = onToggleHabit,
                        onAddHabit = onAddHabit,
                        onAddNote = onAddNote,
                        onToggleNote = onToggleNote,
                        onAddRule = onAddRule,
                        onSaveParking = onSaveParking,
                        onSaveMedical = onSaveMedical,
                        onToggleWakeWord = onToggleWakeWord,
                        onToggleMediaShield = onToggleMediaShield,
                        onToggleCallPrivacyShield = onToggleCallPrivacyShield,
                        onToggleCallAnnouncer = onToggleCallAnnouncer,
                        onToggleNotificationReader = onToggleNotificationReader,
                        onTestCallAnnouncement = onTestCallAnnouncement,
                        onTestNotificationAnnouncement = onTestNotificationAnnouncement,
                        onToggleFlashlight = onToggleFlashlight,
                        onTestServerPing = onTestServerPing,
                        onStartTimer = onStartTimer,
                        onCancelTimer = onCancelTimer,
                        onCheckUpdate = onCheckUpdate,
                        onOpenSettingsStudio = onOpenSettings
                    )
                }
            }

            // Floating 5-Tab Bottom Navigation Bar: ORB • CHAT • PERSONAS • MEMORY • HUB
            if (settings.layoutMode != "zen" || selectedTab != 0) {
                GlassCard(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val active = selectedTab == index
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (active) NeonPurple.copy(alpha = 0.32f) else Color.Transparent)
                                    .border(
                                        width = if (active) 1.dp else 0.dp,
                                        color = if (active) NeonCyan.copy(alpha = 0.65f) else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        if (index == 4 && selectedTab == 4) activeHubModuleId = null
                                        selectedTab = index
                                    }
                                    .padding(horizontal = if (active) 11.dp else 9.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (active) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (active) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = tab.label,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbTab(
    uiState: SuzyUiState,
    settings: SuzySettings,
    memoryCount: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCaptions: () -> Unit,
    onSelectMood: (MoodOption) -> Unit,
    onExitZenMode: () -> Unit,
    onQuickReaction: (String) -> Unit,
    onInterruptClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isActive = uiState.state == SuzyState.CONNECTING ||
        uiState.state == SuzyState.LISTENING ||
        uiState.state == SuzyState.SPEAKING

    val emotions = listOf(
        EmotionChip("Laugh", Icons.Rounded.SentimentVerySatisfied, NeonCyan, "Zor se khul kar hanso aur koi cute funny si line bolo Hinglish mein!"),
        EmotionChip("Love", Icons.Rounded.Favorite, NeonPink, "Pyaar se soft romantic voice mein ek sweet si line bolo."),
        EmotionChip("Hero Swag", Icons.Rounded.AutoAwesome, Color(0xFFF59E0B), "Ekdum South movie hero jaisa dumdaar stylish punch dialogue bolo Hinglish mein!"),
        EmotionChip("Care", Icons.Rounded.Spa, Color(0xFF34D399), "Bahut pyaar se caring voice mein mera mood theek karo.")
    )

    val captionScrollState = rememberScrollState()
    val captionToShow = uiState.liveCaption.ifBlank { uiState.lastCaption }
    LaunchedEffect(captionToShow) {
        captionScrollState.animateScrollTo(captionScrollState.maxValue)
    }

    val orbPrimary = Color(settings.orbPrimaryHex)
    val orbSecondary = Color(settings.orbSecondaryHex)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (settings.layoutMode == "zen") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GlassCard(shape = RoundedCornerShape(50), accentBorder = NeonCyan, modifier = Modifier.clickable { onExitZenMode() }) {
                    Text("EXIT ZEN", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    shape = RoundedCornerShape(18.dp),
                    accentBorder = NeonPurple,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = settings.activePersonaName.uppercase(),
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isActive && uiState.callDurationSec > 0L) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = formatDuration(uiState.callDurationSec), color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else if (!isActive && settings.wakeWordEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val (badgeText, badgeColor) = when (uiState.audioBlockReason) {
                                    AudioBlockReason.MEDIA_PLAYING -> "MEDIA SHIELD" to Color(0xFFF59E0B)
                                    AudioBlockReason.PHONE_CALL_ACTIVE -> "CALL SHIELD" to Color(0xFFEF4444)
                                    else -> if (uiState.isWakeWordListening) ("WAKE ON" to Color(0xFF34D399)) else ("STANDBY" to TextSecondary)
                                }
                                Text(text = badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "${settings.voiceName} • ${settings.orbStyle.uppercase()} • ${memoryCount} MEM",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            letterSpacing = 0.7.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassCard(shape = CircleShape, accentBorder = if (settings.captionsEnabled) NeonPurple else null) {
                        IconButton(onClick = onToggleCaptions, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = if (settings.captionsEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                                contentDescription = "Live Captions",
                                tint = if (settings.captionsEnabled) NeonCyan else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    GlassCard(shape = CircleShape, accentBorder = if (uiState.isSpeakerOn) NeonCyan else null) {
                        IconButton(onClick = onToggleSpeaker, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = if (uiState.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.PhoneInTalk,
                                contentDescription = "Speaker Toggle",
                                tint = if (uiState.isSpeakerOn) NeonCyan else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    GlassCard(shape = CircleShape) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SuzyOrb(
                state = uiState.state,
                amplitude = uiState.amplitude,
                orbSize = if (settings.layoutMode == "zen") 270.dp else if (isActive && settings.captionsEnabled) 205.dp else 235.dp,
                orbStyle = settings.orbStyle,
                primaryColor = orbPrimary,
                secondaryColor = orbSecondary,
                glowIntensity = settings.orbGlowIntensity,
                spinSpeed = settings.orbSpinSpeed
            )

            Spacer(modifier = Modifier.height(8.dp))

            val pillText = when {
                isActive -> "${uiState.statusText}  •  LOCK-SCREEN ACTIVE"
                uiState.audioBlockReason == AudioBlockReason.MEDIA_PLAYING -> "Media Playing • Mic & Wake Word Auto-Paused"
                uiState.audioBlockReason == AudioBlockReason.PHONE_CALL_ACTIVE -> "Call Active • Privacy Shield Locked Mic"
                else -> uiState.statusText
            }

            GlassCard(
                shape = RoundedCornerShape(50),
                accentBorder = when {
                    uiState.state == SuzyState.SPEAKING -> NeonPink
                    uiState.state == SuzyState.LISTENING -> NeonCyan
                    uiState.state == SuzyState.ERROR -> Color(0xFFEF4444)
                    else -> NeonPurple
                }
            ) {
                Text(
                    text = pillText,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (settings.captionsEnabled && isActive && settings.layoutMode != "zen") {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    shape = RoundedCornerShape(16.dp),
                    accentBorder = if (uiState.state == SuzyState.SPEAKING) NeonPink else NeonPurple,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text(
                            text = "${settings.activePersonaName.uppercase()} • LIVE TRANSCRIPT",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp, max = 56.dp).verticalScroll(captionScrollState)) {
                            Text(
                                text = captionToShow.ifBlank { "Baat shuru karo, live transcript yahan dikhega..." },
                                color = if (captionToShow.isBlank()) TextSecondary else TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (settings.layoutMode != "zen") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp)
                ) {
                    SuzyCatalog.moods.forEach { mood ->
                        val selected = settings.activeMoodId == mood.id
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            accentBorder = if (selected) mood.color else null,
                            modifier = Modifier.clickable { onSelectMood(mood) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Icon(Icons.Rounded.Mood, contentDescription = mood.label, tint = if (selected) mood.color else TextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mood.label, color = if (selected) TextPrimary else TextSecondary, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                if (isActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        emotions.forEach { item ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                accentBorder = item.tint,
                                modifier = Modifier.clickable { onQuickReaction(item.prompt) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(item.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActive) {
                    GlassCard(shape = CircleShape, accentBorder = if (uiState.isMuted) Color(0xFFEF4444) else NeonCyan) {
                        IconButton(onClick = onToggleMute, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = "Mute",
                                tint = if (uiState.isMuted) Color(0xFFEF4444) else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(52.dp)
                        .width(180.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            if (isActive) Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEC4899)))
                            else Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                        )
                        .clickable { if (isActive) onStopClick() else onStartClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isActive) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isActive) "END CALL" else "START CALL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.4.sp)
                    }
                }

                if (isActive) {
                    Spacer(modifier = Modifier.width(14.dp))
                    GlassCard(shape = CircleShape, accentBorder = NeonPink) {
                        IconButton(onClick = onInterruptClick, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = "Interrupt", tint = NeonPink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonasTab(
    settings: SuzySettings,
    customPersonas: List<PersonaProfile>,
    onSelectPersona: (PersonaProfile) -> Unit,
    onCreateCustomPersona: (String, String, String, Long, String, String) -> Unit,
    onDeleteCustomPersona: (String) -> Unit,
    onSelectVoice: (String) -> Unit
) {
    var showBuilder by remember { mutableStateOf(false) }
    val allPersonas = customPersonas + SuzyCatalog.personas

    if (showBuilder) {
        CustomPersonaDialog(
            onDismiss = { showBuilder = false },
            onSave = { name, role, voice, colorHex, prompt, gender ->
                onCreateCustomPersona(name, role, voice, colorHex, prompt, gender)
                showBuilder = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PERSONAS & VOICE STUDIO", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Female Companions + Vikram South Movie Mass Hero", color = TextSecondary, fontSize = 11.sp)
            }

            GlassCard(shape = RoundedCornerShape(14.dp), accentBorder = NeonPink, modifier = Modifier.clickable { showBuilder = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create", tint = NeonPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CREATE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("SOUTH MOVIE HERO MALE VOICES (4) & FEMALE VOICES (12)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            (SuzyCatalog.maleHeroVoices + SuzyCatalog.femaleVoices).forEach { v ->
                val selected = settings.voiceName.equals(v.id, ignoreCase = true)
                val borderTint = if (selected) (if (v.gender == "Male") Color(0xFFF59E0B) else NeonCyan) else null
                GlassCard(shape = RoundedCornerShape(14.dp), accentBorder = borderTint, modifier = Modifier.clickable { onSelectVoice(v.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                        Icon(
                            Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (v.gender == "Male") Color(0xFFF59E0B) else NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("${v.id} (${v.gender.first()})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(v.style, color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("COMPANION & HERO PERSONAS (${allPersonas.size})", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allPersonas, key = { it.id }) { persona ->
                val isSelected = settings.activePersonaName.equals(persona.name, ignoreCase = true)
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    accentBorder = if (isSelected) persona.accentColor else null,
                    modifier = Modifier.fillMaxWidth().clickable { onSelectPersona(persona) }
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(persona.accentColor.copy(alpha = 0.22f))
                            ) {
                                Icon(persona.icon, contentDescription = persona.name, tint = persona.accentColor, modifier = Modifier.size(18.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Text("LIVE", color = persona.accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (persona.isCustom) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp).clickable { onDeleteCustomPersona(persona.id) })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(persona.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(persona.role, color = persona.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(persona.tagline, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Voice: ${persona.voiceName} • ${persona.gender}", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomPersonaDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var selectedVoice by remember { mutableStateOf("Aoede") }
    var selectedHex by remember { mutableLongStateOf(0xFFEC4899L) }
    var prompt by remember {
        mutableStateOf("Tum user ki bahut sweet aur expressive Indian companion ho. Natural Hinglish mein chhote (1-2 lines) dil se nikle jawab do.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101226),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Create Custom Persona", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Female", "Male").forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = {
                                gender = g
                                selectedVoice = if (g == "Male") "Charon" else "Aoede"
                            },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Persona Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Vibe / Role") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Select Voice", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val voices = if (gender == "Male") SuzyCatalog.maleHeroVoices else SuzyCatalog.femaleVoices
                    voices.forEach { v ->
                        FilterChip(selected = selectedVoice == v.id, onClick = { selectedVoice = v.id }, label = { Text(v.id, fontSize = 11.sp) })
                    }
                }

                Text("Accent Color", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuzyCatalog.accentPresets.forEach { preset ->
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(preset.color)
                                .border(if (selectedHex == preset.hex) 2.5.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedHex = preset.hex }
                        )
                    }
                }

                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Speaking Style Prompt") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, role, selectedVoice, selectedHex, prompt, gender) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) { Text("SAVE & ACTIVATE", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        }
    )
}

@Composable
private fun MemoryTab(
    memories: List<MemoryItem>,
    isGuestMode: Boolean,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onTogglePin: (String) -> Unit
) {
    val categories = listOf("Profile", "Preference", "Routine", "Relationship", "Goals")
    var selectedCategory by remember { mutableStateOf("Profile") }
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter { it.content.contains(searchQuery, true) || it.category.contains(searchQuery, true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("PERMANENT MEMORY VAULT", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(
            text = if (isGuestMode) "GUEST SANDBOX ACTIVE — Private memories hidden during guest testing" else "Voice, Chat aur har screen mein AI ko yeh sab hamesha yaad rahega",
            color = if (isGuestMode) Color(0xFFF59E0B) else TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(shape = RoundedCornerShape(18.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    categories.forEach { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 11.sp) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("e.g. Mera naam Shahid hai...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (inputText.isNotBlank()) { onAddMemory(selectedCategory, inputText); inputText = "" } },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.height(54.dp)
                    ) { Icon(Icons.Rounded.Add, contentDescription = "Add") }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search saved memories (${memories.size})...", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isGuestMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Guest Mode ON hai — Owner Security screen se Guest Mode band karein.", color = Color(0xFFF59E0B), textAlign = TextAlign.Center)
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Abhi Memory Vault khaali hai.
Upar apni pasand ya baatein add karein!", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { mem ->
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        accentBorder = if (mem.pinned) NeonPink else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("${mem.category.uppercase()}  •  ${mem.createdAt}", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(mem.content, color = TextPrimary, fontSize = 13.sp)
                            }
                            Row {
                                IconButton(onClick = { onTogglePin(mem.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Rounded.PushPin, contentDescription = "Pin", tint = if (mem.pinned) NeonPink else TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDeleteMemory(mem.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
