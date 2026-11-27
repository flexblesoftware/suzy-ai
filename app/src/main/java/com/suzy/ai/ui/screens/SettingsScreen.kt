package com.suzy.ai.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassBackground
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsScreen(
    current: SuzySettings,
    onSave: (SuzySettings) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember(current) { mutableStateOf(current.apiKey) }
    var modelId by remember(current) { mutableStateOf(current.modelId) }
    var voiceName by remember(current) { mutableStateOf(current.voiceName) }
    var personaName by remember(current) { mutableStateOf(current.activePersonaName) }
    var customNote by remember(current) { mutableStateOf(current.customNote) }
    var bargeIn by remember(current) { mutableStateOf(current.bargeInEnabled) }
    var captions by remember(current) { mutableStateOf(current.captionsEnabled) }
    var temperature by remember(current) { mutableFloatStateOf(current.temperature) }
    var showKey by remember { mutableStateOf(false) }

    val modelPresets = listOf(
        "Gemini 3.8 Live" to "models/gemini-3.8-live",
        "Gemini 2.5 Live" to "models/gemini-2.5-flash-native-audio-preview-12-2025"
    )

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("AI Configuration", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Save karte hi live call mein turant apply hoga", color = NeonCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 1: API & Live Model
            Text("SECTION 1 • API & LIVE VOICE ENGINE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle API Key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Live Model Presets", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        modelPresets.forEach { (label, id) ->
                            FilterChip(
                                selected = modelId.equals(id, ignoreCase = true),
                                onClick = { modelId = id },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text("Model ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Persona, Voice & Personal Note (No raw hardcoded prompt wall)
            Text("SECTION 2 • PERSONA & VOICE TUNING", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Persona Switch (Hardcoded Profiles)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        SuzyCatalog.personas.forEach { p ->
                            FilterChip(
                                selected = personaName.equals(p.name, ignoreCase = true),
                                onClick = {
                                    personaName = p.name
                                    voiceName = p.voiceName
                                    temperature = p.temperature
                                },
                                label = { Text(p.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Female Voices (12 Styles)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        SuzyCatalog.femaleVoices.forEach { v ->
                            FilterChip(
                                selected = voiceName.equals(v.id, ignoreCase = true),
                                onClick = { voiceName = v.id },
                                label = { Text("${v.id} (${v.style})", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Extra Personal Note (Optional)") },
                        placeholder = { Text("e.g. Mujhe hamesha Jaan bulao...", fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Expressiveness (Temperature): ${String.format(Locale.US, "%.2f", temperature)}",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0.3f..1.4f)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: Call & HUD Controls
            Text("SECTION 3 • CALL & HUD CONTROLS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Live Interruption (Barge-In)", color = TextPrimary, fontSize = 14.sp)
                            Text("Bolte waqt beech mein rokne ki suvidha", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = bargeIn, onCheckedChange = { bargeIn = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Live Captions HUD", color = TextPrimary, fontSize = 14.sp)
                            Text("ORB ke neeche real-time transcript dikhayein", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = captions, onCheckedChange = { captions = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val cleanName = personaName.ifBlank { "Suzy" }
                    onSave(
                        SuzySettings(
                            apiKey = apiKey,
                            modelId = modelId,
                            voiceName = voiceName,
                            activePersonaName = cleanName,
                            activeMoodId = current.activeMoodId,
                            customNote = customNote,
                            systemPrompt = SuzyCatalog.resolveHardcodedPrompt(cleanName, current.systemPrompt),
                            bargeInEnabled = bargeIn,
                            captionsEnabled = captions,
                            temperature = temperature
                        )
                    )
                    onBack()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("SAVE & APPLY LIVE", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
