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

data class ADPResponse(
    val statusCode: Int,
    val body: ADPBody
)

data class ADPBody(
    val adpDate: String,
    val adpList: List<ADPPlayer>
)

data class ADPPlayer(
    val overallADP: String,
    val playerID: String,
    val longName: String,
    val posADP: String
)

data class PlayerGameStatsResponse(
    val statusCode: Int,
    val body: Map<String, PlayerGameStatsBody>
)

data class PlayerGameStatsBody(
    val pts: String,
    val reb: String,
    val ast: String,
    val stl: String,
    val blk: String,
    val TOV: String,
    val fantasyPoints: String,
    val gameID: String
)

fun PlayerGameStatsBody.extractOpponentAndDate(): Pair<String, String> {
    val parts = gameID.split("_")
    val rawDate = parts[0]
    val opponent = if (parts.size > 1) parts[1] else "Unknown"

    val formattedDate = "${rawDate.substring(0, 4)}-${rawDate.substring(4, 6)}-${rawDate.substring(6, 8)}"

    return Pair(opponent, formattedDate)
}

