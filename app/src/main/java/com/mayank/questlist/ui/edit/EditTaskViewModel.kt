package com.mayank.questlist.ui.edit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mayank.questlist.data.Task
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditTaskViewModel(
    private val repository: TaskRepository,
    val taskId: Long
) : ViewModel() {

    var title by mutableStateOf("")
    var goalText by mutableStateOf("")
    var startMinutes by mutableIntStateOf(360) // Default 06:00 AM
    var endMinutes by mutableIntStateOf(480)   // Default 08:00 AM
    var repeatIntervalMin by mutableIntStateOf(10) // Default 10 min
    var daysOfWeekMask by mutableIntStateOf(TimeUtils.ALL_DAYS_MASK) // Default 127
    var enabled by mutableStateOf(true)

    var isEditMode by mutableStateOf(taskId > 0)
        private set

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    var errorMessage by mutableStateOf<String?>(null)

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                val task = repository.getTaskByIdSync(taskId)
                task?.let {
                    title = it.title
                    goalText = it.goalText ?: ""
                    startMinutes = it.startMinutes
                    endMinutes = it.endMinutes
                    repeatIntervalMin = it.repeatIntervalMin
                    daysOfWeekMask = it.daysOfWeekMask
                    enabled = it.enabled
                }
            }
        }
    }

    fun toggleDay(dayIndex: Int) {
        val bit = TimeUtils.dayIndexToMask(dayIndex)
        daysOfWeekMask = if ((daysOfWeekMask and bit) != 0) {
            daysOfWeekMask and bit.inv()
        } else {
            daysOfWeekMask or bit
        }
    }

    fun saveTask(context: Context) {
        if (title.isBlank()) {
            errorMessage = "Quest title is required."
            return
        }
        if (startMinutes >= endMinutes) {
            errorMessage = "End time must be after start time."
            return
        }
        if (repeatIntervalMin <= 0) {
            errorMessage = "Repeat interval must be greater than 0 minutes."
            return
        }

        viewModelScope.launch {
            val task = Task(
                id = if (taskId > 0) taskId else 0,
                title = title.trim(),
                goalText = goalText.trim().ifEmpty { null },
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                repeatIntervalMin = repeatIntervalMin,
                daysOfWeekMask = daysOfWeekMask,
                enabled = enabled
            )

            val insertedId = if (isEditMode) {
                repository.updateTask(task)
                task.id
            } else {
                repository.insertTask(task)
            }

            val savedTask = task.copy(id = insertedId)
            val scheduler = AlarmScheduler(context.applicationContext)

            if (savedTask.enabled) {
                scheduler.scheduleNext(savedTask)
            } else {
                scheduler.cancel(savedTask.id)
            }

            _isSaved.value = true
        }
    }

    fun deleteTask(context: Context) {
        if (taskId > 0) {
            viewModelScope.launch {
                val task = repository.getTaskByIdSync(taskId)
                task?.let {
                    AlarmScheduler(context.applicationContext).cancel(it.id)
                    repository.deleteTask(it)
                }
                _isSaved.value = true
            }
        }
    }

    fun triggerDebugReminder(context: Context) {
        val tempTask = Task(
            id = if (taskId > 0) taskId else 9999,
            title = title.ifBlank { "Test Quest" },
            goalText = goalText.ifBlank { "Complete your test quest!" },
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            repeatIntervalMin = repeatIntervalMin,
            daysOfWeekMask = daysOfWeekMask,
            enabled = true
        )
        AlarmScheduler(context.applicationContext).scheduleDebugReminder(tempTask, delaySeconds = 10)
    }

    class Factory(
        private val repository: TaskRepository,
        private val taskId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditTaskViewModel::class.java)) {
                return EditTaskViewModel(repository, taskId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}