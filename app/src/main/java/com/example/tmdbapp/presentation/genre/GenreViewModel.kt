package com.example.tmdbapp.presentation.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.common.UiState
import com.example.tmdbapp.presentation.common.toErrorKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<List<Genre>>>(UiState.Loading)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = UiState.Loading
            mutableState.value = repository.getGenres().fold(
                onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorKind()) },
            )
        }
    }
}
