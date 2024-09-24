package com.example.fantasy_basketball

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


suspend fun fetchPlayerStatsForLastSeason(playerName: String): PlayerAveragesResponse? {
    return withContext(Dispatchers.IO) {
        try {
            val playerId = fetchPlayerIdByName(playerName)
            if (playerId != null) {
                val seasonType = "Regular" // Can be changed to "Playoffs" if needed
                val seasonId = "2023-2024" // Specify the season

                val request = Request.Builder()
                    .url("https://basketball-head.p.rapidapi.com/players/$playerId/stats/PerGame?seasonType=$seasonType&seasonId=$seasonId")
                    .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
                    .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData ?: return@withContext null)
                    val bodyArray = jsonObject.getJSONArray("body")

                    if (bodyArray.length() > 0) {
                        val statsJson = bodyArray.getJSONObject(0)

                        return@withContext PlayerAveragesResponse(
                            team = statsJson.optString("team", ""),
                            position = statsJson.optString("position", ""),
                            points = statsJson.optDouble("pointsPerGame", 0.0),
                            rebounds = statsJson.optDouble("totalReboundsPerGame", 0.0),
                            assists = statsJson.optDouble("assistsPerGame", 0.0),
                            steals = statsJson.optDouble("stealsPerGame", 0.0),
                            blocks = statsJson.optDouble("blocksPerGame", 0.0),
                            fieldGoalPercentage = statsJson.optDouble("fieldGoalPercentage", 0.0),
                            fieldGoalsMade = statsJson.optDouble("fieldGoalsMadePerGame", 0.0),
                            fieldGoalsAttempted = statsJson.optDouble("fieldGoalAttemptsPerGame", 0.0),
                            threePointPercentage = statsJson.optDouble("threePointFieldGoalPercentage", 0.0),
                            threePointMade = statsJson.optDouble("threePointFieldGoalsMadePerGame", 0.0),
                            threePointAttempted = statsJson.optDouble("threePointFieldGoalAttemptsPerGame", 0.0),
                            personalFouls = statsJson.optDouble("personalFoulsPerGame", 0.0),
                            turnovers = statsJson.optDouble("turnoversPerGame", 0.0)
                        )
                    }
                }
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

                // Parse the response and extract the player's ID
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
            println("Team: ${stats.team}")
            println("Position: ${stats.position}")
            println("Points: ${stats.points}")
            println("Rebounds: ${stats.rebounds}")
            println("Assists: ${stats.assists}")
            println("Steals: ${stats.steals}")
            println("Blocks: ${stats.blocks}")
            println("Field Goal Percentage: ${stats.fieldGoalPercentage}")
            println("Field Goals Made: ${stats.fieldGoalsMade}")
            println("Field Goals Attempted: ${stats.fieldGoalsAttempted}")
            println("3-Point Percentage: ${stats.threePointPercentage}")
            println("3-Point Made: ${stats.threePointMade}")
            println("3-Point Attempted: ${stats.threePointAttempted}")
            println("Personal Fouls: ${stats.personalFouls}")
            println("Turnovers: ${stats.turnovers}")
        } else {
            println("Failed to fetch stats for $playerName.")
        }
    }
}



fun fetchAndPrintAllPlayers() {
    val client = OkHttpClient()

    // Create the media type and request body for the POST request
    val mediaType = "application/json".toMediaType()
    val requestBody = """
        {
            "pageSize": 100
        }
    """.trimIndent().toRequestBody(mediaType)

    // Build the request
    val request = Request.Builder()
        .url("https://basketball-head.p.rapidapi.com/players")
        .post(requestBody)
        .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
        .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
        .addHeader("Content-Type", "application/json")
        .build()

    // Make the request and handle the response using OkHttp's non-generic Call and Callback
    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            // Check if the response was successful
            if (!response.isSuccessful) {
                println("Error fetching player list: ${response.body?.string()}")
                return
            }

            // Print the raw response for debugging
            val responseData = response.body?.string()
            println("Raw response data: $responseData")

            // Parse the JSON response
            if (responseData != null) {
                val jsonObject = JSONObject(responseData)
                val playersArray = jsonObject.getJSONArray("body")

                for (i in 0 until playersArray.length()) {
                    val player = playersArray.getJSONObject(i)
                    val firstName = player.getString("firstName")
                    val lastName = player.getString("lastName")
                    println("Player: $firstName $lastName")
                }
            }
        }

        override fun onFailure(call: okhttp3.Call, e: IOException) {
            e.printStackTrace()
        }
    })
}




suspend fun fetchAndPrintPlayersWithStatsForSeason(season: String) {
    val client = OkHttpClient()

    // Fetch all players (without season in the search payload)
    val mediaType = "application/json".toMediaType()
    val requestBody = """
        {
            "pageSize": 100
        }
    """.trimIndent().toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://basketball-head.p.rapidapi.com/players")
        .post(requestBody)
        .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
        .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
        .addHeader("Content-Type", "application/json")
        .build()

    withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("Error fetching player list: ${response.body?.string()}")
                return@withContext
            }

            // Parse the response and fetch player stats
            val responseData = response.body?.string()
            println("Raw response data: $responseData")

            if (responseData != null) {
                val jsonObject = JSONObject(responseData)
                val playersArray = jsonObject.getJSONArray("body")

                // Loop through each player and fetch their stats for the specified season
                for (i in 0 until playersArray.length()) {
                    val player = playersArray.getJSONObject(i)
                    val firstName = player.getString("firstName")
                    val lastName = player.getString("lastName")
                    val playerId = player.getString("playerId")

                    // Fetch stats for each player for the specified season
                    val stats = fetchPlayerStatsForSeason(playerId, season)
                    if (stats != null) {
                        // Only print player and their stats if available
                        println("Player: $firstName $lastName")
                        println("Team: ${stats.team}")
                        println("Position: ${stats.position}")
                        println("Points: ${stats.points}")
                        println("Rebounds: ${stats.rebounds}")
                        println("Assists: ${stats.assists}")
                        println("Steals: ${stats.steals}")
                        println("Blocks: ${stats.blocks}")
                        println("Field Goal Percentage: ${stats.fieldGoalPercentage}")
                        println("Field Goals Made: ${stats.fieldGoalsMade}")
                        println("Field Goals Attempted: ${stats.fieldGoalsAttempted}")
                        println("3-Point Percentage: ${stats.threePointPercentage}")
                        println("3-Point Made: ${stats.threePointMade}")
                        println("3-Point Attempted: ${stats.threePointAttempted}")
                        println("Personal Fouls: ${stats.personalFouls}")
                        println("Turnovers: ${stats.turnovers}")
                    }
                }
            }
        }
    }
}

suspend fun fetchPlayerStatsForSeason(playerId: String, season: String): PlayerAveragesResponse? {
    return withContext(Dispatchers.IO) {
        try {
            val seasonType = "Regular" // Set to "Playoffs" if needed

            val request = Request.Builder()
                .url("https://basketball-head.p.rapidapi.com/players/$playerId/stats/PerGame?seasonType=$seasonType&seasonId=$season")
                .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
                .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
                .build()

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Error fetching player stats: ${response.body?.string()}")
                    return@withContext null
                }

                // Parse the stats response
                val responseData = response.body?.string()
                val jsonObject = JSONObject(responseData)
                val bodyArray = jsonObject.getJSONArray("body")
                if (bodyArray.length() > 0) {
                    val statsJson = bodyArray.getJSONObject(0) // Get the first stat object

                    return@withContext PlayerAveragesResponse(
                        team = statsJson.optString("team", ""),
                        position = statsJson.optString("position", ""),
                        points = statsJson.optDouble("pointsPerGame", 0.0),
                        rebounds = statsJson.optDouble("totalReboundsPerGame", 0.0),
                        assists = statsJson.optDouble("assistsPerGame", 0.0),
                        steals = statsJson.optDouble("stealsPerGame", 0.0),
                        blocks = statsJson.optDouble("blocksPerGame", 0.0),
                        fieldGoalPercentage = statsJson.optDouble("fieldGoalPercentage", 0.0),
                        fieldGoalsMade = statsJson.optDouble("fieldGoalsMadePerGame", 0.0),
                        fieldGoalsAttempted = statsJson.optDouble("fieldGoalAttemptsPerGame", 0.0),
                        threePointPercentage = statsJson.optDouble("threePointFieldGoalPercentage", 0.0),
                        threePointMade = statsJson.optDouble("threePointFieldGoalsMadePerGame", 0.0),
                        threePointAttempted = statsJson.optDouble("threePointFieldGoalAttemptsPerGame", 0.0),
                        personalFouls = statsJson.optDouble("personalFoulsPerGame", 0.0),
                        turnovers = statsJson.optDouble("turnoversPerGame", 0.0)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}

fun displayPlayersWithStatsForSeason(season: String) {
    CoroutineScope(Dispatchers.Main).launch {
        fetchAndPrintPlayersWithStatsForSeason(season)
    }
}
