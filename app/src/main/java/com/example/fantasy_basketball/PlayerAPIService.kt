package com.example.fantasy_basketball

import retrofit2.http.GET
import retrofit2.http.Headers

interface PlayerAPIService {

    @Headers(
        "x-rapidapi-host: tank01-fantasy-stats.p.rapidapi.com",
        "x-rapidapi-key: 4deda66674mshf5152633c5351f8p1d66ccjsn8b6a83f987f9"
    )
    @GET("getNBAPlayerList")
    suspend fun getNBAPlayerList(): PlayerAPIResponse  // Return the full response object
}