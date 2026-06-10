package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class Friend(
    val uid: String = "",
    val username: String = "",
    val isOnline: Boolean = false,
    val inviteRoomId: String = "" // if they invited us
)

class FriendManager {
    private val db = FirebaseDatabase.getInstance().reference

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Friend>>(emptyList())
    val searchResults: StateFlow<List<Friend>> = _searchResults.asStateFlow()

    fun updateMyPresence(username: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(uid).child("username").setValue(username)
        db.child("users").child(uid).child("isOnline").setValue(true)
        db.child("users").child(uid).child("isOnline").onDisconnect().setValue(false)
        listenToFriends()
    }

    suspend fun searchUsers(query: String) {
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        val snapshot = db.child("users").get().await()
        val results = mutableListOf<Friend>()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        for (userSnapshot in snapshot.children) {
            val uid = userSnapshot.key ?: continue
            val username = userSnapshot.child("username").getValue(String::class.java) ?: ""
            if (uid != myUid && username.contains(query, ignoreCase = true)) {
                results.add(Friend(uid = uid, username = username))
            }
        }
        _searchResults.value = results
    }

    suspend fun addFriend(friendUid: String, friendUsername: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(myUid).child("friends").child(friendUid).setValue(friendUsername).await()
    }

    fun listenToFriends() {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(myUid).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Friend>()
                for (friendSnapshot in snapshot.children) {
                    val fUid = friendSnapshot.key ?: continue
                    val fUsername = friendSnapshot.getValue(String::class.java) ?: ""
                    // we can listen to their online status or just add to list
                    list.add(Friend(uid = fUid, username = fUsername))
                }
                _friends.value = list
                
                // Now load their online status and invites
                list.forEach { friend ->
                    db.child("users").child(friend.uid).addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(s2: DataSnapshot) {
                            val isOnline = s2.child("isOnline").getValue(Boolean::class.java) ?: false
                            val currentList = _friends.value.toMutableList()
                            val idx = currentList.indexOfFirst { it.uid == friend.uid }
                            if (idx != -1) {
                                currentList[idx] = currentList[idx].copy(isOnline = isOnline)
                                _friends.value = currentList
                            }
                        }
                        override fun onCancelled(e: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        // Listen to invites directly on my user node
        db.child("users").child(myUid).child("invites").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // updates the friends list with who invited us
                for (inviteSnapshot in snapshot.children) {
                    val fUid = inviteSnapshot.key ?: continue
                    val roomId = inviteSnapshot.getValue(String::class.java) ?: ""
                    
                    val currentList = _friends.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.uid == fUid }
                    if (idx != -1) {
                        currentList[idx] = currentList[idx].copy(inviteRoomId = roomId)
                        _friends.value = currentList
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun sendInvite(friendUid: String, roomId: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(friendUid).child("invites").child(myUid).setValue(roomId).await()
    }
    
    suspend fun clearInvite(friendUid: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(myUid).child("invites").child(friendUid).removeValue().await()
    }
}
