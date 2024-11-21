package com.example.fantasy_basketball

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit




private val retrofit = Retrofit.Builder()
    .baseUrl("https://tank01-fantasy-stats.p.rapidapi.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val api: PlayerAPIService = retrofit.create(PlayerAPIService::class.java)

private fun fetchMatchups(leagueId: String, callback: (List<Map<String, Any>>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("Leagues").document(leagueId).collection("Matchups")
        .get()
        .addOnSuccessListener { documents ->
            val matchups = mutableListOf<Map<String, Any>>()
            for (document in documents) {
                matchups.add(
                    mapOf(
                        "matchupId" to document.id,
                        "team1ID" to (document.getString("teamID") ?: ""),
                        "team2ID" to (document.getString("team2ID") ?: ""),
                        "week" to (document.getString("week") ?: "")
                    )
                )
            }
            callback(matchups)
        }
        .addOnFailureListener { exception ->
            Log.e("FetchMatchups", "Error fetching matchups", exception)
        }
}


private fun fetchTeamRosters(leagueId: String, callback: (Map<String, List<String>>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("Leagues").document(leagueId).collection("Teams")
        .get()
        .addOnSuccessListener { documents ->
            val teamRosters = mutableMapOf<String, List<String>>()
            for (document in documents) {
                val teamId = document.id
                val roster = document.get("roster") as? List<String> ?: emptyList()
                teamRosters[teamId] = roster
            }
            callback(teamRosters)
        }
        .addOnFailureListener { exception ->
            Log.e("FetchTeamRosters", "Error fetching rosters", exception)
        }
}


private suspend fun fetchPlayerFantasyPoints(playerID: String): Double {
    return try {
        val response = api.getNBAGamesForPlayer(playerID, "2025", "true")
        if (response.statusCode == 200) {
            val pastGames = response.body.filterKeys { gameKey ->
                val gameDateStr = gameKey.substring(0, 8)
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val gameDate = dateFormat.parse(gameDateStr)

                val currentDate = Calendar.getInstance()
                val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

                gameDate != null && gameDate.after(oneWeekAgo.time) && gameDate.before(currentDate.time)
            }.mapNotNull { (_, gameData) ->
                val dataMap = gameData as? Map<String, Any> ?: return@mapNotNull null
                dataMap["fantasyPoints"]?.toString()?.toDoubleOrNull() ?: 0.0
            }

            pastGames.sum()
        } else {
            Log.e("FetchPoints", "Failed for playerID: $playerID, Status: ${response.statusCode}")
            0.0
        }
    } catch (e: Exception) {
        Log.e("FetchPoints", "Error fetching fantasy points for playerID: $playerID", e)
        0.0
    }
}


private suspend fun calculateTeamPoints(teamRosters: Map<String, List<String>>): Map<String, Double> {
    val teamPoints = mutableMapOf<String, Double>()
    for ((teamId, roster) in teamRosters) {
        val teamTotalPoints = roster.sumOf { playerID ->
            fetchPlayerFantasyPoints(playerID)
        }
        teamPoints[teamId] = teamTotalPoints
    }
    return teamPoints
}


private fun updateMatchupResult(
    leagueId: String,
    matchupId: String,
    team1ID: String,
    team1Score: Double,
    team2ID: String,
    team2Score: Double
) {
    val db = FirebaseFirestore.getInstance()
    val result: String

    if (team1Score > team2Score) {
        result = "team1"
    } else if (team2Score > team1Score) {
        result = "team2"
    } else {
        result = "tie"
    }

    val matchupRef = db.collection("Leagues").document(leagueId)
        .collection("Matchups").document(matchupId)

    matchupRef.update(
        mapOf(
            "team1Score" to team1Score,
            "team2Score" to team2Score,
            "result" to result
        )
    )

    val teamRef = db.collection("Leagues").document(leagueId).collection("Teams")

    if (result == "team1") {
        teamRef.document(team1ID).update("wins", FieldValue.increment(1))
        teamRef.document(team2ID).update("losses", FieldValue.increment(1))
    } else if (result == "team2") {
        teamRef.document(team2ID).update("wins", FieldValue.increment(1))
        teamRef.document(team1ID).update("losses", FieldValue.increment(1))
    }
}


fun processLeagueMatchups(leagueId: String, week: String) {
    fetchMatchups(leagueId) { matchups ->
        val weeklyMatchups = matchups.filter { matchup ->
            matchup["week"] == week
        }

        weeklyMatchups.forEach { matchup ->
            val matchupId = matchup["matchupId"] as String
            val team1ID = matchup["team1ID"] as String
            val team2ID = matchup["team2ID"] as String

            fetchTeamRosters(leagueId) { teamRosters ->
                val roster1 = teamRosters[team1ID] ?: emptyList()
                val roster2 = teamRosters[team2ID] ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    val team1Score = calculateTeamPoints(mapOf(team1ID to roster1))[team1ID] ?: 0.0
                    val team2Score = calculateTeamPoints(mapOf(team2ID to roster2))[team2ID] ?: 0.0

                    withContext(Dispatchers.Main) {
                        updateMatchupResult(leagueId, matchupId, team1ID, team1Score, team2ID, team2Score)
                    }
                }
            }
        }
    }
}
