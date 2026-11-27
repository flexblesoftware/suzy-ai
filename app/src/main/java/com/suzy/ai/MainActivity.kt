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

    override fun onResume() {
        super.onResume()
        resumeTick++
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
                var showSettings by remember { mutableStateOf(false) }

                val scope = rememberCoroutineScope()
                val updateManager = remember { UpdateManager(applicationContext, scope) }
                val updateState by updateManager.state.collectAsState()

                LaunchedEffect(resumeTick) {
                    vm.refreshGuardAndPermissions()
                    vm.refreshDeviceTelemetry()
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
                        onCreateCustomPersona = { name, role, voice, colorHex, prompt ->
                            vm.createCustomPersona(name, role, voice, colorHex, prompt)
                        },
                        onDeleteCustomPersona = { vm.deleteCustomPersona(it) },
                        onSelectVoice = { vm.selectVoice(it) },
                        onAddMemory = { cat, text -> vm.addMemory(cat, text) },
                        onDeleteMemory = { vm.deleteMemory(it) },
                        onToggleCallAnnouncer = { vm.toggleCallAnnouncer(it) },
                        onToggleAnnounceCallerName = { vm.toggleAnnounceCallerName(it) },
                        onSetAnnouncerRepeat = { vm.setAnnouncerRepeat(it) },
                        onSetAnnouncerStyle = { vm.setAnnouncerStyle(it) },
                        onToggleNotificationReader = { vm.toggleNotificationReader(it) },
                        onToggleReadNotificationContent = { vm.toggleReadNotificationContent(it) },
                        onToggleBackgroundGuard = { vm.toggleBackgroundGuard(it) },
                        onTestCallAnnouncement = { vm.triggerTestCallAnnouncement() },
                        onTestNotificationAnnouncement = { vm.triggerTestNotificationAnnouncement() },
                        onRequestAllPermissions = {
                            multiPermissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                        },
                        onRequestNotificationListener = {
                            PermissionHelper.openNotificationListenerSettings(this@MainActivity)
                        },
                        onRequestBatteryExemption = {
                            PermissionHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                        },
                        onRefreshGuardAndPermissions = { vm.refreshGuardAndPermissions() },
                        onToggleFlashlight = { vm.toggleFlashlightManual() },
                        onTestServerPing = { vm.testServerPing() },
                        onStartTimer = { vm.startCountdownTimer(it) },
                        onCancelTimer = { vm.cancelCountdownTimer() },
                        onRefreshTelemetry = { vm.refreshDeviceTelemetry() },
                        onCheckUpdate = { updateManager.checkForUpdate(silent = false) },
                        onClearTranscripts = { vm.clearTranscripts() },
                        onQuickReaction = { vm.sendQuickReaction(it) },
                        onInterruptClick = { vm.interruptSuzy() },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
