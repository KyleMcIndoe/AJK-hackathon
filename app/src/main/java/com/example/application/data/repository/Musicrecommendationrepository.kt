package com.example.application.data.repository

import com.example.application.data.api.LastFmApiClient
import com.example.application.data.models.LastFmArtistInfo
import com.example.application.data.models.LastFmTrack
import com.example.application.data.models.SimilarArtist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRecommendationRepository {

    private val lastFmService = LastFmApiClient.apiService
    private val apiKey = LastFmApiClient.getApiKey()

    suspend fun getSimilarArtists(artistName: String): Result<List<SimilarArtist>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = lastFmService.getSimilarArtists(
                    artist = artistName,
                    apiKey = apiKey
                )
                Result.success(response.similarartists?.artist ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getArtistInfo(artistName: String): Result<LastFmArtistInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = lastFmService.getArtistInfo(
                    artist = artistName,
                    apiKey = apiKey
                )
                Result.success(response.artist)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getTopTracks(artistName: String): Result<List<LastFmTrack>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = lastFmService.getTopTracks(
                    artist = artistName,
                    apiKey = apiKey
                )
                Result.success(response.toptracks?.track ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}