package com.example.application.data.models

import retrofit2.http.GET
import retrofit2.http.Query

// Last.fm API Models
data class LastFmArtistResponse(
    val similarartists: SimilarArtists?
)

data class SimilarArtists(
    val artist: List<SimilarArtist>
)

data class SimilarArtist(
    val name: String,
    val mbid: String?,
    val match: String,  // Similarity score (0-1)
    val url: String,
    val image: List<LastFmImage>
)

data class LastFmImage(
    val size: String,
    val text: String
)

// Last.fm API Service
interface LastFmApiService {

    @GET("2.0/")
    suspend fun getSimilarArtists(
        @Query("method") method: String = "artist.getsimilar",
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10
    ): LastFmArtistResponse

    @GET("2.0/")
    suspend fun getArtistInfo(
        @Query("method") method: String = "artist.getinfo",
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): LastFmArtistInfoResponse

    @GET("2.0/")
    suspend fun getTopTracks(
        @Query("method") method: String = "artist.gettoptracks",
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5
    ): LastFmTopTracksResponse
}

data class LastFmArtistInfoResponse(
    val artist: LastFmArtistInfo?
)

data class LastFmArtistInfo(
    val name: String,
    val bio: LastFmBio?,
    val tags: LastFmTags?,
    val stats: LastFmStats?
)

data class LastFmBio(
    val summary: String,
    val content: String
)

data class LastFmTags(
    val tag: List<LastFmTag>
)

data class LastFmTag(
    val name: String
)

data class LastFmStats(
    val listeners: String,
    val playcount: String
)

data class LastFmTopTracksResponse(
    val toptracks: LastFmTopTracks?
)

data class LastFmTopTracks(
    val track: List<LastFmTrack>
)

data class LastFmTrack(
    val name: String,
    val playcount: String,
    val listeners: String,
    val url: String
)