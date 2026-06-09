package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_rewards")
data class MatchReward(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: String,
    val difficulty: String,
    val coinsWon: Int,
    val matchId: String,
    val timestamp: Long = System.currentTimeMillis()
)
