package com.example.tmdbapp.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.model.MovieVideo
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.common.ErrorKind
import com.example.tmdbapp.presentation.common.UiState
import com.example.tmdbapp.presentation.detail.MovieDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MovieDetailViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<MovieRepository>()
    private val store = ViewModelStore()
    private val movie = mockk<MovieDetail>()

    @After
    fun clearViewModels() {
        store.clear()
    }

    @Test
    fun `detail and trailers load independently and detail retry preserves trailers`() = runTest {
        val pending = CompletableDeferred<Result<MovieDetail>>()
        val trailer = MovieVideo("video", "key", "Trailer", "YouTube", "Trailer", true, null)
        coEvery { repository.getMovieDetail(42) } coAnswers { pending.await() }
        coEvery { repository.getMovieTrailer(42) } returns Result.success(trailer)
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val viewModel = viewModel()
        runCurrent()
        assertEquals(UiState.Loading, viewModel.state.value.detail)
        assertEquals(UiState.Success(trailer), viewModel.state.value.trailer)

        pending.complete(Result.failure(IOException("Offline")))
        runCurrent()
        assertEquals(UiState.Error(ErrorKind.Network), viewModel.state.value.detail)
        coEvery { repository.getMovieDetail(42) } returns Result.success(movie)
        viewModel.retryDetail()
        runCurrent()
        assertEquals(UiState.Success(movie), viewModel.state.value.detail)
        coVerify(exactly = 1) { repository.getMovieTrailer(42) }
    }

    @Test
    fun `trailer failure can retry to empty without hiding movie or reviews`() = runTest {
        coEvery { repository.getMovieDetail(42) } returns Result.success(movie)
        coEvery { repository.getMovieTrailer(42) } returnsMany listOf(
            Result.failure(IOException("Offline")), Result.success(null),
        )
        val review = MovieReview("review", "Author", null, null, null, "Review", null, null, null)
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(listOf(review)))
        val viewModel = viewModel()
        runCurrent()
        assertEquals(UiState.Error(ErrorKind.Network), viewModel.state.value.trailer)
        assertEquals(listOf(review), viewModel.reviews.asSnapshot())
        viewModel.retryTrailers()
        runCurrent()

        assertEquals(UiState.Empty, viewModel.state.value.trailer)
        assertEquals(UiState.Success(movie), viewModel.state.value.detail)
        coVerify(exactly = 1) { repository.getMovieDetail(42) }
    }

    @Test
    fun `playback position restores from saved state and invalid positions are ignored`() = runTest {
        coEvery { repository.getMovieDetail(42) } returns Result.success(movie)
        coEvery { repository.getMovieTrailer(42) } returns Result.success(null)
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val savedState = SavedStateHandle(mapOf("movieId" to 42, "trailerPositionSeconds" to 18f))
        val viewModel = viewModel(savedState)
        assertEquals(18f, viewModel.state.value.playback.positionSeconds)
        viewModel.onPlaybackPosition(37f)
        viewModel.onPlaybackPosition(Float.NaN)
        viewModel.onPlaybackPosition(-1f)
        assertEquals(37f, savedState.get<Float>("trailerPositionSeconds"))
        viewModel.onPlayerError()
        assertTrue(viewModel.state.value.playback.hasError)
        viewModel.retryPlayer()
        assertFalse(viewModel.state.value.playback.hasError)

        val restored = viewModel(savedState)
        assertEquals(37f, restored.state.value.playback.positionSeconds)
        runCurrent()
    }

    @Test
    fun `detail emits Loading then Success`() = runTest {
        val pending = CompletableDeferred<Result<MovieDetail>>()
        coEvery { repository.getMovieDetail(42) } coAnswers { pending.await() }
        coEvery { repository.getMovieTrailer(42) } returns Result.success(null)
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val viewModel = viewModel()
        viewModel.state.map { it.detail }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            pending.complete(Result.success(movie))
            assertEquals(UiState.Success(movie), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail emits Loading Error and retry Loading Success`() = runTest {
        val recovered = CompletableDeferred<Result<MovieDetail>>()
        var attempts = 0
        coEvery { repository.getMovieDetail(42) } coAnswers {
            if (attempts++ == 0) Result.failure(IOException("Offline")) else recovered.await()
        }
        coEvery { repository.getMovieTrailer(42) } returns Result.success(null)
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val viewModel = viewModel()
        viewModel.state.map { it.detail }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(ErrorKind.Network), awaitItem())
            viewModel.retryDetail()
            assertEquals(UiState.Loading, awaitItem())
            recovered.complete(Result.success(movie))
            assertEquals(UiState.Success(movie), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 2) { repository.getMovieDetail(42) }
    }

    @Test
    fun `missing trailer emits Loading then Empty and reviews can also be empty`() = runTest {
        coEvery { repository.getMovieDetail(42) } returns Result.success(movie)
        val pending = CompletableDeferred<Result<MovieVideo?>>()
        coEvery { repository.getMovieTrailer(42) } coAnswers { pending.await() }
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val viewModel = viewModel()
        viewModel.state.map { it.trailer }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            pending.complete(Result.success(null))
            assertEquals(UiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.reviews.asSnapshot().isEmpty())
    }

    @Test
    fun `trailer emits Loading Error and retry Loading Success without reloading detail`() = runTest {
        coEvery { repository.getMovieDetail(42) } returns Result.success(movie)
        val recovered = CompletableDeferred<Result<MovieVideo?>>()
        var attempts = 0
        coEvery { repository.getMovieTrailer(42) } coAnswers {
            if (attempts++ == 0) Result.failure(IOException("Offline")) else recovered.await()
        }
        every { repository.getMovieReviews(42) } returns flowOf(loadedPagingData(emptyList()))
        val viewModel = viewModel()
        val trailer = MovieVideo("official", "key", "Trailer", "YouTube", "Trailer", true, null)
        viewModel.state.map { it.trailer }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(ErrorKind.Network), awaitItem())
            viewModel.retryTrailers()
            assertEquals(UiState.Loading, awaitItem())
            recovered.complete(Result.success(trailer))
            assertEquals(UiState.Success(trailer), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 2) { repository.getMovieTrailer(42) }
        coVerify(exactly = 1) { repository.getMovieDetail(42) }
    }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle(mapOf("movieId" to 42))) =
        MovieDetailViewModel(repository, savedState).also { store.put("detail-${store.keys().size}", it) }
}
