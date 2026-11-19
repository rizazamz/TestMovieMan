package com.example.tmdbapp.presentation.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.navigation.DiscoverMoviesRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiscoverMoviesViewModel @Inject constructor(
    repository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DiscoverMoviesRoute>()
    val genreName = route.genreName
    val movies = repository.discoverMovies(route.genreId.toString()).cachedIn(viewModelScope)
}
