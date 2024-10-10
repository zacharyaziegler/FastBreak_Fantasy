package com.example.fantasy_basketball

data class PlayerAPIResponse(
    val statusCode: Int,
    val body: List<Player>  // The player data is in the "body" array
)

