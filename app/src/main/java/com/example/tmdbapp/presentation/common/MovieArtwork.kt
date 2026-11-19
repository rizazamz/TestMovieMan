package com.example.tmdbapp.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.tmdbapp.R

@Composable
fun MovieArtwork(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    backdrop: Boolean = false,
) {
    val size = if (backdrop) "w780" else "w342"
    val placeholder = painterResource(R.drawable.image_placeholder)
    AsyncImage(
        model = path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" },
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
