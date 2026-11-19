package com.example.tmdbapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.presentation.common.MovieArtwork

@Composable
fun MovieInformation(movie: MovieDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MovieArtwork(movie.backdropPath, null, Modifier.fillMaxWidth().aspectRatio(16f / 9f), backdrop = true)
        MovieSummary(movie)
        GenreLabels(movie.genres)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.synopsis), style = MaterialTheme.typography.titleLarge)
            Text(
                movie.overview.takeIf { it.isNotBlank() } ?: stringResource(R.string.synopsis_empty),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun MovieSummary(movie: MovieDetail) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MovieArtwork(
            movie.posterPath,
            stringResource(R.string.poster_description, movie.title),
            Modifier.width(104.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(movie.title, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.rating_value, movie.voteAverage), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.release_label, movie.releaseDate ?: stringResource(R.string.unavailable)),
                style = MaterialTheme.typography.bodyMedium,
            )
            val runtime = movie.runtime?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.runtime_minutes, it, it) }
                ?: stringResource(R.string.unavailable)
            Text(stringResource(R.string.runtime_label, runtime), style = MaterialTheme.typography.bodyMedium)
            movie.tagline?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreLabels(genres: List<Genre>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { genre ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(genre.name, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
