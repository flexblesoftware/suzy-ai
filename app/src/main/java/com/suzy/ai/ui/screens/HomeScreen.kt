package com.suzy.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.BuildConfig
import com.suzy.ai.ui.components.SuzyOrb
import com.suzy.ai.ui.theme.BgDark
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.SurfaceGlass
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

@Composable
fun HomeScreen(
    uiState: SuzyUiState,
    updateStatus: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenApp: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onClearLogs: () -> Unit,
    onQuickReaction: (String) -> Unit,
    onInterruptClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            NavigationBar(containerColor = SurfaceGlass) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = "Orb") },
                    label = { Text("ORB") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.History, contentDescription = "Logs") },
                    label = { Text("LOGS") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Build, contentDescription = "Tools") },
                    label = { Text("TOOLS") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A0B16), BgDark, Color(0xFF0C0918))
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            when (selectedTab) {
                0 -> OrbTab(
                    uiState = uiState,
                    onStartClick = onStartClick,
                    onStopClick = onStopClick,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onQuickReaction = onQuickReaction,
                    onInterruptClick = onInterruptClick,
                    onOpenSettings = onOpenSettings
                )
                1 -> LogsTab(uiState = uiState, onClearLogs = onClearLogs)
                2 -> ToolsTab(
                    uiState = uiState,
                    updateStatus = updateStatus,
                    onToggleFlashlight = onToggleFlashlight,
                    onOpenApp = onOpenApp,
                    onCheckUpdate = onCheckUpdate,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun OrbTab(
    uiState: SuzyUiState,
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
        EmotionChip("Love", Icons.Rounded.Favorite, NeonPink, "Pyaar se soft romantic voice mein ek sweet si line bolo jaise girlfriend bolti hai."),
        EmotionChip("Tease", Icons.Rounded.AutoAwesome, NeonPurple, "Naughty aur playful tone mein mujhe thoda chidhao aur giggle karo!"),
        EmotionChip("Care", Icons.Rounded.Spa, Color(0xFF34D399), "Bahut pyaar se caring voice mein mera mood theek karo, bolo main hoon na.")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SUZY", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(
                    text = if (uiState.isSpeakerOn) "LOUDSPEAKER MODE" else "EARPIECE MODE",
                    color = if (uiState.isSpeakerOn) NeonCyan else NeonPurple,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(1.dp, if (uiState.isSpeakerOn) NeonCyan.copy(alpha = 0.8f) else NeonPurple.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.PhoneInTalk,
                        contentDescription = "Speaker Toggle",
                        tint = if (uiState.isSpeakerOn) NeonCyan else TextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuzyOrb(state = uiState.state, amplitude = uiState.amplitude)
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                color = SurfaceGlass.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = when (uiState.state) {
                        SuzyState.SPEAKING -> NeonPink.copy(alpha = 0.6f)
                        SuzyState.LISTENING -> NeonCyan.copy(alpha = 0.6f)
                        SuzyState.ERROR -> Color(0xFFEF4444).copy(alpha = 0.7f)
                        else -> NeonPurple.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = uiState.statusText,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    emotions.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceGlass.copy(alpha = 0.9f))
                                .border(1.dp, item.tint.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable { onQuickReaction(item.prompt) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, if (uiState.isMuted) Color(0xFFEF4444) else NeonCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = "Mute",
                            tint = if (uiState.isMuted) Color(0xFFEF4444) else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = { if (isActive) onStopClick() else onStartClick() },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color(0xFFDC2626) else NeonPurple),
                    modifier = Modifier.height(58.dp).width(185.dp)
                ) {
                    Icon(if (isActive) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isActive) "STOP" else "START", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.5.sp)
                }

                if (isActive) {
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onInterruptClick,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, NeonPink.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Interrupt", tint = NeonPink)
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
            Text("JARVIS ACTIVITY LOGS", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            IconButton(onClick = onClearLogs) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Logs", tint = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Abhi koi activity log nahi hai.", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.logs.reversed()) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
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
        Text("JARVIS CONTROL CENTER", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onToggleFlashlight,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isFlashlightOn) NeonCyan else SurfaceGlass),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = "Flashlight",
                    tint = if (uiState.isFlashlightOn) BgDark else TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isFlashlightOn) "FLASH ON" else "FLASH OFF",
                    color = if (uiState.isFlashlightOn) BgDark else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlass),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI CONFIG", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Text("QUICK LAUNCH", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("YouTube", "WhatsApp", "Chrome").forEach { app ->
                Button(
                    onClick = { onOpenApp(app) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlass),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(app, color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
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
                shape = RoundedCornerShape(12.dp),
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
