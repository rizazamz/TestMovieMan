package com.example.tmdbapp.presentation.common

import java.io.IOException
import retrofit2.HttpException

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val kind: ErrorKind) : UiState<Nothing>
}

enum class ErrorKind { Network, Unauthorized, Server, Unknown }

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is IOException -> ErrorKind.Network
    is HttpException -> when (code()) {
        401, 403 -> ErrorKind.Unauthorized
        in 500..599 -> ErrorKind.Server
        else -> ErrorKind.Unknown
    }
    else -> ErrorKind.Unknown
}
