package com.example.tmdbapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.tmdbapp.R
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.presentation.common.MessageContent
import com.example.tmdbapp.presentation.common.PagingStatus

fun LazyListScope.movieReviewItems(reviews: LazyPagingItems<MovieReview>, onRetry: () -> Unit) {
    item(key = "reviews-heading", contentType = "heading") {
        Text(stringResource(R.string.reviews), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
    }
    item(key = "reviews-refresh", contentType = "status") {
        val refresh = reviews.loadState.refresh
        if (reviews.itemCount == 0 && refresh is LoadState.NotLoading) {
            MessageContent(stringResource(R.string.reviews_empty))
        } else {
            PagingStatus(refresh, onRetry)
        }
    }
    items(reviews.itemCount, key = reviews.itemKey { "review-${it.id}" }, contentType = { "review" }) { index ->
        reviews[index]?.let { ReviewCard(it) }
    }
    item(key = "reviews-append", contentType = "status") { PagingStatus(reviews.loadState.append, onRetry) }
}

@Composable
private fun ReviewCard(review: MovieReview) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                review.author.ifBlank {
                    review.username?.takeIf { it.isNotBlank() } ?: stringResource(R.string.anonymous_reviewer)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            review.rating?.let {
                Text(stringResource(R.string.rating_value, it), style = MaterialTheme.typography.labelLarge)
            }
            Text(review.content, style = MaterialTheme.typography.bodyMedium)
            review.createdAt?.let {
                Text(it.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
