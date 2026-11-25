package com.mayank.questlist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY startMinutes ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY startMinutes ASC")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE enabled = 1 ORDER BY startMinutes ASC")
    fun getEnabledTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE enabled = 1 ORDER BY startMinutes ASC")
    suspend fun getEnabledTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdSync(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM task_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<TaskLog>>

    @Query("SELECT * FROM task_logs WHERE date = :date")
    suspend fun getLogsForDateSync(date: String): List<TaskLog>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND date = :date")
    suspend fun getLogForTaskAndDate(taskId: Long, date: String): TaskLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: TaskLog)

    @Query("DELETE FROM task_logs WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: Long)
}