package com.example.application

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity

/**
 * Spotify Deep Link Handler
 * Opens Spotify app or web player for artists and tracks
 */
object SpotifyHelper {

    /**
     * Opens an artist in Spotify
     * @param context Android context
     * @param artistName Name of the artist to search for
     */
    fun openArtistInSpotify(context: Context, artistName: String) {
        // Try to open in Spotify app first
        val spotifyAppUri = "spotify:search:${Uri.encode(artistName)}"
        val spotifyAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyAppUri))

        // Check if Spotify app is installed
        if (isSpotifyInstalled(context)) {
            try {
                context.startActivity(spotifyAppIntent)
                Log.d("Spotify", "Opened artist in Spotify app: $artistName")
            } catch (e: Exception) {
                Log.e("Spotify", "Failed to open Spotify app", e)
                openArtistInSpotifyWeb(context, artistName)
            }
        } else {
            // Fallback to web player
            openArtistInSpotifyWeb(context, artistName)
        }
    }

    /**
     * Opens a track in Spotify
     * @param context Android context
     * @param trackName Name of the track
     * @param artistName Name of the artist (helps with search accuracy)
     */
    fun openTrackInSpotify(context: Context, trackName: String, artistName: String) {
        // Search for "track artist" for better results
        val searchQuery = "$trackName $artistName"
        val spotifyAppUri = "spotify:search:${Uri.encode(searchQuery)}"
        val spotifyAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyAppUri))

        if (isSpotifyInstalled(context)) {
            try {
                context.startActivity(spotifyAppIntent)
                Log.d("Spotify", "Opened track in Spotify app: $searchQuery")
            } catch (e: Exception) {
                Log.e("Spotify", "Failed to open Spotify app", e)
                openTrackInSpotifyWeb(context, trackName, artistName)
            }
        } else {
            openTrackInSpotifyWeb(context, trackName, artistName)
        }
    }

    /**
     * Opens an album in Spotify
     * @param context Android context
     * @param albumName Name of the album
     * @param artistName Name of the artist
     */
    fun openAlbumInSpotify(context: Context, albumName: String, artistName: String) {
        val searchQuery = "$albumName $artistName"
        val spotifyAppUri = "spotify:search:${Uri.encode(searchQuery)}"
        val spotifyAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyAppUri))

        if (isSpotifyInstalled(context)) {
            try {
                context.startActivity(spotifyAppIntent)
                Log.d("Spotify", "Opened album in Spotify app: $searchQuery")
            } catch (e: Exception) {
                Log.e("Spotify", "Failed to open Spotify app", e)
                openAlbumInSpotifyWeb(context, albumName, artistName)
            }
        } else {
            openAlbumInSpotifyWeb(context, albumName, artistName)
        }
    }

    /**
     * Check if Spotify app is installed
     */
    private fun isSpotifyInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.spotify.music", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fallback: Open artist in Spotify web player
     */
    private fun openArtistInSpotifyWeb(context: Context, artistName: String) {
        val webUrl = "https://open.spotify.com/search/${Uri.encode(artistName)}"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        try {
            context.startActivity(webIntent)
            Log.d("Spotify", "Opened artist in Spotify web: $artistName")
        } catch (e: Exception) {
            Log.e("Spotify", "Failed to open Spotify web", e)
        }
    }

    /**
     * Fallback: Open track in Spotify web player
     */
    private fun openTrackInSpotifyWeb(context: Context, trackName: String, artistName: String) {
        val searchQuery = "$trackName $artistName"
        val webUrl = "https://open.spotify.com/search/${Uri.encode(searchQuery)}"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        try {
            context.startActivity(webIntent)
            Log.d("Spotify", "Opened track in Spotify web: $searchQuery")
        } catch (e: Exception) {
            Log.e("Spotify", "Failed to open Spotify web", e)
        }
    }

    /**
     * Fallback: Open album in Spotify web player
     */
    private fun openAlbumInSpotifyWeb(context: Context, albumName: String, artistName: String) {
        val searchQuery = "$albumName $artistName"
        val webUrl = "https://open.spotify.com/search/${Uri.encode(searchQuery)}"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        try {
            context.startActivity(webIntent)
            Log.d("Spotify", "Opened album in Spotify web: $searchQuery")
        } catch (e: Exception) {
            Log.e("Spotify", "Failed to open Spotify web", e)
        }
    }

    /**
     * Prompt user to install Spotify
     */
    fun promptInstallSpotify(context: Context) {
        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music")
        )
        try {
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            Log.e("Spotify", "Failed to open Play Store", e)
        }
    }
}