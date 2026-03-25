package com.example.rippleci.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

interface TrumbaApiService {
    // This looks at the root of the JSON file you provided and expects a list of SchoolEvents
    @GET("calendars/csuci-calendar-of-events.json")
    suspend fun getEvents(): List<SchoolEvent>
}

// This creates a single, reusable network connection for your whole app
object RetrofitInstance {
    private const val BASE_URL = "https://25livepub.collegenet.com/"

    private val json = Json {
        ignoreUnknownKeys = true // Good practice to ignore unknown fields from API
    }

    val api: TrumbaApiService by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(TrumbaApiService::class.java)
    }
}
