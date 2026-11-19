package com.example.tmdbapp.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.tmdbapp.data.paging.MovieReviewsPagingSource
import com.example.tmdbapp.data.remote.TmdbApiService
import com.example.tmdbapp.data.remote.dto.MovieReviewDto
import com.example.tmdbapp.data.remote.dto.ReviewsResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieReviewsPagingSourceTest {
    private val api = mockk<TmdbApiService>()
    private val source = MovieReviewsPagingSource(api, 42)

    @Test
    fun `first page has no previous key and retains movie id`() = runTest {
        coEvery { api.getMovieReviews(42, 1) } returns page(1, 2)
        val result = source.load(refresh()) as PagingSource.LoadResult.Page
        assertEquals("review-1", result.data.single().id)
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
        coVerify(exactly = 1) { api.getMovieReviews(42, 1) }
    }

    @Test
    fun `last page has no next key and restores refresh anchor`() = runTest {
        coEvery { api.getMovieReviews(42, 2) } returns page(2, 2)
        val result = source.load(PagingSource.LoadParams.Append(2, 20, false)) as PagingSource.LoadResult.Page
        assertEquals(1, result.prevKey)
        assertNull(result.nextKey)
        assertEquals(2, source.getRefreshKey(PagingState(listOf(result), 0, PagingConfig(20), 0)))
    }

    @Test
    fun `empty response ends pagination`() = runTest {
        coEvery { api.getMovieReviews(42, 1) } returns ReviewsResponseDto(1, emptyList(), 0, 0)
        val result = source.load(refresh()) as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `connection failure becomes LoadResult Error`() = runTest {
        val failure = IOException("Offline")
        coEvery { api.getMovieReviews(42, 1) } throws failure
        assertSame(failure, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `server error becomes LoadResult Error`() = runTest {
        val failure = serverError()
        coEvery { api.getMovieReviews(42, 1) } throws failure
        val result = source.load(refresh()) as PagingSource.LoadResult.Error
        assertSame(failure, result.throwable)
        assertEquals(500, failure.code())
    }

    @Test
    fun `invalid JSON becomes LoadResult Error`() = runTest {
        val failure = SerializationException("Invalid JSON")
        coEvery { api.getMovieReviews(42, 1) } throws failure
        assertSame(failure, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `cancellation is propagated`() = runTest {
        val cancellation = CancellationException("Screen left")
        coEvery { api.getMovieReviews(42, 1) } throws cancellation
        try {
            source.load(refresh())
            throw AssertionError("Cancellation swallowed")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
    }

    @Test
    fun `refresh without an anchor returns null`() {
        assertNull(source.getRefreshKey(PagingState(emptyList(), null, PagingConfig(20), 0)))
    }

    private fun refresh() = PagingSource.LoadParams.Refresh<Int>(null, 20, false)
    private fun page(page: Int, totalPages: Int) = ReviewsResponseDto(
        page, listOf(MovieReviewDto("review-$page", "Reviewer", content = "Review $page")), totalPages, totalPages,
    )
}
