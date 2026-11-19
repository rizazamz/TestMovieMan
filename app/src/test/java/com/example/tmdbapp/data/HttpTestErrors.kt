package com.example.tmdbapp.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

fun serverError(code: Int = 500) = HttpException(
    Response.error<Unit>(code, "{}".toResponseBody("application/json".toMediaType())),
)
