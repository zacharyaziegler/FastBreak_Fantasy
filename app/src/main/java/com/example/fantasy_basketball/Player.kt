package com.example.fantasy_basketball


data class Player(
    val name: String,
    val pos: String,
    val team: String,
    val status: Status,
    val score: Int,
    val avg: Double,
    val min: Int,
    val fgm: Int,
    val imageUrl: Int
     // Added status field using the Status enum
) {
    // Enum class defined within Player to represent player status
    enum class Status {
        H,
        Q,
        IR
    }
}
