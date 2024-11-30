package com.example.google_map.ui

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {
    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Call<DirectionsResponse>
}
