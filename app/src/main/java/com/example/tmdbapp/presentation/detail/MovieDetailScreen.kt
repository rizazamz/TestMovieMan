package com.example.tmdbapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.presentation.common.ErrorContent
import com.example.tmdbapp.presentation.common.LoadingContent
import com.example.tmdbapp.presentation.common.MessageContent
import com.example.tmdbapp.presentation.common.ScreenScaffold
import com.example.tmdbapp.presentation.common.UiState

@Composable
fun MovieDetailDestination(onBack: () -> Unit, viewModel: MovieDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reviews = viewModel.reviews.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    MovieDetailScreen(
        state = state,
        reviews = reviews,
        listState = listState,
        onBack = onBack,
        onRetryDetail = viewModel::retryDetail,
        onRetryTrailers = viewModel::retryTrailers,
        onRetryReviews = reviews::retry,
        onPlayerError = viewModel::onPlayerError,
        onRetryPlayer = viewModel::retryPlayer,
        onPlaybackPosition = viewModel::onPlaybackPosition,
    )
}

@Composable
fun MovieDetailScreen(
    state: MovieDetailUiState,
    reviews: LazyPagingItems<MovieReview>,
    listState: LazyListState,
    onBack: () -> Unit,
    onRetryDetail: () -> Unit,
    onRetryTrailers: () -> Unit,
    onRetryReviews: () -> Unit,
    onPlayerError: () -> Unit,
    onRetryPlayer: () -> Unit,
    onPlaybackPosition: (Float) -> Unit,
) {
    val title = (state.detail as? UiState.Success)?.data?.title ?: stringResource(R.string.movie_detail)
    ScreenScaffold(title, onBack) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val detail = state.detail) {
            UiState.Loading -> LoadingContent(modifier)
            UiState.Empty -> MessageContent(stringResource(R.string.detail_empty), modifier, onRetryDetail)
            is UiState.Error -> ErrorContent(detail.kind, onRetryDetail, modifier)
            is UiState.Success -> LazyColumn(
                modifier = modifier,
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "movie-information", contentType = "detail") { MovieInformation(detail.data) }
                item(key = "trailer", contentType = "trailer") {
                    TrailerSection(
                        trailer = state.trailer,
                        playback = state.playback,
                        onRetry = onRetryTrailers,
                        onPlayerError = onPlayerError,
                        onRetryPlayer = onRetryPlayer,
                        onPlaybackPosition = onPlaybackPosition,
                    )
                }
                movieReviewItems(reviews, onRetryReviews)
            }
        }
    }
}
