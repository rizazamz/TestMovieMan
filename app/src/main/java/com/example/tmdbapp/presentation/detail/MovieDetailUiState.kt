package com.example.tmdbapp.presentation.detail

import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieVideo
import com.example.tmdbapp.presentation.common.UiState

data class MovieDetailUiState(
    val detail: UiState<MovieDetail> = UiState.Loading,
    val trailer: UiState<MovieVideo> = UiState.Loading,
    val playback: TrailerPlaybackState = TrailerPlaybackState(),
)

data class TrailerPlaybackState(val positionSeconds: Float = 0f, val hasError: Boolean = false)
