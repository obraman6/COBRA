package com.example

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class LeaderboardEntry(
    val playerId: String = "",
    val playerName: String = "",
    val wins: Int = 0,
    val winStreak: Int = 0,
    val score: Int = 0 
)

class LeaderboardManager {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("leaderboard")

    private val _globalLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val globalLeaderboard: StateFlow<List<LeaderboardEntry>> = _globalLeaderboard.asStateFlow()
    
    // For local leaderboard, let's track the top players that have similar scores or just the player's history, but often local leaderboard can just be caching or filtering.
    // We'll provide a way to load the global leaderboard:
    suspend fun loadGlobalLeaderboard() {
        try {
            val snapshot = collection
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val entries = snapshot.toObjects(LeaderboardEntry::class.java)
            _globalLeaderboard.value = entries
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePlayerStats(playerName: String, isWin: Boolean) {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val playerId = user?.uid ?: return
            
            val docRef = collection.document(playerId)
            val snapshot = docRef.get().await()
            var currentEntry = snapshot.toObject(LeaderboardEntry::class.java)
            
            if (currentEntry == null) {
                currentEntry = LeaderboardEntry(playerId = playerId, playerName = playerName)
            }

            val newWinStreak = if (isWin) currentEntry.winStreak + 1 else 0
            val newWins = if (isWin) currentEntry.wins + 1 else currentEntry.wins
            val scoreChange = if (isWin) 10 + newWinStreak else -5
            val newScore = maxOf(0, currentEntry.score + scoreChange)

            val updatedEntry = currentEntry.copy(
                playerName = playerName,
                wins = newWins,
                winStreak = newWinStreak,
                score = newScore
            )
            docRef.set(updatedEntry).await()
            
            loadGlobalLeaderboard()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
