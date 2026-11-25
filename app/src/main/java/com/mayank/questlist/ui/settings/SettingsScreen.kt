package com.mayank.questlist.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mayank.questlist.overlay.OverlayService
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var warningMessage by remember { mutableStateOf("WARNING: Failure to complete the daily quest will result in a penalty.") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        ScanlineOverlay()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { HeaderBox(title = "SYSTEM CONFIG") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground)
                )
            },
            containerColor = VoidBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Message Settings
                GlowPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[ SYSTEM PENALTY WARNING ]",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAccent,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = warningMessage,
                        onValueChange = { warningMessage = it },
                        label = { Text("Penalty Warning Text") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrightAccent,
                            unfocusedBorderColor = MainGlow.copy(alpha = 0.4f),
                            focusedLabelColor = BrightAccent,
                            unfocusedLabelColor = TextSecondaryBlue,
                            focusedTextColor = TextBlueWhite,
                            unfocusedTextColor = TextBlueWhite,
                            cursorColor = BrightAccent
                        )
                    )
                }

                // System Actions
                GlowPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[ SYSTEM PERMISSIONS & BATTERY ]",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAccent,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowButton(
                        text = "MANAGE OVERLAY PERMISSION",
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowButton(
                        text = "BATTERY OPTIMIZATION",
                        onClick = {
                            context.startActivity(PermissionUtils.getBatteryOptimizationIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Debug Tools
                GlowPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[ DEBUG PROTOCOL ]",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAccent,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowButton(
                        text = "TRIGGER POPUP (10S)",
                        onClick = {
                            Toast.makeText(context, "Triggering test overlay in 10s...", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, OverlayService::class.java).apply {
                                putExtra(AlarmScheduler.EXTRA_TASK_ID, 9999L)
                            }
                            ContextCompat.startForegroundService(context, intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = StatusSuccess,
                        textColor = StatusSuccess
                    )
                }
            }
        }
    }
}