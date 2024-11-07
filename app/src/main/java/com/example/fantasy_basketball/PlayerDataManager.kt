package com.example.fantasy_basketball

import com.example.fantasy_basketball.Injury
import com.example.fantasy_basketball.Player
import com.example.fantasy_basketball.PlayerAPIService
import com.example.fantasy_basketball.PlayerProjection
import com.example.fantasy_basketball.PlayerStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerDataManager {

    private val db = FirebaseFirestore.getInstance()

    // Retrofit setup with the correct API service
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://tank01-fantasy-stats.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: PlayerAPIService = retrofit.create(PlayerAPIService::class.java)

    // Fetch the list of NBA players from the API
    suspend fun fetchPlayerList(): List<Player> {
        return try {
            println("Starting API request to fetch player list...")
            val response = api.getNBAPlayerList()
            println("Fetched ${response.body.size} players from API.")
            response.body
        } catch (e: Exception) {
            println("Error fetching player list: ${e.message}")
            emptyList() // Return an empty list in case of failure
        }
    }

    // Fetch NBA player projections from the API
    suspend fun fetchPlayerProjections(): Map<String, PlayerProjection> {
        return try {
            println("Fetching player projections from API...")
            val response = api.getNBAPlayerProjections()

            // Log the full API response for debugging
            println("Full API Response: $response")

            // Filter the response and return only valid player projections
            val validProjections = mutableMapOf<String, PlayerProjection>()
            response.body.playerProjections.forEach { (playerId, projection) ->
                projection.longName?.let {
                    validProjections[playerId] = projection
                    println("Fetched projection for player ID $playerId: $it")
                } ?: println("No valid projection data for player ID $playerId.")
            }

            // Print how many projections were successfully processed
            println("Successfully processed projections for ${validProjections.size} players.")
            validProjections
        } catch (e: Exception) {
            println("Error fetching player projections: ${e.message}")
            emptyMap() // Return an empty map in case of failure
        }
    }

    suspend fun fetchPlayerListFromTeam(): List<Player> {
        val teamsAbv = listOf(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"
        )

        val allPlayers = mutableListOf<Player>()

        for (teamAbv in teamsAbv) {
            try {
                println("Fetching roster for team $teamAbv...")
                val response = api.getNBATeamRoster(teamAbv)
                println("Fetched ${response.body.roster.size} players for team $teamAbv.")
                allPlayers.addAll(response.body.roster)
            } catch (e: Exception) {
                println("Error fetching roster for team $teamAbv: ${e.message}")
            }
        }
        return allPlayers
    }

    // Update Firestore with player projections
    fun updatePlayersWithProjections(playerProjections: Map<String, PlayerProjection>) {
        db.collection("players").get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { document ->
                    val playerId = document.id
                    val projection = playerProjections[playerId]

                    // Create a map to update player projection data
                    val projectionData = projection?.let {
                        hashMapOf(
                            "blk" to it.blk,
                            "mins" to it.mins,
                            "ast" to it.ast,
                            "pos" to it.pos,
                            "stl" to it.stl,
                            "TOV" to it.TOV,
                            "pts" to it.pts,
                            "reb" to it.reb,
                            "fantasyPoints" to it.fantasyPoints
                        )
                    } ?: hashMapOf(
                        "blk" to null, "mins" to null, "ast" to null, "pos" to null,
                        "stl" to null, "TOV" to null, "pts" to null, "reb" to null, "fantasyPoints" to null
                    )

                    // Update Firestore with projection data
                    db.collection("players").document(playerId)
                        .set(hashMapOf("Projections" to projectionData), SetOptions.merge())
                        .addOnSuccessListener {
                            println("Player ${document.getString("longName")}'s projections updated in Firestore.")
                        }
                        .addOnFailureListener { e ->
                            println("Error updating projections for player ${document.getString("longName")}: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                println("Error fetching players from Firestore: $e")
            }
    }

    fun addPlayersToFirestore(players: List<Player>) {
        val batchSize = 500
        val playerChunks = players.chunked(batchSize)

        playerChunks.forEachIndexed { chunkIndex, playerChunk ->
            val batch = db.batch()

            playerChunk.forEach { player ->
                val playerRef = db.collection("players").document(player.playerID)

                // Handle nullable stats safely
                val stats = player.stats ?: PlayerStats(
                    blk = null, fga = null, DefReb = null, ast = null, ftp = null, tptfgp = null,
                    tptfgm = null, trueShootingAttempts = null, stl = null, fgm = null, pts = null,
                    reb = null, fgp = null, fta = null, mins = null,
                    gamesPlayed = null, TOV = null, tptfga = null, OffReb = null, ftm = null
                )

                // Handle nullable injury safely
                val injury = player.injury ?: Injury(
                    description = null, injReturnDate = null, designation = null, injDate = null
                )

                val playerData = hashMapOf(
                    "team" to player.team,
                    "longName" to player.longName,
                    "teamID" to player.teamID,
                    "jerseyNum" to player.jerseyNum,
                    "pos" to player.pos,
                    "nbaComHeadshot" to player.nbaComHeadshot,
                    "TotalStats" to mapOf(
                        "blk" to stats.blk,
                        "fga" to stats.fga,
                        "DefReb" to stats.DefReb,
                        "ast" to stats.ast,
                        "ftp" to stats.ftp,
                        "tptfgp" to stats.tptfgp,
                        "tptfgm" to stats.tptfgm,
                        "trueShootingPercentage" to stats.trueShootingAttempts,
                        "stl" to stats.stl,
                        "fgm" to stats.fgm,
                        "pts" to stats.pts,
                        "reb" to stats.reb,
                        "fgp" to stats.fgp,
                        "fta" to stats.fta,
                        "mins" to stats.mins,
                        "gamesPlayed" to stats.gamesPlayed,
                        "TOV" to stats.TOV,
                        "tptfga" to stats.tptfga,
                        "OffReb" to stats.OffReb,
                        "ftm" to stats.ftm
                    ),
                    "Injury" to mapOf(
                        "description" to injury.description,
                        "injDate" to injury.injDate,
                        "status" to injury.designation,
                        "injReturnDate" to injury.injReturnDate
                    )
                )

                batch.set(playerRef, playerData)
            }

            // Commit the batch to Firestore
            batch.commit()
                .addOnSuccessListener {
                    println("Successfully committed chunk $chunkIndex to Firestore with ${playerChunk.size} players.")
                }
                .addOnFailureListener { e ->
                    println("Error committing chunk $chunkIndex to Firestore: $e")
                }
        }
    }


    // Fetch player list and store it in Firestore
    suspend fun fetchAndStorePlayers() {
        try {
            println("Initiating process to fetch and store players...")
            val players = fetchPlayerList()
            if (players.isNotEmpty()) {
                addPlayersToFirestore(players)
                println("Player data has been written to Firestore.")
            } else {
                println("No players fetched to store in Firestore.")
            }
        } catch (e: Exception) {
            println("Error fetching or storing player data: ${e.message}")
        }
    }

    // Fetch player projections and update Firestore
    suspend fun fetchAndStorePlayerProjections() {
        try {
            println("Fetching and storing player projections...")
            val playerProjections = fetchPlayerProjections()
            if (playerProjections.isNotEmpty()) {
                updatePlayersWithProjections(playerProjections)
                println("Player projections have been updated in Firestore.")
            } else {
                println("No player projections fetched to update Firestore.")
            }
        } catch (e: Exception) {
            println("Error fetching or updating player projections: ${e.message}")
        }
    }


    suspend fun fetchAndStorePlayersFromTeam() {
        try {
            println("Initiating process to fetch and store players from teams...")
            val players = fetchPlayerListFromTeam()
            if (players.isNotEmpty()) {
                addPlayersToFirestore(players)
                println("Player data from teams has been written to Firestore.")
            } else {
                println("No players fetched to store in Firestore.")
            }
        } catch (e: Exception) {
            println("Error fetching or storing player data from teams: ${e.message}")
        }
    }


    suspend fun fetchAndStoreADP() {
        try {
            println("Starting API request to fetch player ADP data...")

            // Fetch the ADP list from the API
            val response = api.getNBAADP()
            if (response.statusCode != 200) {
                println("Failed to fetch ADP data, status code: ${response.statusCode}")
                return
            }

            val adpList = response.body.adpList

            // Check if ADP data was fetched
            if (adpList.isEmpty()) {
                println("No ADP data fetched to store in Firestore.")
                return
            }

            // Store the ADP list in Firestore with `overallADP` as a Double field
            val batch = db.batch()
            adpList.forEach { adpPlayer ->
                // Use a unique combination of overallADP and playerID for the document ID to avoid conflicts
                val adpDocumentId = "${adpPlayer.overallADP}_${adpPlayer.playerID}"
                val playerRef = db.collection("draftposition").document(adpDocumentId)

                // Store overallADP as a Double
                val adpData = mapOf(
                    "overallADP" to adpPlayer.overallADP.toDoubleOrNull(),  // Store as Double
                    "longName" to adpPlayer.longName,
                    "posADP" to adpPlayer.posADP,
                    "playerID" to adpPlayer.playerID
                )
                batch.set(playerRef, adpData)
            }

            // Commit the batch
            batch.commit().await()
            println("Successfully stored ADP data in Firestore with `overallADP` as a numeric field.")

        } catch (e: Exception) {
            println("Error in fetchAndStoreADP: ${e.message}")
        }
    }



}



 

