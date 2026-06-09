package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val playerId: String = "local_player",
    val totalCoins: Int = 1000,
    val lockedCoins: Int = 0,
    val winCoins: Int = 0,
    val lostCoins: Int = 0
)
