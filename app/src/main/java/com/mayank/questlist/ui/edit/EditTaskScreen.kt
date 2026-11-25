package com.mayank.questlist.ui.edit

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.overlay.QuestPopup
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    viewModel: EditTaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isSaved by viewModel.isSaved.collectAsState()

    var isVisible by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onNavigateBack()
        }
    }

    if (showPreviewDialog) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                QuestPopup(
                    taskTitle = viewModel.title.ifBlank { "Daily Workout" },
                    goalText = viewModel.goalText.ifBlank { null },
                    onComplete = { showPreviewDialog = false },
                    onSnooze = { showPreviewDialog = false },
                    onDismiss = { showPreviewDialog = false }
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            GlowPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "[ CONFIRM DELETE ]",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusError,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Are you sure you want to delete this quest from the system?",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    color = TextBlueWhite
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlowButton(
                        text = "CANCEL",
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlowButton(
                        text = "DELETE QUEST",
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteTask(context)
                        },
                        modifier = Modifier.weight(1f),
                        borderColor = StatusError,
                        textColor = StatusError
                    )
                }
            }
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
                        title = {
                            Text(
                                text = if (viewModel.isEditMode) "EDIT QUEST" else "ADD DAILY QUEST",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrightAccent,
                                letterSpacing = 1.5.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = BrightAccent
                                )
                            }
                        },
                        actions = {
                            if (viewModel.isEditMode) {
                                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Quest",
                                        tint = StatusError
                                    )
                                }
                            }
                        },
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
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    viewModel.errorMessage?.let { err ->
                        GlowPanel(
                            modifier = Modifier.fillMaxWidth(),
                            borderAlpha = 0.9f
                        ) {
                            Text(
                                text = err,
                                fontFamily = RajdhaniFontFamily,
                                color = StatusError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Panel 1: Quest Details
                    GlowPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[ QUEST DETAILS ]",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccent,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.title,
                            onValueChange = {
                                viewModel.title = it
                                viewModel.errorMessage = null
                            },
                            label = { Text("Quest Title (Required)") },
                            placeholder = { Text("e.g. Push-ups, Reading, Running") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = customSystemTextFieldColors(),
                            singleLine = true,
                            shape = CutCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.goalText,
                            onValueChange = { viewModel.goalText = it },
                            label = { Text("Goal Text (Optional)") },
                            placeholder = { Text("e.g. 100 reps, 20 pages") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = customSystemTextFieldColors(),
                            singleLine = true,
                            shape = CutCornerShape(8.dp)
                        )
                    }

                    // Panel 2: Quest Schedule
                    GlowPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[ QUEST SCHEDULE ]",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccent,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time Window Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimePickerBox(
                                label = "Start Time",
                                minutes = viewModel.startMinutes,
                                modifier = Modifier.weight(1f),
                                onPickTime = {
                                    val currentHour = viewModel.startMinutes / 60
                                    val currentMinute = viewModel.startMinutes % 60
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.startMinutes = hour * 60 + minute
                                            viewModel.errorMessage = null
                                        },
                                        currentHour,
                                        currentMinute,
                                        false
                                    ).show()
                                }
                            )

                            TimePickerBox(
                                label = "End Time",
                                minutes = viewModel.endMinutes,
                                modifier = Modifier.weight(1f),
                                onPickTime = {
                                    val currentHour = viewModel.endMinutes / 60
                                    val currentMinute = viewModel.endMinutes % 60
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.endMinutes = hour * 60 + minute
                                            viewModel.errorMessage = null
                                        },
                                        currentHour,
                                        currentMinute,
                                        false
                                    ).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Repeats pop-up every ${viewModel.repeatIntervalMin} min during active window",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondaryBlue
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val presetIntervals = listOf(5, 10, 15, 30, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetIntervals.forEach { interval ->
                                val isSelected = viewModel.repeatIntervalMin == interval
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CutCornerShape(6.dp))
                                        .background(if (isSelected) FaintBlueFill else PanelBackground)
                                        .border(
                                            1.dp,
                                            if (isSelected) BrightAccent else MainGlow.copy(alpha = 0.3f),
                                            CutCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.repeatIntervalMin = interval }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${interval}m",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrightAccent else TextSecondaryBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "REPEAT DAYS",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccent,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val dayLabels = TimeUtils.getDayLabels()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dayLabels.forEachIndexed { index, label ->
                                val isEnabled = TimeUtils.isDayEnabled(viewModel.daysOfWeekMask, index)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CutCornerShape(6.dp))
                                        .background(if (isEnabled) FaintBlueFill else PanelBackground)
                                        .border(
                                            1.dp,
                                            if (isEnabled) BrightAccent else MainGlow.copy(alpha = 0.2f),
                                            CutCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.toggleDay(index) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) BrightAccent else TextMutedBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(8.dp))
                                .background(PanelBackground)
                                .border(1.dp, MainGlow.copy(alpha = 0.4f), CutCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "QUEST ACTIVE",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlueWhite,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Enable pop-up reminders",
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondaryBlue
                                )
                            }

                            Switch(
                                checked = viewModel.enabled,
                                onCheckedChange = { viewModel.enabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VoidBackground,
                                    checkedTrackColor = BrightAccent,
                                    uncheckedThumbColor = TextMutedBlue,
                                    uncheckedTrackColor = PanelBackgroundSolid
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlowButton(
                            text = "PREVIEW POPUP",
                            onClick = { showPreviewDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        GlowButton(
                            text = "TEST (10S)",
                            onClick = {
                                viewModel.triggerDebugReminder(context)
                                Toast.makeText(context, "Test reminder scheduled in 10 seconds", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            borderColor = StatusSuccess,
                            textColor = StatusSuccess
                        )
                    }

                    GlowButton(
                        text = if (viewModel.isEditMode) "UPDATE QUEST" else "INITIALIZE QUEST",
                        onClick = { viewModel.saveTask(context) },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = true
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerBox(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onPickTime: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .background(PanelBackground)
            .border(1.dp, MainGlow.copy(alpha = 0.4f), CutCornerShape(8.dp))
            .clickable(onClick = onPickTime)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                color = TextSecondaryBlue,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = TimeUtils.formatMinutesToTime(minutes),
                fontFamily = OrbitronFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrightAccent
            )
        }
    }
}

@Composable
private fun customSystemTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrightAccent,
    unfocusedBorderColor = MainGlow.copy(alpha = 0.4f),
    focusedLabelColor = BrightAccent,
    unfocusedLabelColor = TextSecondaryBlue,
    focusedTextColor = TextBlueWhite,
    unfocusedTextColor = TextBlueWhite,
    cursorColor = BrightAccent
)