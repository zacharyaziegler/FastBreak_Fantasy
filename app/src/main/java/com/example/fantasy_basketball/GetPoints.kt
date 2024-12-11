package com.example.fantasy_basketball

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore




private val retrofit = Retrofit.Builder()
    .baseUrl("https://tank01-fantasy-stats.p.rapidapi.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val api: PlayerAPIService = retrofit.create(PlayerAPIService::class.java)

private val rateLimitPeriod = 1_000L
private val maxRequestsPerSecond = 100
private var lastRequestTimestamps = mutableListOf<Long>()

private fun fetchMatchups(leagueId: String, callback: (List<Map<String, Any>>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("Leagues").document(leagueId).collection("Matchups")
        .get()
        .addOnSuccessListener { documents ->
            val matchups = documents.map { document ->
                mapOf(
                    "matchupId" to document.id,
                    "team1ID" to (document.getString("team1ID") ?: ""),
                    "team2ID" to (document.getString("team2ID") ?: ""),
                    "week" to (document.getString("week") ?: "")
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
            val teamRosters = documents.associate { document ->
                document.id to (document.get("Starting") as? List<String> ?: emptyList())
            }
            callback(teamRosters)
        }
        .addOnFailureListener { exception ->
            Log.e("FetchTeamRosters", "Error fetching rosters", exception)
        }
}

private suspend fun fetchPlayerFantasyPoints(playerID: String): Double {
    var delayTime: Long = 0

    synchronized(lastRequestTimestamps) {
        val now = System.currentTimeMillis()
        lastRequestTimestamps = lastRequestTimestamps.filter { it > now - rateLimitPeriod }.toMutableList()

        if (lastRequestTimestamps.size >= maxRequestsPerSecond) {
            val earliest = lastRequestTimestamps.first()
            delayTime = earliest + rateLimitPeriod - now
        }

        lastRequestTimestamps.add(now)
    }

    if (delayTime > 0) {
        delay(delayTime)
    }

    return try {
        val response = api.getNBAGamesForPlayer(playerID, "2025", "true")
        if (response.statusCode == 200) {
            val gamesData = response.body
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val currentDate = Calendar.getInstance()
            val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

            val totalPoints = gamesData.entries.filter { (gameKey, _) ->
                val gameDateStr = gameKey.substring(0, 8)
                val gameDate = dateFormat.parse(gameDateStr)
                gameDate != null && gameDate.after(oneWeekAgo.time) && gameDate.before(currentDate.time)
            }.sumOf { (_, gameData) ->
                val statsBody = gameData as? PlayerGameStatsBody
                statsBody?.fantasyPoints?.toDoubleOrNull() ?: 0.0
            }

            Log.d("fetchPlayerFantasyPoints", "Player $playerID has fantasy points: $totalPoints")
            totalPoints
        } else {
            Log.e("fetchPlayerFantasyPoints", "Failed for playerID: $playerID, Status: ${response.statusCode}")
            0.0
        }
    } catch (e: Exception) {
        Log.e("fetchPlayerFantasyPoints", "Error fetching fantasy points for playerID: $playerID", e)
        0.0
    }
}

private suspend fun calculateTeamPoints(rosters: Map<String, List<String>>): Map<String, Double> {
    val allPlayerIDs = rosters.values.flatten()
    val playerPoints = coroutineScope {
        allPlayerIDs.associateWith { playerID ->
            async { fetchPlayerFantasyPoints(playerID) }
        }.mapValues { it.value.await() }
    }

    return rosters.mapValues { (teamID, roster) ->
        val totalPoints = roster.sumOf { playerPoints[it] ?: 0.0 }
        Log.d("calculateTeamPoints", "Team $teamID total points: $totalPoints")
        totalPoints
    }
}

private fun updateMatchupResultsInBatch(
    leagueId: String,
    results: List<Triple<String, Double, Double>>,
    teamDetails: List<Pair<String, Pair<String, String>>>
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    results.forEachIndexed { index, (matchupId, team1Score, team2Score) ->
        val result = when {
            team1Score > team2Score -> "team1"
            team2Score > team1Score -> "team2"
            else -> "tie"
        }

        Log.d("updateMatchupResultsInBatch", "Matchup $matchupId: Team1=$team1Score, Team2=$team2Score, Result=$result")

        val matchupRef = db.collection("Leagues").document(leagueId)
            .collection("Matchups").document(matchupId)

        batch.update(
            matchupRef,
            mapOf(
                "team1Score" to team1Score,
                "team2Score" to team2Score,
                "result" to result
            )
        )

        val (team1ID, team2ID) = teamDetails[index].second
        val teamRef = db.collection("Leagues").document(leagueId).collection("Teams")

        if (result == "team1") {
            batch.update(teamRef.document(team1ID), "wins", FieldValue.increment(1))
            batch.update(teamRef.document(team2ID), "losses", FieldValue.increment(1))
        } else if (result == "team2") {
            batch.update(teamRef.document(team2ID), "wins", FieldValue.increment(1))
            batch.update(teamRef.document(team1ID), "losses", FieldValue.increment(1))
        }
    }

    batch.commit().addOnSuccessListener {
        Log.d("FirebaseBatch", "Matchup results and team stats updated successfully.")
    }.addOnFailureListener { e ->
        Log.e("FirebaseBatch", "Error updating matchup results and team stats", e)
    }
}

fun processLeagueMatchups(leagueId: String, week: String) {
    fetchMatchups(leagueId) { matchups ->
        val weeklyMatchups = matchups.filter { matchup ->
            matchup["week"] == week
        }

        fetchTeamRosters(leagueId) { teamRosters ->
            CoroutineScope(Dispatchers.IO).launch {
                val matchupResults = mutableListOf<Triple<String, Double, Double>>()
                val teamDetails = mutableListOf<Pair<String, Pair<String, String>>>()

                weeklyMatchups.forEach { matchup ->
                    val matchupId = matchup["matchupId"] as String
                    val team1ID = matchup["team1ID"] as String
                    val team2ID = matchup["team2ID"] as String

                    val roster1 = teamRosters[team1ID] ?: emptyList()
                    val roster2 = teamRosters[team2ID] ?: emptyList()

                    Log.d("processLeagueMatchups", "Calculating points for matchup $matchupId")
                    Log.d("processLeagueMatchups", "Team1 ($team1ID) roster: $roster1")
                    Log.d("processLeagueMatchups", "Team2 ($team2ID) roster: $roster2")

                    val teamScores = calculateTeamPoints(
                        mapOf(team1ID to roster1, team2ID to roster2)
                    )

                    val team1Score = teamScores[team1ID] ?: 0.0
                    val team2Score = teamScores[team2ID] ?: 0.0

                    Log.d("processLeagueMatchups", "Team1 ($team1ID) score: $team1Score")
                    Log.d("processLeagueMatchups", "Team2 ($team2ID) score: $team2Score")

                    matchupResults.add(Triple(matchupId, team1Score, team2Score))
                    teamDetails.add(Pair(matchupId, Pair(team1ID, team2ID)))
                }

                withContext(Dispatchers.Main) {
                    updateMatchupResultsInBatch(leagueId, matchupResults, teamDetails)
                }
            }
        }
    }
}

fun processAllLeaguesForWeek() {
    val db = FirebaseFirestore.getInstance()

    db.collection("Leagues")
        .get()
        .addOnSuccessListener { leagues ->
            leagues.forEach { league ->
                val leagueId = league.id
                val currentWeek = league.getString("currentWeek") ?: "week01"
                val draftStatus = league.getString("draftStatus") ?: ""

                Log.d("processAllLeaguesForWeek", "Processing league $leagueId for week $currentWeek with draftStatus $draftStatus")

                if (draftStatus == "completed") {
                    processLeagueMatchups(leagueId, currentWeek)

                    incrementWeek(currentWeek) { nextWeek ->
                        if (nextWeek != null) {
                            db.collection("Leagues").document(leagueId)
                                .update("currentWeek", nextWeek)
                                .addOnSuccessListener {
                                    Log.d("processAllLeaguesForWeek", "Updated league $leagueId to week $nextWeek")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("processAllLeaguesForWeek", "Error updating week for league $leagueId", e)
                                }
                        } else {
                            Log.d("processAllLeaguesForWeek", "League $leagueId has reached the final week.")
                        }
                    }
                } else {
                    Log.d("processAllLeaguesForWeek", "Skipping league $leagueId as draft is not completed.")
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("processAllLeaguesForWeek", "Error fetching leagues", e)
        }
}

private fun incrementWeek(currentWeek: String, callback: (String?) -> Unit) {
    val weekNumber = currentWeek.removePrefix("week").toIntOrNull()
    if (weekNumber != null && weekNumber < 19) {
        callback("week${String.format("%02d", weekNumber + 1)}")
    } else {
        callback(null) // Return null if already at week19
    }
}
