package com.suzy.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.suzy.ai.viewmodel.SuzyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuzyTheme {
                val vm: SuzyViewModel = viewModel()
                val uiState by vm.uiState.collectAsState()
                val settings by vm.settings.collectAsState()
                var showSettings by remember { mutableStateOf(false) }

                val scope = rememberCoroutineScope()
                val updateManager = remember { UpdateManager(applicationContext, scope) }
                val updateState by updateManager.state.collectAsState()

                val notifLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    updateManager.checkForUpdate(silent = true)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        vm.startSession()
                    }
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
                        updateStatus = updateState.statusMessage,
                        onStartClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                vm.startSession()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopClick = { vm.stopSession() },
                        onToggleMute = { vm.toggleMute() },
                        onToggleSpeaker = { vm.toggleSpeaker() },
                        onSelectPersona = { vm.selectPersona(it) },
                        onSelectVoice = { vm.selectVoice(it) },
                        onToggleFlashlight = { vm.toggleFlashlightManual() },
                        onOpenApp = { vm.openAppManual(it) },
                        onCheckUpdate = { updateManager.checkForUpdate(silent = false) },
                        onClearLogs = { vm.clearLogs() },
                        onQuickReaction = { vm.sendQuickReaction(it) },
                        onInterruptClick = { vm.interruptSuzy() },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
