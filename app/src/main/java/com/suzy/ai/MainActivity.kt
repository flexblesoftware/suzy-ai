package com.suzy.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suzy.ai.ui.components.UpdateDialog
import com.suzy.ai.ui.screens.HomeScreen
import com.suzy.ai.ui.screens.SettingsScreen
import com.suzy.ai.ui.theme.SuzyTheme
import com.suzy.ai.update.UpdateManager
import com.suzy.ai.util.PermissionHelper
import com.suzy.ai.viewmodel.SuzyViewModel

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)
    private var pauseTick by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onPause() {
        super.onPause()
        pauseTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuzyTheme {
                val vm: SuzyViewModel = viewModel()
                val uiState by vm.uiState.collectAsState()
                val settings by vm.settings.collectAsState()
                val memories by vm.memories.collectAsState()
                val customPersonas by vm.customPersonas.collectAsState()
                val transcripts by vm.transcripts.collectAsState()
                val chatMessages by vm.chatMessages.collectAsState()
                val tasks by vm.tasks.collectAsState()
                val financeEntries by vm.financeEntries.collectAsState()
                val habits by vm.habits.collectAsState()
                val notes by vm.notes.collectAsState()
                val rules by vm.rules.collectAsState()
                val securityLogs by vm.securityLogs.collectAsState()
                val parkingNote by vm.parkingNote.collectAsState()
                val medicalId by vm.medicalId.collectAsState()

                var showSettings by remember { mutableStateOf(false) }
                var initialStudioSection by remember { mutableIntStateOf(0) }

                val scope = rememberCoroutineScope()
                val updateManager = remember { UpdateManager(applicationContext, scope) }
                val updateState by updateManager.state.collectAsState()

                LaunchedEffect(resumeTick) {
                    vm.refreshGuardAndPermissions()
                    vm.refreshDeviceTelemetry()
                }

                LaunchedEffect(pauseTick) {
                    if (pauseTick > 0) {
                        vm.onAppMovedToBackground()
                    }
                }

                val multiPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    vm.refreshGuardAndPermissions()
                    if (settings.backgroundGuardEnabled) {
                        vm.syncBackgroundGuard(true)
                    }
                }

                val startCallMicLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    vm.refreshGuardAndPermissions()
                    if (granted) {
                        PermissionHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                        vm.startSession()
                    }
                }

                LaunchedEffect(Unit) {
                    multiPermissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                    updateManager.checkForUpdate(silent = true)
                }

                LaunchedEffect(settings.backgroundGuardEnabled) {
                    vm.syncBackgroundGuard(settings.backgroundGuardEnabled)
                }

                UpdateDialog(
                    state = updateState,
                    onDownloadClick = { updateManager.startDownload() },
                    onInstallClick = { updateManager.installApk() },
                    onDismiss = { updateManager.dismissDialog() }
                )

                if (showSettings) {
                    SettingsScreen(
                        current = settings,
                        initialStudioSection = initialStudioSection,
                        onSave = { vm.updateSettings(it) },
                        onBack = { showSettings = false }
                    )
                } else {
                    HomeScreen(
                        uiState = uiState,
                        settings = settings,
                        memories = memories,
                        customPersonas = customPersonas,
                        transcripts = transcripts,
                        chatMessages = chatMessages,
                        tasks = tasks,
                        financeEntries = financeEntries,
                        habits = habits,
                        notes = notes,
                        rules = rules,
                        securityLogs = securityLogs,
                        parkingNote = parkingNote,
                        medicalId = medicalId,
                        updateStatus = updateState.statusMessage,
                        onStartClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                PermissionHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                                vm.startSession()
                            } else {
                                startCallMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopClick = { vm.stopSession() },
                        onToggleMute = { vm.toggleMute() },
                        onToggleSpeaker = { vm.toggleSpeaker() },
                        onToggleCaptions = { vm.toggleCaptions() },
                        onSelectMood = { vm.selectMood(it) },
                        onSelectPersona = { vm.selectPersona(it) },
                        onCreateCustomPersona = { name, role, voice, colorHex, prompt, gender ->
                            vm.createCustomPersona(name, role, voice, colorHex, prompt, gender)
                        },
                        onDeleteCustomPersona = { vm.deleteCustomPersona(it) },
                        onSelectVoice = { vm.selectVoice(it) },
                        onAddMemory = { cat, text -> vm.addMemory(cat, text) },
                        onDeleteMemory = { vm.deleteMemory(it) },
                        onTogglePinMemory = { vm.togglePinMemory(it) },
                        onSendChatMessage = { msg, imgB64, lbl -> vm.sendChatMessage(msg, imgB64, lbl) },
                        onSpeakInGeminiVoice = { vm.speakTextInGeminiVoice(it) },
                        onClearChat = { vm.clearChatHistory() },
                        onUpdateSettings = { vm.updateSettings(it) },
                        onRunOmniCommand = { vm.executeOmniCommand(it, speakResponse = true) },
                        onConsumeNavRoute = { vm.consumeNavigationRoute() },
                        onAddTask = { vm.addPlannerTask(it) },
                        onToggleTask = { vm.togglePlannerTask(it) },
                        onAddFinance = { k, t, p, a -> vm.addFinanceEntry(k, t, p, a) },
                        onToggleFinanceSettled = { vm.toggleFinanceSettled(it) },
                        onToggleHabit = { vm.toggleHabit(it) },
                        onAddHabit = { vm.addHabit(it) },
                        onAddNote = { k, t, b -> vm.addNoteItem(k, t, b) },
                        onToggleNote = { vm.toggleNoteChecked(it) },
                        onAddRule = { k, tr, s, tg -> vm.addAutomationRule(k, tr, s, tg) },
                        onSaveParking = { vm.saveParkingSpot(it) },
                        onSaveMedical = { vm.saveMedicalId(it) },
                        onToggleWakeWord = { vm.toggleWakeWord(it) },
                        onToggleMediaShield = { vm.toggleMediaShield(it) },
                        onToggleCallPrivacyShield = { vm.toggleCallPrivacyShield(it) },
                        onToggleCallAnnouncer = { vm.toggleCallAnnouncer(it) },
                        onToggleNotificationReader = { vm.toggleNotificationReader(it) },
                        onTestCallAnnouncement = { vm.triggerTestCallAnnouncement() },
                        onTestNotificationAnnouncement = { vm.triggerTestNotificationAnnouncement() },
                        onToggleFlashlight = { vm.toggleFlashlightManual() },
                        onTestServerPing = { vm.testServerPing() },
                        onStartTimer = { vm.startCountdownTimer(it) },
                        onCancelTimer = { vm.cancelCountdownTimer() },
                        onCheckUpdate = { updateManager.checkForUpdate(silent = false) },
                        onQuickReaction = { vm.sendQuickReaction(it) },
                        onInterruptClick = { vm.interruptSuzy() },
                        onPrepareMicForVoiceprint = { vm.prepareMicForVoiceprintRecording(it) },
                        onYieldMicManually = { vm.yieldMicManuallyForExternalAi(30) },
                        onLogSecurityEvent = { c, s, r -> vm.logSecurityEventManual(c, s, r) },
                        onOpenSettings = { studioSec ->
                            initialStudioSection = studioSec
                            showSettings = true
                        }
                    )
                }
            }
        }
    }
}
