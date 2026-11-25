package com.mayank.questlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val streak: Int = 0
) {
    val maxXp: Int
        get() = level * 100

    val rank: String
        get() = when {
            level >= 25 -> "S-RANK"
            level >= 20 -> "A-RANK"
            level >= 15 -> "B-RANK"
            level >= 10 -> "C-RANK"
            level >= 5  -> "D-RANK"
            else        -> "E-RANK"
        }
}