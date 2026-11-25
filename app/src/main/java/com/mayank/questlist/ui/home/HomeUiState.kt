package com.mayank.questlist.ui.home

import com.mayank.questlist.data.TaskWithLog

data class HomeUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskWithLog> = emptyList(),
    val todayDate: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0
)