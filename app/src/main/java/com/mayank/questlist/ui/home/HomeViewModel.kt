package com.mayank.questlist.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mayank.questlist.data.Task
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    val todayDate: String = TimeUtils.getTodayDateString()

    val uiState: StateFlow<HomeUiState> = repository.getTasksForDate(todayDate)
        .map { tasksWithLog ->
            val todayTasks = tasksWithLog.filter { TimeUtils.isScheduledForToday(it.task.daysOfWeekMask) }
            val completed = todayTasks.count { it.isCompleted }
            HomeUiState(
                isLoading = false,
                tasks = todayTasks,
                todayDate = todayDate,
                completedCount = completed,
                totalCount = todayTasks.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun toggleTaskCompletion(context: Context, taskId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, todayDate, !currentlyCompleted)
            val task = repository.getTaskByIdSync(taskId)
            task?.let { AlarmScheduler(context.applicationContext).scheduleNext(it) }
        }
    }

    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch {
            AlarmScheduler(context.applicationContext).cancel(task.id)
            repository.deleteTask(task)
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}