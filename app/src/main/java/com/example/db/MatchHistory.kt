package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opponentName: String,
    val isWin: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val gameMode: String
)
