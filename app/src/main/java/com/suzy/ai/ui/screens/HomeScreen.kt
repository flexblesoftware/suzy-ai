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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material.icons.rounded.SystemUpdate
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
import com.suzy.ai.BuildConfig
import com.suzy.ai.data.MemoryItem
import com.suzy.ai.data.MoodOption
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.SavedTranscript
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
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

private fun formatCallDuration(totalSeconds: Long): String {
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
    updateStatus: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCaptions: () -> Unit,
    onSelectMood: (MoodOption) -> Unit,
    onSelectPersona: (PersonaProfile) -> Unit,
    onCreateCustomPersona: (String, String, String, Long, String) -> Unit,
    onDeleteCustomPersona: (String) -> Unit,
    onSelectVoice: (String) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenApp: (String) -> Unit,
    onRefreshTelemetry: () -> Unit,
    onCheckUpdate: () -> Unit,
    onClearTranscripts: () -> Unit,
    onQuickReaction: (String) -> Unit,
    onInterruptClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavTab("ORB", Icons.Rounded.GraphicEq),
        NavTab("PERSONAS", Icons.Rounded.Face),
        NavTab("MEMORY", Icons.Rounded.Psychology),
        NavTab("TOOLS", Icons.Rounded.Build)
    )

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
            onRefreshTelemetry()
        }
    }

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
                        onQuickReaction = onQuickReaction,
                        onInterruptClick = onInterruptClick,
                        onOpenSettings = onOpenSettings
                    )
                    1 -> PersonasTab(
                        settings = settings,
                        customPersonas = customPersonas,
                        onSelectPersona = {
                            onSelectPersona(it)
                            selectedTab = 0
                        },
                        onCreateCustomPersona = { name, role, voice, colorHex, prompt ->
                            onCreateCustomPersona(name, role, voice, colorHex, prompt)
                            selectedTab = 0
                        },
                        onDeleteCustomPersona = onDeleteCustomPersona,
                        onSelectVoice = onSelectVoice
                    )
                    2 -> MemoryTab(
                        memories = memories,
                        onAddMemory = onAddMemory,
                        onDeleteMemory = onDeleteMemory
                    )
                    3 -> ToolsTab(
                        uiState = uiState,
                        transcripts = transcripts,
                        updateStatus = updateStatus,
                        onToggleFlashlight = onToggleFlashlight,
                        onOpenApp = onOpenApp,
                        onClearTranscripts = onClearTranscripts,
                        onCheckUpdate = onCheckUpdate,
                        onOpenSettings = onOpenSettings
                    )
                }
            }

            // Floating 4-Tab Glassmorphic Navigation Bar (LOGS completely removed)
            GlassCard(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val active = selectedTab == index
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (active) NeonPurple.copy(alpha = 0.32f) else Color.Transparent
                                )
                                .border(
                                    width = if (active) 1.dp else 0.dp,
                                    color = if (active) NeonCyan.copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 13.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (active) NeonCyan else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (active) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp
                                )
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
        EmotionChip("Tease", Icons.Rounded.AutoAwesome, NeonPurple, "Naughty aur playful tone mein mujhe thoda chidhao aur giggle karo!"),
        EmotionChip("Care", Icons.Rounded.Spa, Color(0xFF34D399), "Bahut pyaar se caring voice mein mera mood theek karo.")
    )

    val captionScrollState = rememberScrollState()
    val captionToShow = uiState.liveCaption.ifBlank { uiState.lastCaption }
    LaunchedEffect(captionToShow) {
        captionScrollState.animateScrollTo(captionScrollState.maxValue)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Section 1: Glass Top Header with Live Timer
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
                            Text(
                                text = formatCallDuration(uiState.callDurationSec),
                                color = NeonPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "${settings.voiceName} • ${if (uiState.isSpeakerOn) "SPEAKER" else "EAR"} • ${memoryCount} MEM",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassCard(
                    shape = CircleShape,
                    accentBorder = if (settings.captionsEnabled) NeonPurple else null
                ) {
                    IconButton(onClick = onToggleCaptions, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = if (settings.captionsEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                            contentDescription = "Live Captions",
                            tint = if (settings.captionsEnabled) NeonCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                GlassCard(
                    shape = CircleShape,
                    accentBorder = if (uiState.isSpeakerOn) NeonCyan else null
                ) {
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
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Section 2: Center 3D ORB + Status Pill + Non-Overlapping Transcript HUD
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SuzyOrb(
                state = uiState.state,
                amplitude = uiState.amplitude,
                orbSize = if (isActive && settings.captionsEnabled) 205.dp else 235.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                shape = RoundedCornerShape(50),
                accentBorder = when (uiState.state) {
                    SuzyState.SPEAKING -> NeonPink
                    SuzyState.LISTENING -> NeonCyan
                    SuzyState.ERROR -> Color(0xFFEF4444)
                    else -> NeonPurple
                }
            ) {
                Text(
                    text = if (isActive) "${uiState.statusText}  •  LOCK-SCREEN ACTIVE" else uiState.statusText,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (settings.captionsEnabled && isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    shape = RoundedCornerShape(16.dp),
                    accentBorder = if (uiState.state == SuzyState.SPEAKING) NeonPink else NeonPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.state == SuzyState.SPEAKING) NeonPink else NeonCyan)
                            )
                            Text(
                                text = "${settings.activePersonaName.uppercase()} • LIVE TRANSCRIPT",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 24.dp, max = 56.dp)
                                .verticalScroll(captionScrollState)
                        ) {
                            Text(
                                text = captionToShow.ifBlank { "Baat shuru karo, live transcript yahan dikhega..." },
                                color = if (captionToShow.isBlank()) TextSecondary else TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Bottom Mood Switcher + Quick Emotions + Call Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                SuzyCatalog.moods.forEach { mood ->
                    val selected = settings.activeMoodId == mood.id
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        accentBorder = if (selected) mood.color else null,
                        modifier = Modifier.clickable { onSelectMood(mood) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mood,
                                contentDescription = mood.label,
                                tint = if (selected) mood.color else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mood.label,
                                color = if (selected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (isActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    emotions.forEach { item ->
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            accentBorder = item.tint,
                            modifier = Modifier.clickable { onQuickReaction(item.prompt) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                    GlassCard(
                        shape = CircleShape,
                        accentBorder = if (uiState.isMuted) Color(0xFFEF4444) else NeonCyan
                    ) {
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
                            if (isActive) {
                                Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEC4899)))
                            } else {
                                Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                            }
                        )
                        .clickable { if (isActive) onStopClick() else onStartClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isActive) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "END CALL" else "START CALL",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.4.sp
                        )
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
    onCreateCustomPersona: (String, String, String, Long, String) -> Unit,
    onDeleteCustomPersona: (String) -> Unit,
    onSelectVoice: (String) -> Unit
) {
    var showBuilder by remember { mutableStateOf(false) }
    val allPersonas = customPersonas + SuzyCatalog.personas

    if (showBuilder) {
        CustomPersonaDialog(
            onDismiss = { showBuilder = false },
            onSave = { name, role, voice, colorHex, prompt ->
                onCreateCustomPersona(name, role, voice, colorHex, prompt)
                showBuilder = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Section 1: Header & Custom Persona Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PERSONAS & VOICE STUDIO",
                    color = NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "1-tap live switch • Har persona ka hardcoded alag behavior hai",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            GlassCard(
                shape = RoundedCornerShape(14.dp),
                accentBorder = NeonPink,
                modifier = Modifier.clickable { showBuilder = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create", tint = NeonPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CREATE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 2: 12 Female Voices Strip (Instant Live Switch)
        Text(
            text = "SECTION 1 • FEMALE VOICES (12)",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            SuzyCatalog.femaleVoices.forEach { v ->
                val selected = settings.voiceName.equals(v.id, ignoreCase = true)
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    accentBorder = if (selected) NeonCyan else null,
                    modifier = Modifier.clickable { onSelectVoice(v.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (selected) NeonCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(v.id, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(v.style, color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 3: Hardcoded & Custom Persona Cards (Clean Taglines, Zero Raw Prompt Clutter)
        Text(
            text = "SECTION 2 • GIRL COMPANION PERSONAS (${allPersonas.size})",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPersona(persona) }
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
                                    .background(persona.accentColor.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    imageVector = persona.icon,
                                    contentDescription = persona.name,
                                    tint = persona.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Text(
                                        text = "LIVE",
                                        color = persona.accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                if (persona.isCustom) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete Custom",
                                        tint = TextSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteCustomPersona(persona.id) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = persona.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = persona.role,
                            color = persona.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = persona.tagline,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Voice: ${persona.voiceName}",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomPersonaDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedVoice by remember { mutableStateOf("Aoede") }
    var selectedHex by remember { mutableLongStateOf(0xFFEC4899L) }
    var prompt by remember {
        mutableStateOf("Tum user ki bahut sweet aur expressive Indian girlfriend ho. Pyaari natural Hinglish mein chhote (1-2 lines) dil se nikle jawab do.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101226),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Custom Persona", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Persona Name (e.g. Aarohi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Vibe / Tagline (e.g. Sweet Crush)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Voice", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    SuzyCatalog.femaleVoices.forEach { v ->
                        FilterChip(
                            selected = selectedVoice == v.id,
                            onClick = { selectedVoice = v.id },
                            label = { Text(v.id, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Accent Color", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuzyCatalog.accentPresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .border(
                                    width = if (selectedHex == preset.hex) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedHex = preset.hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Custom Speaking Style") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, role, selectedVoice, selectedHex, prompt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("SAVE & ACTIVATE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun MemoryTab(
    memories: List<MemoryItem>,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    val categories = listOf("Profile", "Preference", "Routine", "Relationship")
    var selectedCategory by remember { mutableStateOf("Profile") }
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "PERSONAL MEMORY VAULT",
            color = NeonCyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Yahan save ki gayi baatein live call mein turant sync hoti hain",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Section 1: Add Memory Card
        Text(
            text = "SECTION 1 • ADD NEW MEMORY",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlassCard(shape = RoundedCornerShape(18.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
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
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddMemory(selectedCategory, inputText)
                                inputText = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Memory")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: Saved Memories List
        Text(
            text = "SECTION 2 • SAVED MEMORIES (${memories.size})",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Abhi Memory Vault khaali hai.\nApna naam, pasand ya routine upar add karein!",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(memories, key = { it.id }) { mem ->
                    GlassCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "${mem.category.uppercase()}  •  ${mem.createdAt}",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = mem.content, color = TextPrimary, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onDeleteMemory(mem.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Memory", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsTab(
    uiState: SuzyUiState,
    transcripts: List<SavedTranscript>,
    updateStatus: String,
    onToggleFlashlight: () -> Unit,
    onOpenApp: (String) -> Unit,
    onClearTranscripts: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Telemetry & Hardware Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONTROL CENTER", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

            GlassCard(shape = RoundedCornerShape(50), accentBorder = NeonCyan) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        contentDescription = "Battery",
                        tint = if (uiState.isCharging) Color(0xFF34D399) else NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.batteryPercent}%${if (uiState.isCharging) " CHG" else ""}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (uiState.toolFeedback.isNotBlank()) {
            GlassCard(shape = RoundedCornerShape(12.dp), accentBorder = NeonPink, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.toolFeedback,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Text("SECTION 1 • DEVICE & AI CONTROLS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentBorder = if (uiState.isFlashlightOn) NeonCyan else null,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clickable { onToggleFlashlight() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (uiState.isFlashlightOn) NeonCyan else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isFlashlightOn) "FLASH ON" else "FLASH OFF",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentBorder = NeonPurple,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clickable { onOpenSettings() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = NeonPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI CONFIG", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Section 2: Quick App Launcher
        Text("SECTION 2 • QUICK APP LAUNCHER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("YouTube", "WhatsApp", "Chrome").forEach { app ->
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable { onOpenApp(app) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Launch, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(app, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Instagram", "Telegram", "Settings").forEach { app ->
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable { onOpenApp(app) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Launch, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(app, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Section 3: Saved Call Transcripts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = NeonPink, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SECTION 3 • SAVED CALL HISTORY (${transcripts.size})",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (transcripts.isNotEmpty()) {
                IconButton(onClick = onClearTranscripts, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear History", tint = TextSecondary, modifier = Modifier.size(17.dp))
                }
            }
        }

        if (transcripts.isEmpty()) {
            GlassCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Call end karne par poori conversation history yahan save hogi.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            transcripts.take(5).forEach { item ->
                GlassCard(shape = RoundedCornerShape(16.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.personaName.uppercase()}  •  ${item.durationLabel}",
                                color = NeonPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = item.dateTime, color = TextSecondary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = item.dialogue,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Section 4: App Updates
        Text("SECTION 4 • SYSTEM & UPDATES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        GlassCard(shape = RoundedCornerShape(20.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Suzy AI v${BuildConfig.VERSION_NAME}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateStatus.ifBlank { "GitHub Releases se direct in-app update check karein." },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCheckUpdate,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CHECK FOR UPDATE", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
