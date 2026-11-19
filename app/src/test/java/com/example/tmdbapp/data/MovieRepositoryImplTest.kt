package com.example.tmdbapp.data

import androidx.paging.testing.asSnapshot
import com.example.tmdbapp.data.remote.TmdbApiService
import com.example.tmdbapp.data.remote.dto.GenreDto
import com.example.tmdbapp.data.remote.dto.GenresResponseDto
import com.example.tmdbapp.data.remote.dto.MovieDetailDto
import com.example.tmdbapp.data.remote.dto.MovieDto
import com.example.tmdbapp.data.remote.dto.MovieReviewDto
import com.example.tmdbapp.data.remote.dto.MovieVideoDto
import com.example.tmdbapp.data.remote.dto.MoviesResponseDto
import com.example.tmdbapp.data.remote.dto.ReviewsResponseDto
import com.example.tmdbapp.data.remote.dto.VideosResponseDto
import com.example.tmdbapp.data.repository.MovieRepositoryImpl
import com.example.tmdbapp.domain.model.Genre
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieRepositoryImplTest {
    private val api = mockk<TmdbApiService>()
    private val repository = MovieRepositoryImpl(api)

    @Test
    fun `genres map to domain models`() = runTest {
        coEvery { api.getGenres() } returns GenresResponseDto(listOf(GenreDto(28, "Action")))
        assertEquals(listOf(Genre(28, "Action")), repository.getGenres().getOrThrow())
    }

    @Test
    fun `detail maps missing metadata and nested genres`() = runTest {
        coEvery { api.getMovieDetail(42) } returns MovieDetailDto(
            id = 42,
            title = "Movie",
            overview = null,
            releaseDate = "",
            genres = listOf(GenreDto(28, "Action")),
            revenue = 3_000_000_000L,
        )
        val detail = repository.getMovieDetail(42).getOrThrow()

        assertEquals(42, detail.id)
        assertEquals("Movie", detail.title)
        assertEquals("", detail.overview)
        assertNull(detail.releaseDate)
        assertNull(detail.runtime)
        assertEquals(listOf(Genre(28, "Action")), detail.genres)
        assertEquals(3_000_000_000L, detail.revenue)
    }

    @Test
    fun `trailer selection excludes other sites and types and prefers first official video`() = runTest {
        coEvery { api.getMovieVideos(42) } returns VideosResponseDto(
            listOf(
                video("fan", official = false),
                video("teaser", type = "Teaser"),
                video("official-1"),
                video("vimeo", site = "Vimeo"),
                video("official-2"),
            ),
        )

        val trailer = repository.getMovieTrailer(42).getOrThrow()!!
        assertEquals("official-1", trailer.id)
        assertEquals("YouTube", trailer.site)
        assertEquals("Trailer", trailer.type)
        assertTrue(trailer.official)
    }

    @Test
    fun `no videos is successful null rather than a failure`() = runTest {
        coEvery { api.getMovieVideos(42) } returns VideosResponseDto(emptyList())
        assertNull(repository.getMovieTrailer(42).getOrThrow())
    }

    @Test
    fun `non trailer videos and blank keys produce null`() = runTest {
        coEvery { api.getMovieVideos(42) } returns VideosResponseDto(
            listOf(video("teaser", type = "Teaser"), video("vimeo", site = "Vimeo"), video("blank").copy(key = " ")),
        )
        assertNull(repository.getMovieTrailer(42).getOrThrow())
    }

    @Test
    fun `unofficial YouTube trailer remains a fallback when no official trailer exists`() = runTest {
        coEvery { api.getMovieVideos(42) } returns VideosResponseDto(listOf(video("fan", official = false)))
        assertEquals("fan", repository.getMovieTrailer(42).getOrThrow()?.id)
    }

    @Test
    fun `non paged requests expose failures through Result`() = runTest {
        val failure = IOException("Offline")
        coEvery { api.getGenres() } throws failure
        coEvery { api.getMovieDetail(42) } throws failure
        coEvery { api.getMovieVideos(42) } throws failure

        assertSame(failure, repository.getGenres().exceptionOrNull())
        assertSame(failure, repository.getMovieDetail(42).exceptionOrNull())
        assertSame(failure, repository.getMovieTrailer(42).exceptionOrNull())
    }

    @Test
    fun `HTTP failures remain failed Results for genre detail and trailer`() = runTest {
        val failure = serverError()
        coEvery { api.getGenres() } throws failure
        coEvery { api.getMovieDetail(42) } throws failure
        coEvery { api.getMovieVideos(42) } throws failure
        assertSame(failure, repository.getGenres().exceptionOrNull())
        assertSame(failure, repository.getMovieDetail(42).exceptionOrNull())
        assertSame(failure, repository.getMovieTrailer(42).exceptionOrNull())
    }

    @Test
    fun `Result requests never swallow cancellation`() = runTest {
        val cancellation = CancellationException("Screen left")
        coEvery { api.getGenres() } throws cancellation
        coEvery { api.getMovieDetail(42) } throws cancellation
        coEvery { api.getMovieVideos(42) } throws cancellation
        val requests: List<suspend () -> Any> = listOf(
            { repository.getGenres() },
            { repository.getMovieDetail(42) },
            { repository.getMovieTrailer(42) },
        )
        for (request in requests) {
            try {
                request()
                throw AssertionError("Cancellation was swallowed")
            } catch (exception: CancellationException) {
                assertSame(cancellation, exception)
            }
        }
    }

    @Test
    fun `discover Pager loads both pages and stops at the last page`() = runTest {
        for (page in 1..2) {
            coEvery { api.discoverMovies("28", page) } returns MoviesResponseDto(
                page, listOf(MovieDto(page, "Movie $page")), 2, 2,
            )
        }
        val movies = repository.discoverMovies("28").asSnapshot { appendScrollWhile { true } }

        assertEquals(listOf(1, 2), movies.map { it.id })
        coVerify(exactly = 0) { api.discoverMovies(any(), 3) }
    }

    @Test
    fun `review Pager loads both pages and retains selected movie`() = runTest {
        for (page in 1..2) {
            coEvery { api.getMovieReviews(42, page) } returns ReviewsResponseDto(
                page, listOf(MovieReviewDto("$page", "Reviewer", content = "Review $page")), 2, 2,
            )
        }
        val reviews = repository.getMovieReviews(42).asSnapshot { appendScrollWhile { true } }

        assertEquals(listOf("1", "2"), reviews.map { it.id })
        coVerify(exactly = 0) { api.getMovieReviews(any(), 3) }
    }

    private fun video(
        id: String,
        site: String = "YouTube",
        type: String = "Trailer",
        official: Boolean = true,
    ) = MovieVideoDto(id, "key-$id", "Video $id", site, type, official)
}
