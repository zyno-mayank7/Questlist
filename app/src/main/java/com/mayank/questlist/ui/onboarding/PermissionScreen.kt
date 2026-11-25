package com.mayank.questlist.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel,
    onGetStarted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        ScanlineOverlay()

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(200))
        ) {
            Scaffold(
                containerColor = VoidBackground
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    HeaderBox(title = "SYSTEM INITIALIZATION")

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Grant required permissions to enable Daily Quest pop-ups over other applications.",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp,
                            color = TextSecondaryBlue,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Permission Cards

                    // 1. Overlay Permission (Required)
                    PermissionCard(
                        title = "Display Over Other Apps",
                        subtitle = "Required for glowing pop-up reminders over other apps",
                        isRequired = true,
                        isGranted = viewModel.hasOverlay,
                        onAction = {
                            context.startActivity(PermissionUtils.getOverlayPermissionIntent(context))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Notifications Permission (Required)
                    PermissionCard(
                        title = "Notifications",
                        subtitle = "Required for status alerts and background service notice",
                        isRequired = true,
                        isGranted = viewModel.hasNotification,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(PermissionUtils.getNotificationPermissionIntent(context))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Exact Alarms (Recommended)
                    PermissionCard(
                        title = "Exact Alarms",
                        subtitle = "Recommended for precise pop-up timing during windows",
                        isRequired = false,
                        isGranted = viewModel.hasExactAlarm,
                        onAction = {
                            context.startActivity(PermissionUtils.getExactAlarmPermissionIntent(context))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Battery Optimization (Recommended)
                    PermissionCard(
                        title = "Battery Unrestricted",
                        subtitle = "Recommended to prevent OS from delaying background alarms",
                        isRequired = false,
                        isGranted = viewModel.isBatteryOptimizedExempt,
                        onAction = {
                            context.startActivity(PermissionUtils.getBatteryOptimizationIntent(context))
                        }
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    if (!viewModel.canProceed) {
                        Text(
                            text = "* Grant both Overlay & Notification permissions to activate system",
                            fontFamily = RajdhaniFontFamily,
                            color = StatusError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    GlowButton(
                        text = "INITIALIZE SYSTEM",
                        onClick = {
                            if (viewModel.canProceed) {
                                viewModel.completeOnboarding()
                                onGetStarted()
                            }
                        },
                        enabled = viewModel.canProceed,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    subtitle: String,
    isRequired: Boolean,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    val borderColor = if (isGranted) StatusSuccess.copy(alpha = 0.5f) else if (isRequired) StatusError.copy(alpha = 0.5f) else BrightAccent.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(10.dp))
            .background(PanelBackground)
            .border(1.dp, borderColor, CutCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CutCornerShape(6.dp))
                    .background(
                        if (isGranted) StatusSuccess.copy(alpha = 0.15f)
                        else if (isRequired) StatusError.copy(alpha = 0.15f)
                        else BrightAccent.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) StatusSuccess else if (isRequired) StatusError else BrightAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlueWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isRequired) {
                        Text(
                            text = "[REQUIRED]",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) StatusSuccess else StatusError
                        )
                    } else {
                        Text(
                            text = "[OPTIONAL]",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondaryBlue,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            GlowButton(
                text = if (isGranted) "GRANTED" else "GRANT",
                onClick = onAction,
                enabled = !isGranted,
                isPrimary = !isGranted && isRequired,
                borderColor = if (isGranted) StatusSuccess else if (isRequired) StatusError else BrightAccent,
                textColor = if (isGranted) StatusSuccess else if (isRequired) StatusError else BrightAccent
            )
        }
    }
}