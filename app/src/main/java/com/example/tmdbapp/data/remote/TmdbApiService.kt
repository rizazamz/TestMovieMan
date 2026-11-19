package com.example.tmdbapp.data.remote

import com.example.tmdbapp.data.remote.dto.GenresResponseDto
import com.example.tmdbapp.data.remote.dto.MovieDetailDto
import com.example.tmdbapp.data.remote.dto.MoviesResponseDto
import com.example.tmdbapp.data.remote.dto.ReviewsResponseDto
import com.example.tmdbapp.data.remote.dto.VideosResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("genre/movie/list")
    suspend fun getGenres(): GenresResponseDto

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") withGenres: String? = null,
        @Query("page") page: Int = 1,
    ): MoviesResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(@Path("movie_id") movieId: Int): MovieDetailDto

    @GET("movie/{movie_id}/reviews")
    suspend fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
    ): ReviewsResponseDto

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(@Path("movie_id") movieId: Int): VideosResponseDto
}
