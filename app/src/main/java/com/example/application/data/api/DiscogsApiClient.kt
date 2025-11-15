package com.example.application.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DiscogsApiClient {

    private const val BASE_URL = "https://api.discogs.com/"
    private const val USER_AGENT = "VinylScanner/1.0 +http://yourwebsite.com"

    // ⚠️ IMPORTANT: Never hardcode tokens in production!
    // Use BuildConfig or secure storage
    private const val PERSONAL_TOKEN = "RPcZxOFnGDWlzRPNizjpcwJOCcpTXOoHGlNPbbTv"

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Discogs token=$PERSONAL_TOKEN")
            .addHeader("User-Agent", USER_AGENT)
            .build()

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: DiscogsApiService = retrofit.create(DiscogsApiService::class.java)
}