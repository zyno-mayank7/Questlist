package com.mayank.questlist.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mayank.questlist.data.PlayerStats
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.data.TaskWithLog
import com.mayank.questlist.overlay.OverlayService
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.PermissionUtils
import com.mayank.questlist.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    repository: TaskRepository,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onFixOverlayClick: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerStats by repository.getPlayerStats().collectAsState(initial = PlayerStats())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlayPermission by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
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
                topBar = {
                    TopAppBar(
                        title = { HeaderBox(title = "DAILY QUESTS") },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground)
                    )
                },
                bottomBar = {
                    BottomNavigationBar(
                        activeTab = 0,
                        onTabQuests = {},
                        onTabHistory = onNavigateHistory,
                        onTabSettings = onNavigateSettings
                    )
                },
                containerColor = VoidBackground
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Player Status Panel
                    PlayerStatusPanel(stats = playerStats)

                    Spacer(modifier = Modifier.height(12.dp))

                    // TEST POP-UP NOW Debug Button
                    GlowButton(
                        text = "TEST POP-UP NOW",
                        onClick = {
                            val canDraw = Settings.canDrawOverlays(context)
                            Log.d("QuestOverlay", "TEST POP-UP NOW tapped. canDrawOverlays = $canDraw")
                            if (!canDraw) {
                                Toast.makeText(
                                    context,
                                    "Overlay permission required to display pop-up over other apps.",
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Starting System Overlay Pop-Up...", Toast.LENGTH_SHORT).show()
                                val intent = Intent(context, OverlayService::class.java).apply {
                                    putExtra(AlarmScheduler.EXTRA_TASK_ID, 9999L)
                                }
                                ContextCompat.startForegroundService(context, intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!hasOverlayPermission) {
                        OverlayWarningBanner(onFixClick = onFixOverlayClick)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Clearance Bar & Add Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[ TODAY'S QUEST LOG ]",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccent,
                            letterSpacing = 1.5.sp,
                            style = TextStyle(shadow = TextGlowShadow)
                        )

                        GlowButton(
                            text = "+ NEW QUEST",
                            onClick = onAddTaskClick
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    QuestProgressCard(
                        completedCount = uiState.completedCount,
                        totalCount = uiState.totalCount
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrightAccent)
                        }
                    } else if (uiState.tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NO QUESTS ACTIVE FOR TODAY",
                                    fontFamily = OrbitronFontFamily,
                                    color = TextSecondaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap + NEW QUEST to initialize",
                                    fontFamily = RajdhaniFontFamily,
                                    color = TextMutedBlue,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(uiState.tasks, key = { it.task.id }) { taskWithLog ->
                                QuestItemCard(
                                    taskWithLog = taskWithLog,
                                    onToggleComplete = {
                                        viewModel.toggleTaskCompletion(
                                            context,
                                            taskWithLog.task.id,
                                            taskWithLog.isCompleted
                                        )
                                    },
                                    onCardClick = { onEditTaskClick(taskWithLog.task.id) },
                                    onDeleteClick = { viewModel.deleteTask(context, taskWithLog.task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerStatusPanel(stats: PlayerStats) {
    GlowPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "[ PLAYER STATUS ]",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryBlue,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stats.rank}  •  LVL ${stats.level}",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightAccent,
                    style = TextStyle(shadow = TextGlowShadow)
                )
            }

            Box(
                modifier = Modifier
                    .clip(CutCornerShape(6.dp))
                    .background(FaintBlueFill)
                    .border(1.dp, BrightAccent.copy(alpha = 0.5f), CutCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "STREAK: ${stats.streak} DAYS",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val xpProgress = if (stats.maxXp > 0) stats.xp.toFloat() / stats.maxXp else 0f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "XP PROGRESS",
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                color = TextSecondaryBlue
            )
            Text(
                text = "${stats.xp} / ${stats.maxXp} XP",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlueWhite
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { xpProgress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CutCornerShape(3.dp)),
            color = MainGlow,
            trackColor = PanelBackgroundSolid
        )
    }
}

@Composable
fun QuestProgressCard(
    completedCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    GlowPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLEARANCE",
                fontFamily = OrbitronFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryBlue,
                letterSpacing = 1.sp
            )
            Text(
                text = "$completedCount / $totalCount DONE",
                fontFamily = OrbitronFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrightAccent,
                style = TextStyle(shadow = TextGlowShadow)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CutCornerShape(3.dp)),
            color = StatusSuccess,
            trackColor = PanelBackgroundSolid
        )
    }
}

@Composable
fun QuestItemCard(
    taskWithLog: TaskWithLog,
    onToggleComplete: () -> Unit,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val task = taskWithLog.task
    val isCompleted = taskWithLog.isCompleted
    val isFailed = taskWithLog.isFailed

    val nowMin = TimeUtils.getCurrentMinutes()
    val isCrossMidnight = task.endMinutes < task.startMinutes
    val isInsideWindow = if (!isCrossMidnight) {
        nowMin in task.startMinutes until task.endMinutes
    } else {
        nowMin >= task.startMinutes || nowMin < task.endMinutes
    }
    val isActiveNow = !isCompleted && !isFailed && isInsideWindow

    val borderColor = when {
        isCompleted -> StatusSuccess.copy(alpha = 0.6f)
        isFailed -> StatusError.copy(alpha = 0.6f)
        isActiveNow -> BrightAccent
        else -> MainGlow.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(10.dp))
            .background(if (isCompleted) PanelBackground.copy(alpha = 0.4f) else PanelBackground)
            .border(1.dp, borderColor, CutCornerShape(10.dp))
            .clickable(onClick = onCardClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SystemCheckbox(
                checked = isCompleted,
                onCheckedChange = onToggleComplete
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) TextMutedBlue else TextBlueWhite,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    StatusBadge(
                        status = taskWithLog.status,
                        isActiveNow = isActiveNow
                    )
                }

                if (!task.goalText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GOAL: ${task.goalText}",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 13.sp,
                        color = if (isCompleted) TextMutedBlue else BrightAccent,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startTime = TimeUtils.formatMinutesToTime(task.startMinutes)
                    val endTime = TimeUtils.formatMinutesToTime(task.endMinutes)
                    Text(
                        text = "$startTime - $endTime  •  Every ${task.repeatIntervalMin}m",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondaryBlue
                    )
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Quest",
                    tint = TextMutedBlue
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, isActiveNow: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "ActivePulse")
    val pulseAlpha by if (isActiveNow) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ActivePulseAlpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val (bgColor, textColor, text) = when {
        status == TaskLog.STATUS_COMPLETED -> Triple(StatusSuccess.copy(alpha = 0.15f), StatusSuccess, "CLEARED")
        status == TaskLog.STATUS_FAILED -> Triple(StatusError.copy(alpha = 0.15f), StatusError, "FAILED")
        isActiveNow -> Triple(BrightAccent.copy(alpha = 0.25f * pulseAlpha), BrightAccent, "ACTIVE")
        else -> Triple(MainGlow.copy(alpha = 0.15f), TextSecondaryBlue, "PENDING")
    }

    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.6f * pulseAlpha), CutCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = OrbitronFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun OverlayWarningBanner(onFixClick: () -> Unit) {
    GlowPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFixClick),
        borderAlpha = 0.8f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OVERLAY PERMISSION REQUIRED",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusError,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Pop-ups will not trigger without overlay permission.",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = TextBlueWhite
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            GlowButton(
                text = "FIX",
                onClick = onFixClick,
                borderColor = StatusError,
                textColor = StatusError
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    activeTab: Int,
    onTabQuests: () -> Unit,
    onTabHistory: () -> Unit,
    onTabSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(PanelBackground)
            .border(1.dp, MainGlow.copy(alpha = 0.4f), CutCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                text = "QUESTS",
                isSelected = activeTab == 0,
                onClick = onTabQuests
            )
            BottomNavItem(
                text = "HISTORY",
                isSelected = activeTab == 1,
                onClick = onTabHistory
            )
            BottomNavItem(
                text = "SETTINGS",
                isSelected = activeTab == 2,
                onClick = onTabSettings
            )
        }
    }
}

@Composable
fun BottomNavItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(6.dp))
            .background(if (isSelected) FaintBlueFill else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) BrightAccent else Color.Transparent,
                CutCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontFamily = OrbitronFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) BrightAccent else TextSecondaryBlue,
            letterSpacing = 1.5.sp
        )
    }
}