package com.example.application.data.repository

import com.example.application.Release
import com.example.application.SearchResponse
import com.example.application.data.api.DiscogsApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscogsRepository {

    private val apiService = DiscogsApiClient.apiService

    suspend fun searchByAlbumName(albumName: String): Result<SearchResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchReleases(query = albumName)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchByBarcode(barcode: String): Result<SearchResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchByBarcode(barcode = barcode)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getReleaseDetails(releaseId: Long): Result<Release> {
        return withContext(Dispatchers.IO) {
            try {
                val release = apiService.getRelease(releaseId)
                Result.success(release)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMasterRelease(masterId: Long): Result<Release> {
        return withContext(Dispatchers.IO) {
            try {
                val master = apiService.getMaster(masterId)
                Result.success(master)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}