package com.example.fantasy_basketball

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


suspend fun fetchPlayerStatsForLastSeason(playerName: String): PlayerAveragesResponse? {
    return withContext(Dispatchers.IO) {
        try {

            val playerId = fetchPlayerIdByName(playerName)

            if (playerId != null) {
                println("Player found: $playerName, ID: $playerId")


                val seasonType = "Regular" // Set to "Playoffs" if needed
                val seasonId = "2023-2024" // Specify the season if needed


                val request = Request.Builder()
                    .url("https://basketball-head.p.rapidapi.com/players/$playerId/stats/PerGame?seasonType=$seasonType&seasonId=$seasonId")
                    .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
                    .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println("Error fetching player stats: ${response.body?.string()}")
                        return@withContext null
                    }

                    // Print the raw response for debugging
                    val responseData = response.body?.string()
                    println("Raw response data: $responseData")

                    // Parse the stats response
                    val jsonObject = JSONObject(responseData)
                    val bodyArray = jsonObject.getJSONArray("body")
                    if (bodyArray.length() > 0) {
                        val statsJson = bodyArray.getJSONObject(0) // Get the first stat object

                        // Manually extract fields
                        val points = statsJson.optDouble("pointsPerGame", 0.0)
                        val assists = statsJson.optDouble("assistsPerGame", 0.0)
                        val rebounds = statsJson.optDouble("totalReboundsPerGame", 0.0)

                        // Create PlayerAveragesResponse object from the parsed values
                        val stats = PlayerAveragesResponse(
                            points = points,
                            assists = assists,
                            rebounds = rebounds
                        )
                        println("Player stats retrieved successfully for $playerName")
                        return@withContext stats
                    } else {
                        println("No stats found in the response body for player: $playerName")
                    }
                }
            } else {
                println("Player not found: $playerName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}


suspend fun fetchPlayerIdByName(playerName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            // Split the player's full name into first name and last name
            val nameParts = playerName.split(" ")
            val firstName = nameParts[0]
            val lastName = nameParts.getOrNull(1) ?: ""

            // Create the JSON body for the request
            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = """
                {
                    "pageSize": 100,
                    "firstname": "$firstName",
                    "lastname": "$lastName"
                }
            """.trimIndent().toRequestBody(mediaType)

            // Build the request
            val request = Request.Builder()
                .url("https://basketball-head.p.rapidapi.com/players/search")
                .post(requestBody)
                .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
                .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
                .addHeader("Content-Type", "application/json")
                .build()

            // Make the request
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Error fetching player data: ${response.body?.string()}")
                    return@withContext null
                }

                // Parse the response and get the player's ID
                val responseData = response.body?.string()
                val gson = Gson()
                val playerListResponse = gson.fromJson(responseData, PlayerListResponse::class.java)

                if (playerListResponse.body != null && playerListResponse.body.isNotEmpty()) {
                    val player = playerListResponse.body.first()
                    println("Player found: ${player.firstName} ${player.lastName}, ID: ${player.playerId}")
                    return@withContext player.playerId
                } else {
                    println("No player found with name: $playerName")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}


fun displayPlayerStats(playerName: String) {
    CoroutineScope(Dispatchers.Main).launch {
        val stats = fetchPlayerStatsForLastSeason(playerName)
        if (stats != null) {
            println("Player stats for $playerName in last season:")
            println("Points: ${stats.points}")
            println("Assists: ${stats.assists}")
            println("Rebounds: ${stats.rebounds}")

        } else {
            println("Failed to fetch stats for $playerName.")
        }
    }
}
