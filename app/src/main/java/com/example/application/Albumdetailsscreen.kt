package com.example.application

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.data.models.LastFmArtistInfo
import com.example.application.data.models.LastFmTrack
import com.example.application.data.models.SimilarArtist
import com.example.application.data.repository.MusicRecommendationRepository
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailsScreen(
    release: Release,
    onBack: () -> Unit
) {
    var similarArtists by remember { mutableStateOf<List<SimilarArtist>>(emptyList()) }
    var artistInfo by remember { mutableStateOf<LastFmArtistInfo?>(null) }
    var topTracks by remember { mutableStateOf<List<LastFmTrack>>(emptyList()) }
    var isLoadingRecommendations by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val recommendationRepo = remember { MusicRecommendationRepository() }

    // Load recommendations when screen opens
    LaunchedEffect(release.artists.firstOrNull()?.name) {
        release.artists.firstOrNull()?.name?.let { artistName ->
            isLoadingRecommendations = true

            // Get similar artists
            recommendationRepo.getSimilarArtists(artistName).fold(
                onSuccess = { artists ->
                    similarArtists = artists
                    Log.d("Recommendations", "Found ${artists.size} similar artists")
                },
                onFailure = { error ->
                    Log.e("Recommendations", "Failed to get similar artists", error)
                }
            )

            // Get artist info
            recommendationRepo.getArtistInfo(artistName).fold(
                onSuccess = { info ->
                    artistInfo = info
                    Log.d("Recommendations", "Got artist info for $artistName")
                },
                onFailure = { error ->
                    Log.e("Recommendations", "Failed to get artist info", error)
                }
            )

            // Get top tracks
            recommendationRepo.getTopTracks(artistName).fold(
                onSuccess = { tracks ->
                    topTracks = tracks
                    Log.d("Recommendations", "Found ${tracks.size} top tracks")
                },
                onFailure = { error ->
                    Log.e("Recommendations", "Failed to get top tracks", error)
                }
            )

            isLoadingRecommendations = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Album Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = release.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    release.year?.let {
                        Text(
                            text = "Released: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    release.genres?.let { genres ->
                        Text(
                            text = "Genres: ${genres.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pricing info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        release.lowest_price?.let {
                            Text(
                                text = "From: $$it",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        release.num_for_sale?.let {
                            Text(
                                text = "$it for sale",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Community stats
                    release.community?.let { community ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            community.rating?.let { rating ->
                                Text(
                                    text = "⭐ ${String.format("%.1f", rating.average)} (${rating.count} ratings)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "❤️ ${community.want} want | ✓ ${community.have} have",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Artist Bio
        item {
            artistInfo?.bio?.summary?.let { bio ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About the Artist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bio.replace(Regex("<.*?>"), "").take(300) + "...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Top Tracks
        if (topTracks.isNotEmpty()) {
            item {
                Text(
                    text = "🔥 Top Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(topTracks.take(5)) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { /* Could open Spotify/YouTube here */ }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${track.playcount} plays",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Similar Artists Section
        if (similarArtists.isNotEmpty()) {
            item {
                Text(
                    text = "🎸 Similar Artists You Might Like",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(similarArtists.take(10)) { artist ->
                SimilarArtistCard(
                    artist = artist,
                    onClick = {
                        // Could search Discogs for this artist's albums
                        Log.d("Recommendations", "Clicked on ${artist.name}")
                    }
                )
            }
        } else if (isLoadingRecommendations) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Tracklist
        release.tracklist?.let { tracks ->
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📀 Tracklist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(tracks) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = track.position,
                            modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            track.duration?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Back button at bottom
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Search")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SimilarArtistCard(
    artist: SimilarArtist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artist image placeholder
            Surface(
                modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = artist.name.take(1),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(artist.match.toDoubleOrNull()?.times(100))?.toInt() ?: 0}% match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = "→",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}