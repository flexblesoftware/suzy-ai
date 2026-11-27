package com.suzy.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suzy.ai.data.ChatMessageItem
import com.suzy.ai.data.SuzySettings
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary

@Composable
fun ChatScreen(
    settings: SuzySettings,
    messages: List<ChatMessageItem>,
    isSending: Boolean,
    onSendMessage: (text: String, imageBase64: String?, attachmentLabel: String) -> Unit,
    onSpeakInGeminiVoice: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var attachedImageB64 by remember { mutableStateOf<String?>(null) }
    var attachedLabel by remember { mutableStateOf("") }
    var isDictating by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.lastIndex)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 4_000_000) {
                    attachedImageB64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    attachedLabel = "Image Attached (${bytes.size / 1024} KB)"
                } else {
                    Toast.makeText(context, "Image 4MB se chhoti chunein", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
            }
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(Unit) {
        onDispose {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
        }
    }

    val quickPrompts = listOf(
        "Boss briefing do",
        "Vikram hero persona lagao",
        "ARC Reactor ORB lagao",
        "25 min focus timer lagao",
        "Aaj ka hisab batao"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${settings.activePersonaName.uppercase()} • AI CHAT & COMMANDS",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Voice-enabled chat • App ke saare commands yahin se bhi follow honge",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassCard(shape = CircleShape, accentBorder = if (showSearch) NeonCyan else null) {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search Chat", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
                if (messages.isNotEmpty()) {
                    GlassCard(shape = CircleShape) {
                        IconButton(onClick = onClearChat, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Chat", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (showSearch) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Command & Prompt Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            quickPrompts.forEach { qp ->
                GlassCard(
                    shape = RoundedCornerShape(50),
                    accentBorder = NeonPurple,
                    modifier = Modifier.clickable {
                        onSendMessage(qp, null, "")
                    }
                ) {
                    Text(
                        text = qp,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(shape = RoundedCornerShape(22.dp), accentBorder = NeonPurple, modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NeonPink, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${settings.activePersonaName} Ready in Chat & Command Mode",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Koi bhi sawaal poochho, photo attach karo, ya seedha command likho jaise:
• "Vikram South Hero persona lagao"
• "Brightness 40% karo aur 15 min timer lagao"
• "Rahul ke 500 rupaye udhaar mein likho"",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(filteredMessages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        GlassCard(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            ),
                            accentBorder = if (isUser) NeonCyan else NeonPink,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isUser) settings.ownerName.uppercase() else msg.personaName.uppercase(),
                                        color = if (isUser) NeonCyan else NeonPink,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(text = msg.timestamp, color = TextSecondary, fontSize = 9.sp)
                                }

                                if (msg.attachmentLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "[${msg.attachmentLabel}]", color = NeonCyan, fontSize = 10.sp)
                                }

                                if (msg.commandExecuted.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚡ Executed: ${msg.commandExecuted}",
                                        color = Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                if (!isUser) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { onSpeakInGeminiVoice(msg.text) }
                                        ) {
                                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Speak", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("SuzyReply", msg.text))
                                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Copy", color = TextSecondary, fontSize = 10.sp)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, msg.text)
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Share", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isSending) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeonPink)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${settings.activePersonaName} likh rahi hai...", color = NeonPink, fontSize = 11.sp)
            }
        }

        if (attachedLabel.isNotBlank()) {
            GlassCard(shape = RoundedCornerShape(12.dp), accentBorder = NeonCyan, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(attachedLabel, color = NeonCyan, fontSize = 11.sp)
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp).clickable {
                            attachedImageB64 = null
                            attachedLabel = ""
                        }
                    )
                }
            }
        }

        // Bottom Composer Bar (+ Attachment, Input Field, Mic Dictation, Send)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassCard(shape = CircleShape, accentBorder = NeonPurple) {
                IconButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Attach Image", tint = NeonCyan)
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message ya command likho...", fontSize = 12.sp) },
                maxLines = 3,
                modifier = Modifier.weight(1f)
            )

            GlassCard(
                shape = CircleShape,
                accentBorder = if (isDictating) NeonPink else NeonCyan
            ) {
                IconButton(
                    onClick = {
                        if (speechRecognizer == null) {
                            Toast.makeText(context, "Speech dictation unavailable", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        if (isDictating) {
                            speechRecognizer.stopListening()
                            isDictating = false
                        } else {
                            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) { isDictating = true }
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() { isDictating = false }
                                override fun onError(error: Int) { isDictating = false }
                                override fun onResults(results: Bundle?) {
                                    isDictating = false
                                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                                    if (spoken.isNotBlank()) {
                                        inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
                                    }
                                }
                                override fun onPartialResults(partialResults: Bundle?) {}
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                            }
                            speechRecognizer.startListening(intent)
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Dictate",
                        tint = if (isDictating) NeonPink else TextPrimary
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)))
                    .clickable {
                        val msg = inputText.trim()
                        if (msg.isNotBlank() || attachedImageB64 != null) {
                            onSendMessage(msg.ifBlank { "Is photo ko dekho aur batao" }, attachedImageB64, attachedLabel)
                            inputText = ""
                            attachedImageB64 = null
                            attachedLabel = ""
                        }
                    }
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
