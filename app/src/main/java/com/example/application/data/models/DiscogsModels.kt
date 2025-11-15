package com.example.application

data class SearchResponse(
    val results: List<SearchResult>,
    val pagination: Pagination
)

data class SearchResult(
    val id: Long,
    val type: String,
    val title: String,
    val cover_image: String?,
    val thumb: String?,
    val year: String?,
    val format: List<String>?,
    val label: List<String>?,
    val genre: List<String>?,
    val style: List<String>?,
    val country: String?,
    val master_id: Long?,
    val master_url: String?
)

data class Pagination(
    val page: Int,
    val pages: Int,
    val per_page: Int,
    val items: Int
)

data class Release(
    val id: Long,
    val title: String,
    val artists: List<Artist>,
    val year: Int?,
    val released: String?,
    val genres: List<String>?,
    val styles: List<String>?,
    val tracklist: List<Track>?,
    val images: List<Image>?,
    val labels: List<Label>?,
    val formats: List<Format>?,
    val country: String?,
    val notes: String?,
    val uri: String,
    val videos: List<Video>?,
    val community: Community?,
    val lowest_price: Double?,
    val num_for_sale: Int?
)

data class Artist(
    val name: String,
    val id: Long,
    val resource_url: String
)

data class Track(
    val position: String,
    val title: String,
    val duration: String?,
    val type_: String?
)

data class Image(
    val uri: String,
    val height: Int,
    val width: Int,
    val type: String
)

data class Label(
    val name: String,
    val catno: String?,
    val id: Long
)

data class Format(
    val name: String,
    val qty: String,
    val descriptions: List<String>?
)

data class Video(
    val uri: String,
    val title: String,
    val description: String?,
    val duration: Int
)

data class Community(
    val rating: Rating?,
    val want: Int,
    val have: Int
)

data class Rating(
    val average: Double,
    val count: Int
)