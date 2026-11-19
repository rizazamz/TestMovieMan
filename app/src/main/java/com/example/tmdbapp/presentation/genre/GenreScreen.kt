package com.example.tmdbapp.presentation.genre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.presentation.common.ErrorContent
import com.example.tmdbapp.presentation.common.LoadingContent
import com.example.tmdbapp.presentation.common.MessageContent
import com.example.tmdbapp.presentation.common.ScreenScaffold
import com.example.tmdbapp.presentation.common.UiState

@Composable
fun GenreDestination(onGenreClick: (Genre) -> Unit, viewModel: GenreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    GenreScreen(state, gridState, viewModel::retry, onGenreClick)
}

@Composable
fun GenreScreen(
    state: UiState<List<Genre>>,
    gridState: LazyGridState,
    onRetry: () -> Unit,
    onGenreClick: (Genre) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.app_name)) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (state) {
            UiState.Loading -> LoadingContent(modifier)
            UiState.Empty -> MessageContent(stringResource(R.string.genres_empty), modifier, onRetry)
            is UiState.Error -> ErrorContent(state.kind, onRetry, modifier)
            is UiState.Success -> GenreGrid(state.data, gridState, onGenreClick, modifier)
        }
    }
}

@Composable
private fun GenreGrid(
    genres: List<Genre>,
    gridState: LazyGridState,
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(R.string.choose_genre),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(152.dp),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(genres, key = { it.id }) { genre ->
                GenreCard(genre, onClick = { onGenreClick(genre) })
            }
        }
    }
}

@Composable
private fun GenreCard(genre: Genre, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(genre.name, Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}
