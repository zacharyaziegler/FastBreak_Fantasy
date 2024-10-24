package com.example.fantasy_basketball.matchup_display_logic

data class MatchupData(
    val userTeamName: String,
    val opponentTeamName: String,
    val leagueName: String,
    val leagueId: String,
    val userTeamImageUrl: String,
    val opponentTeamImageUrl: String
)
