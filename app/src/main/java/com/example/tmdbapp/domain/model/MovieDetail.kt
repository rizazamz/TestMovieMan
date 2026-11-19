package com.example.tmdbapp.domain.model

data class MovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<Genre>,
    val runtime: Int?,
    val tagline: String?,
    val status: String?,
    val homepage: String?,
    val imdbId: String?,
    val originalLanguage: String?,
    val budget: Long,
    val revenue: Long,
)
