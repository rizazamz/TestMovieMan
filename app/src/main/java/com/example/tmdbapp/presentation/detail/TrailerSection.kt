package com.example.tmdbapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.MovieVideo
import com.example.tmdbapp.presentation.common.ErrorContent
import com.example.tmdbapp.presentation.common.LoadingContent
import com.example.tmdbapp.presentation.common.MessageContent
import com.example.tmdbapp.presentation.common.UiState

@Composable
fun TrailerSection(
    trailer: UiState<MovieVideo>,
    playback: TrailerPlaybackState,
    onRetry: () -> Unit,
    onPlayerError: () -> Unit,
    onRetryPlayer: () -> Unit,
    onPlaybackPosition: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.trailer), style = MaterialTheme.typography.titleLarge)
        when (trailer) {
            UiState.Loading -> LoadingContent()
            UiState.Empty -> MessageContent(stringResource(R.string.trailer_empty))
            is UiState.Error -> ErrorContent(trailer.kind, onRetry)
            is UiState.Success -> {
                val video = trailer.data
                if (playback.hasError) {
                    MessageContent(stringResource(R.string.trailer_playback_error), onRetry = onRetryPlayer)
                } else {
                    Text(video.name, style = MaterialTheme.typography.bodyMedium)
                    YouTubeTrailerPlayer(video.key, playback.positionSeconds, onPlaybackPosition, onPlayerError)
                }
            }
        }
    }
}
