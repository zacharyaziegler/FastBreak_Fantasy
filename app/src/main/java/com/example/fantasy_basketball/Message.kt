package com.example.fantasy_basketball

import com.google.firebase.Timestamp

data class Message(
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Timestamp? = null,
)
