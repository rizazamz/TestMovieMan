package com.example.tmdbapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.common.UiState
import com.example.tmdbapp.presentation.common.toErrorKind
import com.example.tmdbapp.presentation.navigation.MovieDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val movieId = savedStateHandle.toRoute<MovieDetailRoute>().movieId
    private val mutableState = MutableStateFlow(
        MovieDetailUiState(playback = TrailerPlaybackState(savedStateHandle[POSITION_KEY] ?: 0f)),
    )
    val state = mutableState.asStateFlow()
    val reviews = repository.getMovieReviews(movieId).cachedIn(viewModelScope)
    private var detailJob: Job? = null
    private var trailersJob: Job? = null

    init {
        retryDetail()
        retryTrailers()
    }

    fun retryDetail() {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            mutableState.update { it.copy(detail = UiState.Loading) }
            val result = repository.getMovieDetail(movieId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorKind()) },
            )
            mutableState.update { it.copy(detail = result) }
        }
    }

    fun retryTrailers() {
        trailersJob?.cancel()
        trailersJob = viewModelScope.launch {
            mutableState.update { it.copy(trailer = UiState.Loading) }
            val result = repository.getMovieTrailer(movieId).fold(
                onSuccess = { if (it == null) UiState.Empty else UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorKind()) },
            )
            mutableState.update { it.copy(trailer = result) }
        }
    }

    fun onPlaybackPosition(seconds: Float) {
        if (!seconds.isFinite() || seconds < 0f || abs(seconds - state.value.playback.positionSeconds) < 1f) return
        savedStateHandle[POSITION_KEY] = seconds
        mutableState.update { it.copy(playback = it.playback.copy(positionSeconds = seconds)) }
    }

    fun onPlayerError() {
        mutableState.update { it.copy(playback = it.playback.copy(hasError = true)) }
    }

    fun retryPlayer() {
        mutableState.update { it.copy(playback = it.playback.copy(hasError = false)) }
    }

    private companion object {
        const val POSITION_KEY = "trailerPositionSeconds"
    }
}
