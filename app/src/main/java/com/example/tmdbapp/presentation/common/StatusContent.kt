package com.example.tmdbapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import com.example.tmdbapp.R

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MessageContent(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun ErrorContent(kind: ErrorKind, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (kind) {
        ErrorKind.Network -> R.string.error_network
        ErrorKind.Unauthorized -> R.string.error_unauthorized
        ErrorKind.Server -> R.string.error_server
        ErrorKind.Unknown -> R.string.error_unknown
    }
    MessageContent(stringResource(message), modifier, onRetry)
}

@Composable
fun PagingStatus(state: LoadState, onRetry: () -> Unit) {
    when (state) {
        is LoadState.Loading -> LoadingContent()
        is LoadState.Error -> ErrorContent(state.error.toErrorKind(), onRetry)
        is LoadState.NotLoading -> Unit
    }
}
