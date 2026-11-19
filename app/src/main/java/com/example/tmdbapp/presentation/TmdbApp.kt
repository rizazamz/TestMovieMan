package com.example.tmdbapp.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tmdbapp.presentation.detail.MovieDetailDestination
import com.example.tmdbapp.presentation.discover.DiscoverMoviesDestination
import com.example.tmdbapp.presentation.genre.GenreDestination
import com.example.tmdbapp.presentation.navigation.DiscoverMoviesRoute
import com.example.tmdbapp.presentation.navigation.GenreRoute
import com.example.tmdbapp.presentation.navigation.MovieDetailRoute

@Composable
fun TmdbApp() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        val navController = rememberNavController()
        NavHost(navController, startDestination = GenreRoute) {
            composable<GenreRoute> { entry ->
                GenreDestination(onGenreClick = { genre ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(DiscoverMoviesRoute(genre.id, genre.name)) { launchSingleTop = true }
                    }
                })
            }
            composable<DiscoverMoviesRoute> { entry ->
                DiscoverMoviesDestination(
                    onBack = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigateUp()
                    },
                    onMovieClick = { id ->
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(MovieDetailRoute(id)) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable<MovieDetailRoute> { entry ->
                MovieDetailDestination(onBack = {
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigateUp()
                })
            }
        }
    }
}
