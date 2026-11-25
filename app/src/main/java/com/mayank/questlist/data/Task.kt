package com.mayank.questlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goalText: String? = null,
    val startMinutes: Int,          // minutes from midnight, e.g. 360 = 06:00
    val endMinutes: Int,            // e.g. 480 = 08:00
    val repeatIntervalMin: Int,     // pop-up repeat interval in minutes
    val daysOfWeekMask: Int = 127,  // bitmask Mon..Sun, 127 = every day
    val enabled: Boolean = true
)