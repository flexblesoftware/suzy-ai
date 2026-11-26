package com.suzy.ai.ui.screens

import androidx.compose.foundation.background
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
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.theme.BgDark
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    current: SuzySettings,
    onSave: (SuzySettings) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember(current) { mutableStateOf(current.apiKey) }
    var modelId by remember(current) { mutableStateOf(current.modelId) }
    var voiceName by remember(current) { mutableStateOf(current.voiceName) }
    var systemPrompt by remember(current) { mutableStateOf(current.systemPrompt) }
    var bargeIn by remember(current) { mutableStateOf(current.bargeInEnabled) }
    var temperature by remember(current) { mutableFloatStateOf(current.temperature) }
    var showKey by remember { mutableStateOf(false) }

    val modelPresets = listOf(
        "Gemini 2.5 Live" to "models/gemini-2.5-flash-native-audio-preview-12-2025",
        "Gemini 3.8 Live" to "models/gemini-3.8-live"
    )
    val voices = listOf("Aoede", "Kore", "Leda", "Zephyr", "Puck")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Suzy Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        Text("Live Model Presets", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            modelPresets.forEach { (label, id) ->
                FilterChip(
                    selected = modelId.equals(id, ignoreCase = true),
                    onClick = { modelId = id },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelId,
            onValueChange = { modelId = it },
            label = { Text("Custom Model ID (Gemini Live)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Voice Name", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            voices.forEach { v ->
                FilterChip(
                    selected = voiceName.equals(v, ignoreCase = true),
                    onClick = { voiceName = v },
                    label = { Text(v, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = voiceName,
            onValueChange = { voiceName = it },
            label = { Text("Custom Voice Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text("Suzy Persona (System Instruction)") },
            minLines = 4,
            maxLines = 7,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Live Interruption (Barge-In)", color = TextPrimary, fontSize = 15.sp)
                Text("Bolte waqt beech mein rokne ki suvidha", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(checked = bargeIn, onCheckedChange = { bargeIn = it })
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Creativity (Temperature): ${String.format("%.2f", temperature)}",
            color = TextPrimary,
            fontSize = 14.sp
        )
        Slider(
            value = temperature,
            onValueChange = { temperature = it },
            valueRange = 0.2f..1.5f
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                onSave(
                    SuzySettings(
                        apiKey = apiKey,
                        modelId = modelId,
                        voiceName = voiceName,
                        systemPrompt = systemPrompt,
                        bargeInEnabled = bargeIn,
                        temperature = temperature
                    )
                )
                onBack()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("SAVE SETTINGS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
