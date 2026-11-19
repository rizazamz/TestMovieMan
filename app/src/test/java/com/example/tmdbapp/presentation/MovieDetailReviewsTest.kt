package com.example.tmdbapp.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.paging.LoadState
import androidx.paging.PagingSource
import app.cash.turbine.test
import com.example.tmdbapp.data.serverError
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.detail.MovieDetailViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
class MovieDetailReviewsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<MovieRepository>()
    private val store = ViewModelStore()
    private val review = MovieReview("review", "Author", null, null, 8.0, "Review", null, null, null)

    @After
    fun clearViewModels() {
        store.clear()
    }

    @Test
    fun `reviews emit Loading then Success for selected movie`() = runTest {
        val pending = CompletableDeferred<PagingSource.LoadResult<Int, MovieReview>>()
        val viewModel = viewModel { pending.await() }
        val differ = presentPaging(viewModel.reviews, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            pending.complete(PagingSource.LoadResult.Page(listOf(review), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(listOf(review), differ.snapshot().items)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { repository.getMovieReviews(42) }
    }

    @Test
    fun `reviews emit Loading then empty completed state`() = runTest {
        val pending = CompletableDeferred<PagingSource.LoadResult<Int, MovieReview>>()
        val differ = presentPaging(viewModel { pending.await() }.reviews, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            pending.complete(PagingSource.LoadResult.Page(emptyList(), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(0, differ.itemCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reviews emit Loading server Error then retry Loading Success`() = runTest {
        val failure = serverError()
        val first = CompletableDeferred<PagingSource.LoadResult<Int, MovieReview>>()
        val second = CompletableDeferred<PagingSource.LoadResult<Int, MovieReview>>()
        var attempts = 0
        val viewModel = viewModel { if (attempts++ == 0) first.await() else second.await() }
        val differ = presentPaging(viewModel.reviews, mainDispatcher.dispatcher)
        differ.loadStateFlow.test {
            assertTrue(awaitItem().refresh is LoadState.Loading)
            first.complete(PagingSource.LoadResult.Error(failure))
            assertSame(failure, (awaitItem().refresh as LoadState.Error).error)
            differ.retry()
            assertTrue(awaitItem().refresh is LoadState.Loading)
            second.complete(PagingSource.LoadResult.Page(listOf(review), null, null))
            assertTrue(awaitItem().refresh is LoadState.NotLoading)
            assertEquals(listOf(review), differ.snapshot().items)
            assertEquals(2, attempts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(load: suspend () -> PagingSource.LoadResult<Int, MovieReview>): MovieDetailViewModel {
        coEvery { repository.getMovieDetail(42) } returns Result.success(mockk<MovieDetail>())
        coEvery { repository.getMovieTrailer(42) } returns Result.success(null)
        every { repository.getMovieReviews(42) } returns testPager(load)
        return MovieDetailViewModel(repository, SavedStateHandle(mapOf("movieId" to 42)))
            .also { store.put("detail", it) }
    }
}
