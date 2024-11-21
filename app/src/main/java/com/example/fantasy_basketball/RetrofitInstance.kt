package com.example.fantasy_basketball

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "https://tank01-fantasy-stats.p.rapidapi.com/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: PlayerAPIService by lazy {
        retrofit.create(PlayerAPIService::class.java)
    }
}
