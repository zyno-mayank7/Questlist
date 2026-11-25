package com.mayank.questlist.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_logs",
    indices = [Index(value = ["taskId", "date"], unique = true)]
)
data class TaskLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val date: String,               // yyyy-MM-dd
    val status: String,             // PENDING, COMPLETED, FAILED
    val completedAt: Long? = null   // epoch millis
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}