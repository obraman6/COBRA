package com.example

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class RoomState(
    val roomId: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val hostName: String = "",
    val guestName: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED
    val lastMove: String = "",
    val turn: Player = Player.WHITE,
    val lastEmote: String = ""
)

class OnlineMatchManager {

    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Tenganisha (Disconnected)")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var currentRoomListener: ValueEventListener? = null
    var myPlayerId: String = "player_${System.currentTimeMillis()}"

    suspend fun createRoom(playerName: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val uniqueId = (1..6).map { chars.random() }.joinToString("")
        val room = RoomState(
            roomId = uniqueId,
            hostId = myPlayerId,
            hostName = playerName
        )
        db.child("rooms").child(uniqueId).setValue(room).await()
        db.child("rooms").child(uniqueId).onDisconnect().removeValue()
        listenToRoom(uniqueId)
        return uniqueId
    }

    suspend fun joinRoom(roomId: String, playerName: String): Boolean {
        val upperRoomId = roomId.uppercase()
        val snapshot = db.child("rooms").child(upperRoomId).get().await()
        if (snapshot.exists()) {
            val room = snapshot.getValue(RoomState::class.java)
            if (room?.status == "WAITING" && room.guestId.isEmpty()) {
                db.child("rooms").child(upperRoomId).child("guestId").setValue(myPlayerId).await()
                db.child("rooms").child(upperRoomId).child("guestName").setValue(playerName).await()
                db.child("rooms").child(upperRoomId).child("status").setValue("PLAYING").await()
                db.child("rooms").child(upperRoomId).child("status").onDisconnect().setValue("FINISHED")
                listenToRoom(upperRoomId)
                return true
            }
        }
        return false
    }

    suspend fun deleteRoom(roomId: String) {
        try {
            db.child("rooms").child(roomId).onDisconnect().cancel()
            db.child("rooms").child(roomId).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToRoom(roomId: String) {
        _connectionStatus.value = "Inaunganisha... (Connecting)"
        currentRoomListener?.let { db.child("rooms").child(roomState.value?.roomId ?: "").removeEventListener(it) }
        
        currentRoomListener = db.child("rooms").child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(RoomState::class.java)
                if (state != null) {
                    _roomState.value = state
                    _connectionStatus.value = if (state.status == "PLAYING") "Imeunganishwa (Connected)" else "Inasubiri... (Waiting)"
                } else {
                    _roomState.value = null
                    _connectionStatus.value = "Chumba Kimefungwa (Room Closed)"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _connectionStatus.value = "Imeshindwa (Error): ${error.message}"
            }
        })
    }

    suspend fun sendMove(roomId: String, moveStr: String, nextTurn: Player) {
        val updates = mapOf(
            "lastMove" to moveStr,
            "turn" to nextTurn.name
        )
        db.child("rooms").child(roomId).updateChildren(updates).await()
    }
    
    suspend fun sendEmote(roomId: String, emoji: String) {
        val emoteStr = "$myPlayerId|$emoji|${System.currentTimeMillis()}"
        db.child("rooms").child(roomId).child("lastEmote").setValue(emoteStr).await()
    }

    suspend fun setFinished(roomId: String) {
        db.child("rooms").child(roomId).child("status").setValue("FINISHED").await()
    }
}
