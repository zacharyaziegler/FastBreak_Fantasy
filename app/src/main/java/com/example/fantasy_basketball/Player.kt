package com.example.fantasy_basketball


data class Player(
    val playerID: String,
    val longName: String,
    val team: String?,
    val teamID: String?
) {
    // Enum class defined within Player to represent player status
//    enum class Status {
//        H,
//        Q,
//        IR
//    }
}
