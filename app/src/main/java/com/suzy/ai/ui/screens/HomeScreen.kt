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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.suzy.ai.data.PersonaProfile
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassBackground
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.components.SuzyOrb
import com.suzy.ai.ui.theme.BgDark
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
    updateStatus: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSelectPersona: (PersonaProfile) -> Unit,
    onSelectVoice: (String) -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenApp: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onClearLogs: () -> Unit,
    onQuickReaction: (String) -> Unit,
    onInterruptClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavTab("ORB", Icons.Rounded.GraphicEq),
        NavTab("PERSONAS", Icons.Rounded.Face),
        NavTab("LOGS", Icons.Rounded.History),
        NavTab("TOOLS", Icons.Rounded.Build)
    )

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                when (selectedTab) {
                    0 -> OrbTab(
                        uiState = uiState,
                        settings = settings,
                        onStartClick = onStartClick,
                        onStopClick = onStopClick,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onQuickReaction = onQuickReaction,
                        onInterruptClick = onInterruptClick,
                        onOpenSettings = onOpenSettings
                    )
                    1 -> PersonasTab(
                        settings = settings,
                        onSelectPersona = {
                            onSelectPersona(it)
                            selectedTab = 0
                        },
                        onSelectVoice = onSelectVoice
                    )
                    2 -> LogsTab(uiState = uiState, onClearLogs = onClearLogs)
                    3 -> ToolsTab(
                        uiState = uiState,
                        updateStatus = updateStatus,
                        onToggleFlashlight = onToggleFlashlight,
                        onOpenApp = onOpenApp,
                        onCheckUpdate = onCheckUpdate,
                        onOpenSettings = onOpenSettings
                    )
                }
            }

            // Floating Glassmorphic Navigation Bar
            GlassCard(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    if (active) NeonPurple.copy(alpha = 0.28f) else Color.Transparent
                                )
                                .border(
                                    width = if (active) 1.dp else 0.dp,
                                    color = if (active) NeonCyan.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                    letterSpacing = 1.sp
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
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Glass Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentBorder = NeonPurple,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        text = settings.activePersonaName.uppercase(),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${settings.voiceName}  •  ${if (uiState.isSpeakerOn) "SPEAKER" else "EARPIECE"}",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassCard(
                    shape = CircleShape,
                    accentBorder = if (uiState.isSpeakerOn) NeonCyan else null
                ) {
                    IconButton(onClick = onToggleSpeaker, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = if (uiState.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.PhoneInTalk,
                            contentDescription = "Speaker Toggle",
                            tint = if (uiState.isSpeakerOn) NeonCyan else TextPrimary
                        )
                    }
                }

                GlassCard(shape = CircleShape) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                }
            }
        }

        // Center ORB + Glass Status Pill
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuzyOrb(state = uiState.state, amplitude = uiState.amplitude)
            Spacer(modifier = Modifier.height(20.dp))
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
                    text = uiState.statusText,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    emotions.forEach { item ->
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            accentBorder = item.tint,
                            modifier = Modifier.clickable { onQuickReaction(item.prompt) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            ) {
                                Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(item.label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                        IconButton(onClick = onToggleMute, modifier = Modifier.size(52.dp)) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = "Mute",
                                tint = if (uiState.isMuted) Color(0xFFEF4444) else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(58.dp)
                        .width(190.dp)
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
                            text = if (isActive) "STOP" else "START",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.8.sp
                        )
                    }
                }

                if (isActive) {
                    Spacer(modifier = Modifier.width(16.dp))
                    GlassCard(shape = CircleShape, accentBorder = NeonPink) {
                        IconButton(onClick = onInterruptClick, modifier = Modifier.size(52.dp)) {
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
    onSelectPersona: (PersonaProfile) -> Unit,
    onSelectVoice: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "GIRL PERSONAS & VOICES",
            color = NeonCyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        Text(
            text = "Har persona ka apna alag behavior aur voice style hai",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Female Voice Strip
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(SuzyCatalog.personas) { persona ->
                val isSelected = settings.activePersonaName.equals(persona.name, ignoreCase = true)
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    accentBorder = if (isSelected) persona.accentColor else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPersona(persona) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(persona.accentColor.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = persona.icon,
                                    contentDescription = persona.name,
                                    tint = persona.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    color = persona.accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = persona.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = persona.role,
                            color = persona.accentColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Voice: ${persona.voiceName}",
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
private fun LogsTab(uiState: SuzyUiState, onClearLogs: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE ACTIVITY LOGS", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            IconButton(onClick = onClearLogs) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Logs", tint = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Abhi koi activity log nahi hai.", color = TextSecondary, fontSize = 14.sp)
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
