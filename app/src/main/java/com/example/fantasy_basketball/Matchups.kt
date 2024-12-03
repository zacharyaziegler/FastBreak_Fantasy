package com.example.fantasy_basketball

data class Matchup(
    val matchupId: String,
    val team1ID: String,
    val team2ID: String,
    val week: String
)

data class FullMatchup(
    val matchup: Matchup,
    val team1Details: TeamDetails?,
    val team2Details: TeamDetails?
)

data class TeamDetails(
    val name: String,
    val logo: String
)