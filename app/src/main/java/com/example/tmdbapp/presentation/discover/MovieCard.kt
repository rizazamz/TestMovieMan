package com.example.tmdbapp.presentation.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.presentation.common.MovieArtwork

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MovieArtwork(movie.posterPath, movie.title, Modifier.width(104.dp).height(156.dp))
            Column(
                modifier = Modifier.weight(1f).padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.rating_value, movie.voteAverage), style = MaterialTheme.typography.labelLarge)
                Text(movie.releaseDate ?: stringResource(R.string.release_unavailable), style = MaterialTheme.typography.bodySmall)
                if (movie.overview.isNotBlank()) {
                    Text(movie.overview, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
