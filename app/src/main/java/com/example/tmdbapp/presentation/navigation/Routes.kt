package com.example.tmdbapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object GenreRoute

@Serializable
data class DiscoverMoviesRoute(val genreId: Int, val genreName: String)

@Serializable
data class MovieDetailRoute(val movieId: Int)
