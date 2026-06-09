package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE playerId = :playerId LIMIT 1")
    fun getWallet(playerId: String = "local_player"): Flow<Wallet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    @Query("UPDATE wallet SET totalCoins = totalCoins + :amount, winCoins = winCoins + :amount WHERE playerId = :playerId")
    suspend fun addWinCoins(playerId: String = "local_player", amount: Int)

    @Query("UPDATE wallet SET totalCoins = totalCoins - :amount, lostCoins = lostCoins + :amount WHERE playerId = :playerId")
    suspend fun addLostCoins(playerId: String = "local_player", amount: Int)
}

@Dao
interface MatchRewardDao {
    @Query("SELECT * FROM match_rewards ORDER BY timestamp DESC")
    fun getAllRewards(): Flow<List<MatchReward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: MatchReward)
}

@Database(entities = [Wallet::class, MatchReward::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun matchRewardDao(): MatchRewardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "draft_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
