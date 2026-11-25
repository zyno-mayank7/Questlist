package com.mayank.questlist.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.home.StatusBadge
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: TaskRepository,
    onNavigateBack: () -> Unit
) {
    val todayDate = remember { TimeUtils.getTodayDateString() }
    val tasksWithLog by repository.getTasksForDate(todayDate).collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        ScanlineOverlay()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { HeaderBox(title = "SYSTEM HISTORY") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground)
                )
            },
            containerColor = VoidBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlowPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[ QUEST LOG ARCHIVE ]",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAccent,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recorded Clearance & System Events for $todayDate",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondaryBlue
                    )
                }

                if (tasksWithLog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO HISTORICAL LOGS RECORDED YET",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 14.sp,
                            color = TextSecondaryBlue,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tasksWithLog, key = { it.task.id }) { item ->
                            GlowPanel(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.task.title,
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextBlueWhite
                                        )
                                        item.task.goalText?.let { goal ->
                                            Text(
                                                text = "GOAL: $goal",
                                                fontFamily = RajdhaniFontFamily,
                                                fontSize = 13.sp,
                                                color = TextSecondaryBlue
                                            )
                                        }
                                    }

                                    StatusBadge(status = item.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}