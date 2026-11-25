package com.mayank.questlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerStats(stats: PlayerStats)
}