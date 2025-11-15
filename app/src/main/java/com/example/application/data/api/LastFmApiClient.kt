package com.example.application.data.api

import com.example.application.data.models.LastFmApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LastFmApiClient {

    private const val BASE_URL = "https://ws.audioscrobbler.com/"

    // Get your free API key from: https://www.last.fm/api/account/create
    private const val LASTFM_API_KEY = "2901af678b3fc3a4c1d779a37a322d38"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: LastFmApiService = retrofit.create(LastFmApiService::class.java)

    fun getApiKey(): String = LASTFM_API_KEY
}