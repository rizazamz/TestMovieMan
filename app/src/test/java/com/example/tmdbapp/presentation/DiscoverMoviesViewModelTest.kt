package com.example.tmdbapp.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.paging.LoadState
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.discover.DiscoverMoviesViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DiscoverMoviesViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun clearViewModels() {
        store.clear()
    }

    @Test
    fun `type safe arguments select the genre and paging survives reobservation`() = runTest {
        val movie = Movie(42, "Movie", "Overview", null, null, null, 8.0, 10, listOf(28))
        val repository = mockk<MovieRepository>()
        every { repository.discoverMovies("28") } returns flowOf(loadedPagingData(listOf(movie)))
        val arguments = SavedStateHandle(mapOf("genreId" to 28, "genreName" to "Action"))
        val viewModel = DiscoverMoviesViewModel(repository, arguments).also { store.put("discover", it) }

        assertEquals("Action", viewModel.genreName)
        assertEquals(listOf(movie), viewModel.movies.asSnapshot())
        assertEquals(listOf(movie), viewModel.movies.asSnapshot())
        verify(exactly = 1) { repository.discoverMovies("28") }
    }

    @Test
    fun `Paging emits Loading then Success with movies`() = runTest {
        val pending = CompletableDeferred<PagingSource.LoadResult<Int, Movie>>()
        val repository = mockk<MovieRepository>()
        every { repository.discoverMovies("28") } returns testPager { pending.await() }
        val viewModel = createViewModel(repository)
        val differ = presentPaging(viewModel.movies, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            val movie = Movie(42, "Movie", "Overview", null, null, null, 8.0, 10, listOf(28))
            pending.complete(PagingSource.LoadResult.Page(listOf(movie), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(listOf(movie), differ.snapshot().items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Paging emits Loading then empty completed state`() = runTest {
        val pending = CompletableDeferred<PagingSource.LoadResult<Int, Movie>>()
        val repository = mockk<MovieRepository>()
        every { repository.discoverMovies("28") } returns testPager { pending.await() }
        val differ = presentPaging(createViewModel(repository).movies, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            pending.complete(PagingSource.LoadResult.Page(emptyList(), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(0, differ.itemCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Paging emits Loading Error then retry Loading Success`() = runTest {
        val failure = IOException("Offline")
        val first = CompletableDeferred<PagingSource.LoadResult<Int, Movie>>()
        val second = CompletableDeferred<PagingSource.LoadResult<Int, Movie>>()
        var attempts = 0
        val repository = mockk<MovieRepository>()
        every { repository.discoverMovies("28") } returns testPager {
            if (attempts++ == 0) first.await() else second.await()
        }
        val differ = presentPaging(createViewModel(repository).movies, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            first.complete(PagingSource.LoadResult.Error(failure))
            assertSame(failure, (awaitItem().refresh as LoadState.Error).error)
            assertEquals(0, differ.itemCount)
            differ.retry()
            assertTrue(awaitItem().refresh is LoadState.Loading)
            val movie = Movie(42, "Recovered", "Overview", null, null, null, 8.0, 10, listOf(28))
            second.complete(PagingSource.LoadResult.Page(listOf(movie), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(listOf(movie), differ.snapshot().items)
            assertEquals(2, attempts)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { repository.discoverMovies("28") }
    }

    private fun createViewModel(repository: MovieRepository) = DiscoverMoviesViewModel(
        repository, SavedStateHandle(mapOf("genreId" to 28, "genreName" to "Action")),
    ).also { store.put("discover", it) }
}
