package com.example.tmdbapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorDetailsDto(
    val username: String? = null,
    @SerialName("avatar_path") val avatarPath: String? = null,
    val rating: Double? = null,
)

@Serializable
data class MovieReviewDto(
    val id: String,
    val author: String,
    @SerialName("author_details") val authorDetails: AuthorDetailsDto? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val url: String? = null,
)

@Serializable
data class ReviewsResponseDto(
    val page: Int,
    val results: List<MovieReviewDto>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int,
)
