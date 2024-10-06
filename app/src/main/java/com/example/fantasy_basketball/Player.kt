package com.example.fantasy_basketball


data class Player(
    val playerID: String,
    val longName: String,
    val points: Double,
    val rebounds: Double,
    val assists: Double
) {
    // Enum class defined within Player to represent player status
//    enum class Status {
//        H,
//        Q,
//        IR
//    }
}
