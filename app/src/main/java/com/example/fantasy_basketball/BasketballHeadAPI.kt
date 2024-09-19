package com.example.fantasy_basketball

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface BasketballHeadAPI {
    @GET("players/search")
    fun searchPlayerByName(
        @Query("name") playerName: String
    ): Call<PlayerListResponse>


}

data class Player(
    val playerId: String,
    val firstName: String,
    val lastName: String

)

data class PlayerListResponse(
    val body: List<Player>?
)

data class PlayerAveragesResponse(
    val points: Double,
    val assists: Double,
    val rebounds: Double

)


val apiKeyInterceptor = Interceptor { chain ->
    val request: Request = chain.request().newBuilder()
        .addHeader("X-RapidAPI-Key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
        .addHeader("X-RapidAPI-Host", "basketball-head.p.rapidapi.com")
        .build()
    chain.proceed(request)
}


val client: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(apiKeyInterceptor)
    .build()


val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://basketball-head.p.rapidapi.com/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()


val api: BasketballHeadAPI = retrofit.create(BasketballHeadAPI::class.java)
