package com.example.tmdbapp.data.mapper

import com.example.tmdbapp.data.remote.dto.GenreDto
import com.example.tmdbapp.data.remote.dto.MovieDetailDto
import com.example.tmdbapp.data.remote.dto.MovieDto
import com.example.tmdbapp.data.remote.dto.MovieReviewDto
import com.example.tmdbapp.data.remote.dto.MovieVideoDto
import com.example.tmdbapp.domain.model.Genre
import com.example.tmdbapp.domain.model.Movie
import com.example.tmdbapp.domain.model.MovieDetail
import com.example.tmdbapp.domain.model.MovieReview
import com.example.tmdbapp.domain.model.MovieVideo

internal fun GenreDto.toDomain() = Genre(id = id, name = name)

internal fun MovieDto.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview.orEmpty(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds,
)

internal fun MovieDetailDto.toDomain() = MovieDetail(
    id = id,
    title = title,
    overview = overview.orEmpty(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    voteAverage = voteAverage,
    voteCount = voteCount,
    genres = genres.map { it.toDomain() },
    runtime = runtime,
    tagline = tagline,
    status = status,
    homepage = homepage,
    imdbId = imdbId,
    originalLanguage = originalLanguage,
    budget = budget,
    revenue = revenue,
)

internal fun MovieReviewDto.toDomain() = MovieReview(
    id = id,
    author = author,
    username = authorDetails?.username,
    avatarPath = authorDetails?.avatarPath,
    rating = authorDetails?.rating,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    url = url,
)

internal fun MovieVideoDto.toDomain() = MovieVideo(
    id = id,
    key = key,
    name = name,
    site = site,
    type = type,
    official = official,
    publishedAt = publishedAt,
)
