package com.example.tmdbapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.tmdbapp.data.mapper.toDomain
import com.example.tmdbapp.data.paging.DiscoverMoviesPagingSource
import com.example.tmdbapp.data.paging.MovieReviewsPagingSource
import com.example.tmdbapp.data.remote.TmdbApiService
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.model.MovieVideo
import com.example.tmdbapp.domain.repository.MovieRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApiService,
) : MovieRepository {
    override suspend fun getGenres(): Result<List<Genre>> = requestResult {
        api.getGenres().genres.map { it.toDomain() }
    }

    override fun discoverMovies(withGenres: String?): Flow<PagingData<Movie>> = Pager(
        config = pagingConfig(),
        pagingSourceFactory = { DiscoverMoviesPagingSource(api, withGenres) },
    ).flow

    override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = requestResult {
        api.getMovieDetail(movieId).toDomain()
    }

    override suspend fun getMovieTrailer(movieId: Int): Result<MovieVideo?> = requestResult {
        api.getMovieVideos(movieId).results
            .filter { it.site == "YouTube" && it.type == "Trailer" && it.key.isNotBlank() }
            .maxByOrNull { it.official }
            ?.toDomain()
    }

    override fun getMovieReviews(movieId: Int): Flow<PagingData<MovieReview>> = Pager(
        config = pagingConfig(),
        pagingSourceFactory = { MovieReviewsPagingSource(api, movieId) },
    ).flow

    private fun pagingConfig() = PagingConfig(
        pageSize = 20,
        initialLoadSize = 20,
        enablePlaceholders = false,
    )

    private suspend fun <T> requestResult(request: suspend () -> T): Result<T> = try {
        Result.success(request())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
