package com.example.tmdbapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieVideoDto(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
)

@Serializable
data class VideosResponseDto(val results: List<MovieVideoDto>)
