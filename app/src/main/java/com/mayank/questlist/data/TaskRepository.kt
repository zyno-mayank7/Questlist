package com.mayank.questlist.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TaskRepository(
    private val taskDao: TaskDao,
    private val playerStatsDao: PlayerStatsDao
) {

    fun getTasksForDate(date: String): Flow<List<TaskWithLog>> {
        return combine(taskDao.getAllTasks(), taskDao.getLogsForDate(date)) { tasks, logs ->
            val logMap = logs.associateBy { it.taskId }
            tasks.map { task ->
                TaskWithLog(task = task, log = logMap[task.id])
            }
        }
    }

    fun getPlayerStats(): Flow<PlayerStats> {
        return playerStatsDao.getPlayerStats().map { it ?: PlayerStats() }
    }

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun getTaskByIdSync(id: Long): Task? = taskDao.getTaskByIdSync(id)

    suspend fun getAllTasksSync(): List<Task> = taskDao.getAllTasksSync()

    suspend fun getEnabledTasksSync(): List<Task> = taskDao.getEnabledTasksSync()

    suspend fun getLogForTaskAndDate(taskId: Long, date: String): TaskLog? =
        taskDao.getLogForTaskAndDate(taskId, date)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) {
        taskDao.deleteLogsForTask(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun setTaskStatus(taskId: Long, date: String, status: String, completedAt: Long? = null) {
        val existingLog = taskDao.getLogForTaskAndDate(taskId, date)
        val updatedLog = existingLog?.copy(
            status = status,
            completedAt = completedAt
        ) ?: TaskLog(
            taskId = taskId,
            date = date,
            status = status,
            completedAt = completedAt
        )
        taskDao.upsertLog(updatedLog)
    }

    suspend fun toggleTaskCompletion(taskId: Long, date: String, isCompleted: Boolean) {
        val newStatus = if (isCompleted) TaskLog.STATUS_COMPLETED else TaskLog.STATUS_PENDING
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        setTaskStatus(taskId, date, newStatus, completedAt)

        // Award/Deduct XP and streak
        val stats = playerStatsDao.getPlayerStatsSync() ?: PlayerStats()
        if (isCompleted) {
            val xpGain = 50
            var newXp = stats.xp + xpGain
            var newLvl = stats.level
            while (newXp >= newLvl * 100) {
                newXp -= newLvl * 100
                newLvl++
            }
            playerStatsDao.upsertPlayerStats(
                stats.copy(
                    level = newLvl,
                    xp = newXp,
                    streak = stats.streak + 1
                )
            )
        } else {
            val newXp = (stats.xp - 50).coerceAtLeast(0)
            playerStatsDao.upsertPlayerStats(
                stats.copy(
                    xp = newXp,
                    streak = (stats.streak - 1).coerceAtLeast(0)
                )
            )
        }
    }
}