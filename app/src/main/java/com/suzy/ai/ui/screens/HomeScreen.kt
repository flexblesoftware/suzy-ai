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
    onCheckUpdate: () -> Unit,
    onClearLogs: () -> Unit,
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
        NavTab("LOGS", Icons.Rounded.History),
        NavTab("TOOLS", Icons.Rounded.Build)
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
                        onCreateCustomPersona = onCreateCustomPersona,
                        onDeleteCustomPersona = onDeleteCustomPersona,
                        onSelectVoice = onSelectVoice
                    )
                    2 -> MemoryTab(
                        memories = memories,
                        onAddMemory = onAddMemory,
                        onDeleteMemory = onDeleteMemory
                    )
                    3 -> LogsTab(
                        uiState = uiState,
                        transcripts = transcripts,
                        onClearLogs = onClearLogs,
                        onClearTranscripts = onClearTranscripts
                    )
                    4 -> ToolsTab(
                        uiState = uiState,
                        updateStatus = updateStatus,
                        onToggleFlashlight = onToggleFlashlight,
                        onOpenApp = onOpenApp,
                        onCheckUpdate = onCheckUpdate,
                        onOpenSettings = onOpenSettings
                    )
                }
            }

            // Floating 5-Tab Glassmorphic Navigation Bar
            GlassCard(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                                .background(
                                    if (active) NeonPurple.copy(alpha = 0.3f) else Color.Transparent
                                )
                                .border(
                                    width = if (active) 1.dp else 0.dp,
                                    color = if (active) NeonCyan.copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = index }
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
        // 1. Glass Top Header
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
                    Text(
                        text = settings.activePersonaName.uppercase(),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

        // 2. Center ORB + Status Pill + Dedicated Non-Overlapping Transcript HUD
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

        // 3. Bottom Controls (Always cleanly separated below Transcript HUD)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PERSONAS & VOICES",
                    color = NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${allPersonas.size} Personas • ${SuzyCatalog.femaleVoices.size} Female Voices",
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
                                        text = "ACTIVE",
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (persona.isCustom) "Custom • ${persona.voiceName}" else "Voice: ${persona.voiceName}",
                            color = TextSecondary,
                            fontSize = 10.sp
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
                    label = { Text("Vibe / Role (e.g. Sweet Crush)") },
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
                    label = { Text("Personality & Speaking Style") },
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
                Text("SAVE PERSONA", fontWeight = FontWeight.Bold)
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
            text = "Jo yahan save karoge, Suzy ko har call par hamesha yaad rahega",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun LogsTab(
    uiState: SuzyUiState,
    transcripts: List<SavedTranscript>,
    onClearLogs: () -> Unit,
    onClearTranscripts: () -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("LIVE LOGS (${uiState.logs.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("TRANSCRIPTS (${transcripts.size})", fontSize = 11.sp) }
                )
            }

            IconButton(onClick = { if (subTab == 0) onClearLogs() else onClearTranscripts() }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subTab == 0) {
            if (uiState.logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Abhi koi live activity log nahi hai.", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.logs.reversed()) { log ->
                        GlassCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.source, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(log.time, color = TextSecondary, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.message, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            if (transcripts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Abhi koi saved call transcript nahi hai.\nCall end karne par poori baat yahan save ho jayegi.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(transcripts, key = { it.id }) { item ->
                        GlassCard(shape = RoundedCornerShape(16.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = item.dialogue, color = TextPrimary, fontSize = 12.sp, lineHeight = 18.sp)
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
    updateStatus: String,
    onToggleFlashlight: () -> Unit,
    onOpenApp: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CONTROL CENTER", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentBorder = if (uiState.isFlashlightOn) NeonCyan else null,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
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
                    .height(60.dp)
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

        Text("QUICK LAUNCH", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("YouTube", "WhatsApp", "Chrome").forEach { app ->
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable { onOpenApp(app) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(app, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        GlassCard(shape = RoundedCornerShape(20.dp), accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("APP UPDATES (v${BuildConfig.VERSION_NAME})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    }
}
