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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        EmotionChip(
            label = "Laugh",
            icon = Icons.Rounded.SentimentVerySatisfied,
            tint = NeonCyan,
            prompt = "Zor se khul kar hanso aur koi cute funny si line bolo Hinglish mein!"
        ),
        EmotionChip(
            label = "Love",
            icon = Icons.Rounded.Favorite,
            tint = NeonPink,
            prompt = "Pyaar se soft romantic voice mein ek sweet si line bolo jaise girlfriend bolti hai."
        ),
        EmotionChip(
            label = "Tease",
            icon = Icons.Rounded.AutoAwesome,
            tint = NeonPurple,
            prompt = "Naughty aur playful tone mein mujhe thoda chidhao aur giggle karo!"
        ),
        EmotionChip(
            label = "Care",
            icon = Icons.Rounded.Spa,
            tint = Color(0xFF34D399),
            prompt = "Bahut pyaar se caring voice mein mera mood theek karo, bolo main hoon na."
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0B16), BgDark, Color(0xFF0C0918))
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SUZY",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(
                            1.dp,
                            if (uiState.isSpeakerOn) NeonCyan.copy(alpha = 0.8f) else NeonPurple.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (uiState.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.PhoneInTalk,
                        contentDescription = if (uiState.isSpeakerOn) "Loudspeaker On" else "Earpiece Mode",
                        tint = if (uiState.isSpeakerOn) NeonCyan else TextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Center ORB + Status
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuzyOrb(
                state = uiState.state,
                amplitude = uiState.amplitude
            )

            Spacer(modifier = Modifier.height(22.dp))

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

            if (uiState.liveCaption.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.liveCaption,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Bottom Controls + Clean Vector Emotion Triggers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    emotions.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceGlass.copy(alpha = 0.9f))
                                .border(1.dp, item.tint.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable { onQuickReaction(item.prompt) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = item.tint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.label,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(
                                1.dp,
                                if (uiState.isMuted) Color(0xFFEF4444) else NeonCyan.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = "Mute",
                            tint = if (uiState.isMuted) Color(0xFFEF4444) else TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))
                }

                Button(
                    onClick = { if (isActive) onStopClick() else onStartClick() },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFFDC2626) else NeonPurple
                    ),
                    modifier = Modifier
                        .height(60.dp)
                        .width(190.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew,
                        contentDescription = if (isActive) "Stop" else "Start",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isActive) "STOP" else "START",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                if (isActive) {
                    Spacer(modifier = Modifier.width(18.dp))
                    IconButton(
                        onClick = onInterruptClick,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, NeonPink.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Interrupt",
                            tint = NeonPink
                        )
                    }
                }
            }
        }
    }
}
