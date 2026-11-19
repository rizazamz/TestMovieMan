package com.example.tmdbapp.presentation

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.tmdbapp.MainActivity
import com.example.tmdbapp.R
import com.example.tmdbapp.di.RepositoryModule
import com.example.tmdbapp.domain.repository.MovieRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = HiltTestApplication::class, qualifiers = "w420dp-h900dp")
class NavigationFlowTest {
    @BindValue @JvmField val repository: MovieRepository = navigationTestRepository()
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `genre discover detail and back preserve state after retries`() {
        openDetailAfterRetries()
        scrollTo(text(R.string.trailer_empty))
        scrollTo(text(R.string.error_server))
        scrollTo(text(R.string.retry))
        clickRetry()
        scrollTo(navigationReview.content)
        waitForText(navigationReview.content)
        waitForText(text(R.string.anonymous_reviewer))
        compose.activityRule.scenario.recreate()
        waitForText(navigationReview.content)
        waitForText(text(R.string.back)).performClick()
        waitForText(navigationMovie.title)
        waitForText(text(R.string.back)).performClick()
        waitForText(text(R.string.choose_genre))

        coVerify(exactly = 2) { repository.getGenres() }
        verify(exactly = 1) { repository.discoverMovies(navigationGenre.id.toString()) }
        coVerify(exactly = 2) { repository.getMovieDetail(navigationMovie.id) }
        verify(exactly = 1) { repository.getMovieReviews(navigationMovie.id) }
    }

    @Test
    fun `empty genres show an informative state and retry can recover`() {
        waitForText(text(R.string.error_network))
        coEvery { repository.getGenres() } returns Result.success(emptyList())
        clickRetry()
        waitForText(text(R.string.genres_empty))
        coEvery { repository.getGenres() } returns Result.success(listOf(navigationGenre))
        clickRetry()
        waitForText(navigationGenre.name)
    }

    @Test
    fun `empty discover retains back navigation and genre state`() {
        waitForText(text(R.string.error_network))
        clickRetry()
        every { repository.discoverMovies(navigationGenre.id.toString()) } returns flowOf(loadedPagingData(emptyList()))
        waitForText(navigationGenre.name).performClick()
        waitForText(text(R.string.movies_empty))
        waitForText(text(R.string.back)).performClick()
        waitForText(navigationGenre.name)
        coVerify(exactly = 2) { repository.getGenres() }
    }

    @Test
    fun `empty reviews and absent trailer leave detail readable and back available`() {
        every { repository.getMovieReviews(navigationMovie.id) } returns flowOf(loadedPagingData(emptyList()))
        openDetailAfterRetries()
        scrollTo(text(R.string.trailer_empty))
        waitForText(text(R.string.trailer_empty))
        scrollTo(text(R.string.reviews_empty))
        waitForText(text(R.string.reviews_empty))
        waitForText(text(R.string.back)).performClick()
        waitForText(navigationMovie.title)
    }

    private fun openDetailAfterRetries() {
        waitForText(text(R.string.error_network))
        clickRetry()
        waitForText(navigationGenre.name).performClick()
        waitForText(text(R.string.error_network))
        clickRetry()
        waitForText(navigationMovie.title).performClick()
        waitForText(text(R.string.error_server))
        clickRetry()
    }

    private fun clickRetry() {
        waitForText(text(R.string.retry)).performClick()
    }

    private fun scrollTo(value: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(value))
    }

    private fun waitForText(value: String): SemanticsNodeInteraction {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
        return compose.onNodeWithText(value).assertIsDisplayed()
    }

    private fun text(resourceId: Int) = compose.activity.getString(resourceId)
}
