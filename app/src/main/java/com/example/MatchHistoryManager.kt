package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class MatchHistoryEntry(
    val timestamp: Long = 0L,
    val opponentName: String = "",
    val isWin: Boolean = false,
    val gameMode: String = ""
)

class MatchHistoryManager {
    private val db = FirebaseDatabase.getInstance().reference

    private val _history = MutableStateFlow<List<MatchHistoryEntry>>(emptyList())
    val history: StateFlow<List<MatchHistoryEntry>> = _history.asStateFlow()

    suspend fun loadHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val snapshot = db.child("users").child(uid).child("matchHistory").get().await()
            val list = mutableListOf<MatchHistoryEntry>()
            for (child in snapshot.children) {
                val entry = child.getValue(MatchHistoryEntry::class.java)
                if (entry != null) {
                    list.add(entry)
                }
            }
            _history.value = list.sortedByDescending { it.timestamp }.take(10)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addMatchRecord(opponentName: String, isWin: Boolean, gameMode: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val newEntry = MatchHistoryEntry(
                timestamp = System.currentTimeMillis(),
                opponentName = opponentName,
                isWin = isWin,
                gameMode = gameMode
            )
            val ref = db.child("users").child(uid).child("matchHistory").push()
            ref.setValue(newEntry).await()
            
            // Reload history to reflect correctly across sessions (or we can just prepend)
            val snapshot = db.child("users").child(uid).child("matchHistory").get().await()
            val list = mutableListOf<MatchHistoryEntry>()
            for (child in snapshot.children) {
                val entry = child.getValue(MatchHistoryEntry::class.java)
                if (entry != null) {
                    list.add(entry)
                }
            }
            _history.value = list.sortedByDescending { it.timestamp }.take(10)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
