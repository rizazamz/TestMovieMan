package com.example.tmdbapp.presentation.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.presentation.common.ErrorContent
import com.example.tmdbapp.presentation.common.LoadingContent
import com.example.tmdbapp.presentation.common.MessageContent
import com.example.tmdbapp.presentation.common.PagingStatus
import com.example.tmdbapp.presentation.common.ScreenScaffold
import com.example.tmdbapp.presentation.common.toErrorKind

@Composable
fun DiscoverMoviesDestination(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: DiscoverMoviesViewModel = hiltViewModel(),
) {
    val movies = viewModel.movies.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    DiscoverMoviesScreen(viewModel.genreName, movies, listState, onBack, onMovieClick, movies::retry)
}

@Composable
fun DiscoverMoviesScreen(
    genreName: String,
    movies: LazyPagingItems<Movie>,
    listState: LazyListState,
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    ScreenScaffold(genreName, onBack) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        val refresh = movies.loadState.refresh
        when {
            movies.itemCount == 0 && refresh is LoadState.Loading -> LoadingContent(modifier)
            movies.itemCount == 0 && refresh is LoadState.Error ->
                ErrorContent(refresh.error.toErrorKind(), onRetry, modifier)
            movies.itemCount == 0 -> MessageContent(stringResource(R.string.movies_empty), modifier)
            else -> MoviesList(movies, listState, onMovieClick, onRetry, modifier)
        }
    }
}

@Composable
private fun MoviesList(
    movies: LazyPagingItems<Movie>,
    listState: LazyListState,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "refresh", contentType = "status") { PagingStatus(movies.loadState.refresh, onRetry) }
        items(movies.itemCount, key = movies.itemKey { it.id }, contentType = { "movie" }) { index ->
            movies[index]?.let { movie -> MovieCard(movie, onClick = { onMovieClick(movie.id) }) }
        }
        item(key = "append", contentType = "status") { PagingStatus(movies.loadState.append, onRetry) }
    }
}
