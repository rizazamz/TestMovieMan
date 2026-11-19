package com.example.tmdbapp.domain.model

data class MovieReview(
    val id: String,
    val author: String,
    val username: String?,
    val avatarPath: String?,
    val rating: Double?,
    val content: String,
    val createdAt: String?,
    val updatedAt: String?,
    val url: String?,
)
