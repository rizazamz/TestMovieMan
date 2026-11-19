package com.example.tmdbapp.domain.repository

import androidx.paging.PagingData
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.model.MovieVideo
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getGenres(): Result<List<Genre>>
    fun discoverMovies(withGenres: String? = null): Flow<PagingData<Movie>>
    suspend fun getMovieDetail(movieId: Int): Result<MovieDetail>
    suspend fun getMovieTrailer(movieId: Int): Result<MovieVideo?>
    fun getMovieReviews(movieId: Int): Flow<PagingData<MovieReview>>
}
