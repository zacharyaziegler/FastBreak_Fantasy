package com.example.fantasy_basketball


data class PlayerAPIResponse(
    val statusCode: Int,
    val body: List<Player>  // The player data is in the "body" array
)

// Update to correctly map the response
data class PlayerAPIprojResponse(
    val statusCode: Int,
    val body: PlayerBody  // Now the body contains playerProjections
)

data class PlayerBody(
    val playerProjections: Map<String, PlayerProjection>  // The player projections keyed by playerID
)
data class TeamRosterResponse(
    val statusCode: Int,         // HTTP status code of the response
    val body: TeamRosterBody     // Body containing the team information and roster
)
data class TeamRosterBody(
    val team: String,            // Team abbreviation (e.g., "SAC")
    val teamID: String,          // Unique ID for the team
    val roster: List<Player>  // List of players in the team
)
