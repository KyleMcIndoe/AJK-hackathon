package com.example.application.data.api

import com.example.application.Release
import com.example.application.SearchResponse
import retrofit2.http.*

interface DiscogsApiService {

    @GET("database/search")
    suspend fun searchReleases(
        @Query("q") query: String,
        @Query("type") type: String = "release",
        @Query("format") format: String? = "vinyl",
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("releases/{release_id}")
    suspend fun getRelease(
        @Path("release_id") releaseId: Long
    ): Release

    @GET("masters/{master_id}")
    suspend fun getMaster(
        @Path("master_id") masterId: Long
    ): Release

    // Search by barcode
    @GET("database/search")
    suspend fun searchByBarcode(
        @Query("barcode") barcode: String,
        @Query("type") type: String = "release"
    ): SearchResponse

    // Search by catalog number
    @GET("database/search")
    suspend fun searchByCatalogNumber(
        @Query("catno") catalogNumber: String,
        @Query("type") type: String = "release"
    ): SearchResponse
}