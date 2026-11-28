package com.suzy.ai.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.suzy.ai.service.SuzyAccessibilityService
import com.suzy.ai.ui.components.GlassCard
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.ui.theme.TextPrimary
import com.suzy.ai.ui.theme.TextSecondary
import com.suzy.ai.util.PermissionAuditEntry
import com.suzy.ai.util.PermissionCatalogHelper

@Composable
fun PermissionManagerScreen(
    focusAccessibilityOnly: Boolean = false,
    onExecuteTestCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshCounter by remember { mutableIntStateOf(0) }
    val accConnected by SuzyAccessibilityService.isServiceConnected.collectAsState()
    var bankingGuard by remember { mutableStateOf(SuzyAccessibilityService.bankingSafetyGuardEnabled) }

    // Auto-refresh permissions whenever user returns from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsList = remember(refreshCounter, accConnected) {
        PermissionCatalogHelper.auditAllPermissions(context)
    }

    val grantedCount = permissionsList.count { it.isGranted }
    val totalCount = permissionsList.size.coerceAtLeast(1)
    val healthPercent = (grantedCount * 100) / totalCount

    val singleRuntimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshCounter++
    }

    val multiRuntimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshCounter++
    }

    fun requestOrOpen(item: PermissionAuditEntry) {
        if (item.isRuntime && item.runtimePermission != null) {
            singleRuntimeLauncher.launch(item.runtimePermission)
        } else {
            PermissionCatalogHelper.openSpecialPermissionScreen(context, item.id)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. Permission Health Score & One-Tap Fix All Missing Wizard
        if (!focusAccessibilityOnly) {
            GlassCard(
                accentBorder = if (healthPercent == 100) Color(0xFF34D399) else NeonCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PERMISSION HEALTH: $healthPercent% ($grantedCount/$totalCount GRANTED)",
                                color = if (healthPercent == 100) Color(0xFF34D399) else NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saari Runtime aur System Permissions ka live status",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { refreshCounter++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    LinearProgressIndicator(
                        progress = { grantedCount.toFloat() / totalCount.toFloat() },
                        color = if (healthPercent == 100) Color(0xFF34D399) else NeonPink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                    )

                    val missingRuntime = permissionsList
                        .filter { !it.isGranted && it.isRuntime && it.runtimePermission != null }
                        .mapNotNull { it.runtimePermission }
                        .toTypedArray()

                    val firstMissingSpecial = permissionsList.firstOrNull { !it.isGranted && !it.isRuntime }

                    if (missingRuntime.isNotEmpty() || firstMissingSpecial != null) {
                        Button(
                            onClick = {
                                if (missingRuntime.isNotEmpty()) {
                                    multiRuntimeLauncher.launch(missingRuntime)
                                } else if (firstMissingSpecial != null) {
                                    PermissionCatalogHelper.openSpecialPermissionScreen(context, firstMissingSpecial.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (missingRuntime.isNotEmpty())
                                    "GRANT ALL RUNTIME PERMISSIONS (${missingRuntime.size})"
                                else "FIX NEXT MISSING: ${firstMissingSpecial?.id?.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Suzy Accessibility Commander Card (Mobile Data, Hotspot, Wi-Fi, Lock Screen, Screenshot, Scroll)
        GlassCard(
            accentBorder = if (accConnected) Color(0xFF34D399) else Color(0xFFF59E0B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.AccessibilityNew,
                            contentDescription = null,
                            tint = if (accConnected) Color(0xFF34D399) else Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SUZY ACCESSIBILITY COMMANDER",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (accConnected) "CONNECTED • Mobile Data, Hotspot, Wi-Fi, Lock & Scroll Active"
                                else "DISCONNECTED • Direct Data/Hotspot/Lock ke liye ON karein",
                                color = if (accConnected) Color(0xFF34D399) else Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = { PermissionCatalogHelper.openSpecialPermissionScreen(context, "accessibility") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (accConnected) NeonCyan else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (accConnected) "MANAGE ACCESSIBILITY SERVICE (ACTIVE)" else "ENABLE SUZY ACCESSIBILITY SERVICE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("UPI & Banking App Safety Guard", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("GPay, PhonePe, Paytm khulne par auto-tap pause rahega", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                        checked = bankingGuard,
                        onCheckedChange = {
                            bankingGuard = it
                            SuzyAccessibilityService.bankingSafetyGuardEnabled = it
                        }
                    )
                }
            }
        }

        // 3. Live Hardware & Accessibility 1-Tap Test Console
        GlassCard(accentBorder = NeonPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE HARDWARE & COMMAND TEST CONSOLE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExecuteTestCommand("flashlight on karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.weight(1f)
                    ) { Text("TORCH ON", fontSize = 10.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onExecuteTestCommand("flashlight off karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) { Text("TORCH OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExecuteTestCommand("dnd on karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                        modifier = Modifier.weight(1f)
                    ) { Text("DND ON", fontSize = 10.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onExecuteTestCommand("dnd off karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) { Text("DND OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExecuteTestCommand("mobile data toggle karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) { Text("MOBILE DATA", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onExecuteTestCommand("hotspot toggle karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) { Text("HOTSPOT", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onExecuteTestCommand("wifi toggle karo") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) { Text("WI-FI", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExecuteTestCommand("screenshot lo") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) { Text("SCREENSHOT", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onExecuteTestCommand("lock screen") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f)
                    ) { Text("LOCK SCREEN", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 4. Complete Categorized List of ALL Permissions
        if (!focusAccessibilityOnly) {
            val grouped = permissionsList.groupBy { it.group }
            grouped.forEach { (groupTitle, items) ->
                Text(
                    text = groupTitle.uppercase(),
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                items.forEach { perm ->
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        accentBorder = if (perm.isGranted) Color(0xFF34D399) else Color(0xFFEF4444),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestOrOpen(perm) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Icon(
                                    imageVector = if (perm.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (perm.isGranted) Color(0xFF34D399) else Color(0xFFEF4444),
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(perm.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(perm.roleDescription, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (perm.isGranted) Color(0xFF34D399).copy(alpha = 0.2f)
                                        else Color(0xFFEF4444)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (perm.isGranted) "GRANTED" else "GRANT",
                                    color = if (perm.isGranted) Color(0xFF34D399) else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
