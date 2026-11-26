package com.suzy.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.SurfaceGlass
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import com.suzy.ai.update.UpdateState

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.updateAvailable) return

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(250),
        label = "update_progress"
    )

    Dialog(onDismissRequest = { if (!state.isDownloading) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceGlass)
                .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = "Update",
                tint = NeonCyan,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Suzy v${state.latestVersion} Available",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.statusMessage,
                color = TextSecondary,
                fontSize = 13.sp
            )

            if (state.isDownloading || state.isReadyToInstall) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = NeonCyan,
                    trackColor = Color(0xFF1E2238),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (!state.isDownloading) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("LATER", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (state.isReadyToInstall) onInstallClick() else onDownloadClick()
                    },
                    enabled = !state.isDownloading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Text(
                        text = when {
                            state.isReadyToInstall -> "INSTALL NOW"
                            state.isDownloading -> "${(animatedProgress * 100).toInt()}%"
                            else -> "UPDATE NOW"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
