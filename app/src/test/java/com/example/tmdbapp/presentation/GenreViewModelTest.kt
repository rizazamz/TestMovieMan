package com.example.tmdbapp.presentation

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.repository.MovieRepository
import com.example.tmdbapp.presentation.common.ErrorKind
import com.example.tmdbapp.presentation.common.UiState
import com.example.tmdbapp.presentation.genre.GenreViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenreViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<MovieRepository>()
    private val store = ViewModelStore()

    @After
    fun clearViewModels() {
        store.clear()
    }

    @Test
    fun `emits Loading then Success`() = runTest {
        val genres = listOf(Genre(28, "Action"))
        assertTransition(Result.success(genres), UiState.Success(genres))
    }

    @Test
    fun `emits Loading then Error on network failure`() = runTest {
        assertTransition(Result.failure(IOException("Offline")), UiState.Error(ErrorKind.Network))
    }

    @Test
    fun `emits Loading then Empty when no genres exist`() = runTest {
        assertTransition(Result.success(emptyList()), UiState.Empty)
    }

    @Test
    fun `retry emits Loading again before successful recovery`() = runTest {
        val recovered = CompletableDeferred<Result<List<Genre>>>()
        var attempt = 0
        coEvery { repository.getGenres() } coAnswers {
            if (attempt++ == 0) Result.failure(IOException("Offline")) else recovered.await()
        }
        val viewModel = GenreViewModel(repository).also { store.put("genre", it) }
        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error(ErrorKind.Network), awaitItem())
            viewModel.retry()
            assertEquals(UiState.Loading, awaitItem())
            val genres = listOf(Genre(28, "Action"))
            recovered.complete(Result.success(genres))
            assertEquals(UiState.Success(genres), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 2) { repository.getGenres() }
    }

    @Test
    fun `genre state supports loading empty error and successful retry`() = runTest {
        coEvery { repository.getGenres() } returnsMany listOf(
            Result.success(emptyList()),
            Result.failure(IOException("Offline")),
            Result.success(listOf(Genre(28, "Action"))),
        )
        val viewModel = GenreViewModel(repository).also { store.put("genre", it) }
        assertEquals(UiState.Loading, viewModel.state.value)
        runCurrent()
        assertEquals(UiState.Empty, viewModel.state.value)

        viewModel.retry()
        runCurrent()
        assertEquals(UiState.Error(ErrorKind.Network), viewModel.state.value)
        viewModel.retry()
        runCurrent()
        assertEquals(UiState.Success(listOf(Genre(28, "Action"))), viewModel.state.value)
    }

    @Test
    fun `retry cancels the previous request without overriding the new state`() = runTest {
        var requestCount = 0
        coEvery { repository.getGenres() } coAnswers {
            requestCount++
            if (requestCount == 1) awaitCancellation() else Result.success(listOf(Genre(28, "Action")))
        }
        val viewModel = GenreViewModel(repository).also { store.put("genre", it) }
        runCurrent()
        viewModel.retry()
        runCurrent()

        assertEquals(UiState.Success(listOf(Genre(28, "Action"))), viewModel.state.value)
        coVerify(exactly = 2) { repository.getGenres() }
    }

    @Test
    fun `screen reobservation retains data without another request`() = runTest {
        coEvery { repository.getGenres() } returns Result.success(listOf(Genre(28, "Action")))
        val viewModel = GenreViewModel(repository).also { store.put("genre", it) }
        runCurrent()
        repeat(2) {
            viewModel.state.test {
                assertEquals(UiState.Success(listOf(Genre(28, "Action"))), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        coVerify(exactly = 1) { repository.getGenres() }
    }

    private suspend fun assertTransition(result: Result<List<Genre>>, expected: UiState<List<Genre>>) {
        val pending = CompletableDeferred<Result<List<Genre>>>()
        coEvery { repository.getGenres() } coAnswers { pending.await() }
        val viewModel = GenreViewModel(repository).also { store.put("genre", it) }
        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            pending.complete(result)
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repository.getGenres() }
    }
}
