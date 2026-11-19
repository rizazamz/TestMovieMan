package com.example.tmdbapp.presentation

import androidx.paging.PagingSource
import com.example.tmdbapp.data.mapper.toDomain
import com.example.tmdbapp.data.remote.dto.GenreDto
import com.example.tmdbapp.data.remote.dto.MovieDetailDto
import com.example.tmdbapp.data.serverError
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

val navigationGenre = Genre(28, "Action")
val navigationMovie = Movie(42, "Audit Movie", "Audit synopsis", null, null, "2024-01-01", 8.0, 10, listOf(28))
val navigationReview = MovieReview("review", "", " ", null, 8.0, "Audit review", null, null, null)

fun navigationTestRepository(): MovieRepository {
    val repository = mockk<MovieRepository>()
    val discoverAttempts = AtomicInteger()
    val reviewAttempts = AtomicInteger()
    coEvery { repository.getGenres() } returnsMany listOf(
        Result.failure(IOException("Offline")), Result.success(listOf(navigationGenre)),
    )
    every { repository.discoverMovies(navigationGenre.id.toString()) } returns testPager {
        if (discoverAttempts.getAndIncrement() == 0) {
            PagingSource.LoadResult.Error(IOException("Offline"))
        } else {
            PagingSource.LoadResult.Page(listOf(navigationMovie), null, null)
        }
    }
    coEvery { repository.getMovieDetail(navigationMovie.id) } returnsMany listOf(
        Result.failure(serverError()),
        Result.success(
            MovieDetailDto(
                id = navigationMovie.id,
                title = navigationMovie.title,
                overview = navigationMovie.overview,
                genres = listOf(GenreDto(navigationGenre.id, navigationGenre.name)),
                runtime = 120,
            ).toDomain(),
        ),
    )
    coEvery { repository.getMovieTrailer(navigationMovie.id) } returns Result.success(null)
    every { repository.getMovieReviews(navigationMovie.id) } returns testPager {
        if (reviewAttempts.getAndIncrement() == 0) {
            PagingSource.LoadResult.Error(serverError())
        } else {
            PagingSource.LoadResult.Page(listOf(navigationReview), null, null)
        }
    }
    return repository
}
