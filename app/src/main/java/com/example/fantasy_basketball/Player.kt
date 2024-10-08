package com.example.fantasy_basketball


data class Player(
    val playerID: String,
    val longName: String,
    val points: String,
    val rebounds: String,
    val assists: String
) {
    // Enum class defined within Player to represent player status
//    enum class Status {
//        H,
//        Q,
//        IR
//    }
}

data class PlayerProjection(
    val playerID: String,
    val longName: String,
    val points: String,
    val rebounds: String,
    val assists: String,
    val steals: String,
    val blocks: String,
    val fantasyPoints: String
)
