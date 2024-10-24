package com.example.fantasy_basketball

import com.google.android.gms.common.api.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface PlayerAPIService {

    @Headers(
        "x-rapidapi-host: tank01-fantasy-stats.p.rapidapi.com",
        "x-rapidapi-key: 4deda66674mshf5152633c5351f8p1d66ccjsn8b6a83f987f9"
    )
    @GET("getNBAPlayerList")
    suspend fun getNBAPlayerList(): PlayerAPIResponse  // Return the full response object

    @Headers(
        "x-rapidapi-host: tank01-fantasy-stats.p.rapidapi.com",
        "x-rapidapi-key: a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642"
    )
    @GET("getNBAProjections?numOfDays=7&pts=1&reb=1.25&TOV=-1&stl=3&blk=3&ast=1.5&mins=0")
    suspend fun getNBAPlayerProjections(): PlayerAPIprojResponse

    @Headers(
        "x-rapidapi-host: tank01-fantasy-stats.p.rapidapi.com",
        "x-rapidapi-key: a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642"
    )
    @GET("getNBATeamRoster")
    suspend fun getNBATeamRoster(
        @Query("teamID") teamID: String,
        @Query("statsToGet") statsToGet: String = "totals"
    ): TeamRosterResponse

}