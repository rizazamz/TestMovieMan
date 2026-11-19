package com.example.tmdbapp.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.tmdbapp.data.paging.DiscoverMoviesPagingSource
import com.example.tmdbapp.data.remote.TmdbApiService
import com.example.tmdbapp.data.remote.dto.MovieDto
import com.example.tmdbapp.data.remote.dto.MoviesResponseDto
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

class DiscoverMoviesPagingSourceTest {
    private val api = mockk<TmdbApiService>()
    private val source = DiscoverMoviesPagingSource(api, "28,12")

    @Test
    fun `first page has no previous key and forwards genres`() = runTest {
        coEvery { api.discoverMovies("28,12", 1) } returns page(1, 3)
        val result = source.load(refresh()) as PagingSource.LoadResult.Page
        assertEquals(listOf(1), result.data.map { it.id })
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
        coVerify(exactly = 1) { api.discoverMovies("28,12", 1) }
    }

    @Test
    fun `last page has no next key and restores refresh anchor`() = runTest {
        coEvery { api.discoverMovies("28,12", 3) } returns page(3, 3)
        val result = source.load(PagingSource.LoadParams.Append(3, 20, false)) as PagingSource.LoadResult.Page
        assertEquals(2, result.prevKey)
        assertNull(result.nextKey)
        assertEquals(3, source.getRefreshKey(PagingState(listOf(result), 0, PagingConfig(20), 0)))
    }

    @Test
    fun `empty response ends pagination`() = runTest {
        coEvery { api.discoverMovies("28,12", 1) } returns MoviesResponseDto(1, emptyList(), 0, 0)
        val result = source.load(refresh()) as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `connection failure becomes LoadResult Error`() = runTest {
        val failure = IOException("Offline")
        coEvery { api.discoverMovies("28,12", 1) } throws failure
        assertSame(failure, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `server error becomes LoadResult Error`() = runTest {
        val failure = serverError()
        coEvery { api.discoverMovies("28,12", 1) } throws failure
        val result = source.load(refresh()) as PagingSource.LoadResult.Error
        assertSame(failure, result.throwable)
        assertEquals(500, failure.code())
    }

    @Test
    fun `invalid JSON becomes LoadResult Error`() = runTest {
        val failure = SerializationException("Invalid JSON")
        coEvery { api.discoverMovies("28,12", 1) } throws failure
        assertSame(failure, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `cancellation is propagated`() = runTest {
        val cancellation = CancellationException("Screen left")
        coEvery { api.discoverMovies("28,12", 1) } throws cancellation
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
    private fun page(page: Int, totalPages: Int) = MoviesResponseDto(
        page, listOf(MovieDto(page, "Movie $page")), totalPages, totalPages,
    )
}
