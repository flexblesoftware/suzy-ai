package com.suzy.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.suzy.ai.audio.VoiceVerificationResult
import com.suzy.ai.audio.VoiceprintEngine
import com.suzy.ai.data.SecurityAuditItem
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.sin

private data class EnrollmentPromptSlot(
    val slotIndex: Int,
    val title: String,
    val phraseToSpeak: String
)

@Composable
fun OwnerVoiceIdScreen(
    settings: SuzySettings,
    securityLogs: List<SecurityAuditItem>,
    isExternalMicYielded: Boolean,
    externalMicReason: String,
    onUpdateSettings: (SuzySettings) -> Unit,
    onPrepareMicForVoiceprint: (Boolean) -> Unit,
    onYieldMicManually: () -> Unit,
    onLogSecurityEvent: (String, String, String) -> Unit,
    onRunCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sample1Done by remember { mutableStateOf(VoiceprintEngine.isSampleRecorded(context, 1)) }
    var sample2Done by remember { mutableStateOf(VoiceprintEngine.isSampleRecorded(context, 2)) }
    var sample3Done by remember { mutableStateOf(VoiceprintEngine.isSampleRecorded(context, 3)) }

    var recordingSlot by remember { mutableIntStateOf(0) } // 1..3 for enroll, 99 for live verify
    var playingSlot by remember { mutableIntStateOf(0) }
    var liveAmp by remember { mutableFloatStateOf(0f) }
    var liveDb by remember { mutableFloatStateOf(-60f) }
    var recordProgress by remember { mutableFloatStateOf(0f) }
    var verifyResult by remember { mutableStateOf<VoiceVerificationResult?>(null) }

    var ownerNameEdit by remember(settings.ownerName) { mutableStateOf(settings.ownerName) }
    var ownerTitleEdit by remember(settings.ownerNickname) { mutableStateOf(settings.ownerNickname) }
    var passphraseEdit by remember(settings.ownerPassphrase) { mutableStateOf(settings.ownerPassphrase) }
    var pinEdit by remember(settings.ownerPin) { mutableStateOf(settings.ownerPin) }
    var thresholdEdit by remember(settings.ownerVoiceMatchThreshold) { mutableIntStateOf(settings.ownerVoiceMatchThreshold) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission zaroori hai", Toast.LENGTH_SHORT).show()
        }
    }

    val slots = listOf(
        EnrollmentPromptSlot(1, "Sample 1 • Wake Identity", "\"Hey Suzy, verify my owner voice\""),
        EnrollmentPromptSlot(2, "Sample 2 • Command Auth", "\"Authorize system and security controls\""),
        EnrollmentPromptSlot(3, "Sample 3 • Master Passphrase", "\"This is ${settings.ownerName}, ${settings.ownerPassphrase}\"")
    )

    val enrolledCount = listOf(sample1Done, sample2Done, sample3Done).count { it }

    fun startRealRecording(slotIndex: Int) {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (recordingSlot != 0) return

        recordingSlot = slotIndex
        recordProgress = 0f
        liveAmp = 0f
        onPrepareMicForVoiceprint(true)

        scope.launch {
            val res = VoiceprintEngine.recordVoiceSample(
                context = context,
                slotIndex = if (slotIndex in 1..3) slotIndex else null,
                durationMs = 3500L,
                onLiveTelemetry = { amp, db, prog ->
                    liveAmp = amp
                    liveDb = db
                    recordProgress = prog
                }
            )
            recordingSlot = 0
            liveAmp = 0f
            recordProgress = 0f
            onPrepareMicForVoiceprint(false)

            res.fold(
                onSuccess = { _ ->
                    if (slotIndex in 1..3) {
                        sample1Done = VoiceprintEngine.isSampleRecorded(context, 1)
                        sample2Done = VoiceprintEngine.isSampleRecorded(context, 2)
                        sample3Done = VoiceprintEngine.isSampleRecorded(context, 3)

                        val master = VoiceprintEngine.computeMasterSignature(context)
                        if (master != null) {
                            onUpdateSettings(
                                settings.copy(
                                    ownerVoiceEnrolled = true,
                                    ownerVoicePitchHz = master.pitchHz,
                                    ownerVoiceSpreadHz = master.pitchSpreadHz,
                                    ownerVoiceZcr = master.zeroCrossingRate,
                                    ownerVoiceLowBand = master.lowBandRatio,
                                    ownerVoiceMidBand = master.midBandRatio,
                                    ownerVoiceHighBand = master.highBandRatio
                                )
                            )
                        }
                        Toast.makeText(context, "Sample $slotIndex Recorded & Analyzed!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Live Voice Verification Check
                        val master = VoiceprintEngine.computeMasterSignature(context)
                        if (master == null) {
                            Toast.makeText(context, "Pehle kam se kam 1 voice sample record karein", Toast.LENGTH_SHORT).show()
                        } else {
                            val liveFeat = VoiceprintEngine.recordVoiceSample(context, null, 100L) { _, _, _ -> }.getOrNull()
                            // Use the features we just extracted!
                            val outcome = VoiceprintEngine.compareWithOwner(
                                enrolled = master,
                                live = res.getOrThrow(),
                                thresholdPercent = thresholdEdit
                            )
                            verifyResult = outcome
                            if (!outcome.isMatch) {
                                onLogSecurityEvent("Live Voice ID Test", "MISMATCH", outcome.reason)
                            }
                        }
                    }
                },
                onFailure = { err ->
                    Toast.makeText(context, err.message ?: "Recording failed", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. Gemini / Claude External App Mic Coexistence Fix Card
        GlassCard(
            accentBorder = if (isExternalMicYielded) Color(0xFF34D399) else NeonCyan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("GEMINI & CLAUDE MIC SAFE-YIELD", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isExternalMicYielded) "Status: $externalMicReason" else "Auto-releases mic when Gemini, Claude or WhatsApp opens",
                                color = if (isExternalMicYielded) Color(0xFF34D399) else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Switch(
                        checked = settings.externalMicCoexistenceEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(externalMicCoexistenceEnabled = it)) }
                    )
                }

                Button(
                    onClick = onYieldMicManually,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RELEASE MIC FOR GEMINI / CLAUDE NOW (30s)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Real Acoustic Voiceprint Studio (3 WAV Samples + Live Waveform + Playback)
        GlassCard(accentBorder = NeonPink, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = NeonPink, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("REAL ACOUSTIC VOICEPRINT STUDIO", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (enrolledCount > 0 && settings.ownerVoiceEnrolled)
                                    "Enrolled: $enrolledCount/3 Samples • Pitch: ${settings.ownerVoicePitchHz.toInt()} Hz"
                                else "0/3 Samples Recorded — Neeche record karein",
                                color = if (enrolledCount > 0) Color(0xFF34D399) else Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Live Waveform Visualizer Bar when Recording
                if (recordingSlot != 0) {
                    GlassCard(accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(
                                    text = if (recordingSlot == 99) "LISTENING FOR LIVE VERIFICATION..." else "RECORDING SAMPLE $recordingSlot...",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${liveDb.toInt()} dB", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (bar in 0 until 22) {
                                    val waveFactor = (sin(bar * 0.65 + recordProgress * 18.0).toFloat() * 0.35f + 0.65f)
                                    val barHeight by animateFloatAsState(
                                        targetValue = (6f + (liveAmp * 26f * waveFactor)).coerceIn(5f, 32f),
                                        label = "wf_$bar"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (bar % 2 == 0) NeonCyan else NeonPink)
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { recordProgress },
                                color = NeonPink,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                // 3 Individual Enrollment Sample Cards
                slots.forEach { slot ->
                    val isDone = when (slot.slotIndex) {
                        1 -> sample1Done
                        2 -> sample2Done
                        else -> sample3Done
                    }
                    val isThisRecording = recordingSlot == slot.slotIndex
                    val isThisPlaying = playingSlot == slot.slotIndex

                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        accentBorder = when {
                            isThisRecording -> NeonPink
                            isDone -> Color(0xFF34D399)
                            else -> NeonPurple
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Mic,
                                            contentDescription = null,
                                            tint = if (isDone) Color(0xFF34D399) else NeonCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(slot.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Bolkar record karein: ${slot.phraseToSpeak}", color = TextSecondary, fontSize = 10.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isDone) {
                                        IconButton(
                                            onClick = {
                                                if (playingSlot == 0 && recordingSlot == 0) {
                                                    playingSlot = slot.slotIndex
                                                    scope.launch {
                                                        VoiceprintEngine.playSample(context, slot.slotIndex) {
                                                            playingSlot = 0
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                contentDescription = "Play Sample",
                                                tint = if (isThisPlaying) NeonPink else NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                VoiceprintEngine.deleteSample(context, slot.slotIndex)
                                                sample1Done = VoiceprintEngine.isSampleRecorded(context, 1)
                                                sample2Done = VoiceprintEngine.isSampleRecorded(context, 2)
                                                sample3Done = VoiceprintEngine.isSampleRecorded(context, 3)
                                                val anyLeft = sample1Done || sample2Done || sample3Done
                                                onUpdateSettings(settings.copy(ownerVoiceEnrolled = anyLeft))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Sample", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isThisRecording) NeonPink else NeonPurple)
                                            .clickable { startRealRecording(slot.slotIndex) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isThisRecording) "REC..." else if (isDone) "RE-REC" else "RECORD",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Acoustic Timbre Metrics Summary
                if (enrolledCount > 0) {
                    Text(
                        text = "Vocal Signature: Pitch ${settings.ownerVoicePitchHz.toInt()}Hz | Bass ${(settings.ownerVoiceLowBand * 100).toInt()}% | Mid ${(settings.ownerVoiceMidBand * 100).toInt()}% | Air ${(settings.ownerVoiceHighBand * 100).toInt()}%",
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }

                // Live Speaker Verification Tester
                Button(
                    onClick = { startRealRecording(99) },
                    enabled = enrolledCount > 0 && recordingSlot == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST MY VOICE LIVE (3.5s VERIFICATION)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                verifyResult?.let { vr ->
                    GlassCard(
                        accentBorder = if (vr.isMatch) Color(0xFF34D399) else Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (vr.isMatch) Icons.Rounded.VerifiedUser else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (vr.isMatch) Color(0xFF34D399) else Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (vr.isMatch) "OWNER VERIFIED • ${vr.matchPercent}% MATCH" else "INTRUDER ALERT • ${vr.matchPercent}% MATCH",
                                    color = if (vr.isMatch) Color(0xFF34D399) else Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(vr.reason, color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Owner Profile, Passphrase, PIN & Strict Gate Setup
        GlassCard(accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OWNER CREDENTIALS & STRICT LOCK", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ownerNameEdit,
                        onValueChange = { ownerNameEdit = it },
                        label = { Text("Owner Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ownerTitleEdit,
                        onValueChange = { ownerTitleEdit = it },
                        label = { Text("Boss Title", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = passphraseEdit,
                        onValueChange = { passphraseEdit = it },
                        label = { Text("Voice Passphrase", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pinEdit,
                        onValueChange = { pinEdit = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Master PIN", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Voice Match Strictness Threshold: ${thresholdEdit}%", color = NeonCyan, fontSize = 11.sp)
                Slider(
                    value = thresholdEdit.toFloat(),
                    onValueChange = { thresholdEdit = it.toInt() },
                    valueRange = 60f..95f
                )

                Button(
                    onClick = {
                        onUpdateSettings(
                            settings.copy(
                                ownerName = ownerNameEdit.ifBlank { "Shahid" },
                                ownerNickname = ownerTitleEdit.ifBlank { "Boss" },
                                ownerPassphrase = passphraseEdit.ifBlank { "omega" },
                                ownerPin = pinEdit.ifBlank { "1234" },
                                ownerVoiceMatchThreshold = thresholdEdit
                            )
                        )
                        Toast.makeText(context, "Owner Identity & Threshold Saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE OWNER CREDENTIALS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Strict Owner-Only Command Lock", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Sensitive commands sirf Owner Voiceprint ya Passphrase se chalenge", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(checked = settings.ownerModeStrict, onCheckedChange = { onUpdateSettings(settings.copy(ownerModeStrict = it)) })
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Guest / Demo Sandbox Mode", color = TextPrimary, fontSize = 13.sp)
                        Text("Private memories & personal data hide karein", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(checked = settings.guestModeActive, onCheckedChange = { onUpdateSettings(settings.copy(guestModeActive = it)) })
                }

                Button(
                    onClick = { onRunCommand("stealth mode") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ACTIVATE STEALTH PANIC LOCK", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // 4. Security Audit Log
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SECURITY AUDIT LOG (${securityLogs.size})", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        if (securityLogs.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Zero unauthorized command or voice mismatch attempts recorded.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            }
        } else {
            securityLogs.take(10).forEach { log ->
                GlassCard(accentBorder = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${log.status} • ${log.timestamp}", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${log.command} — ${log.reason}", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
