package com.example.tmdbapp.data.remote

import com.example.tmdbapp.BuildConfig
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        if (url.scheme != "https" || url.host != "api.themoviedb.org" || url.port != 443) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.TMDB_API_TOKEN}")
                .build(),
        )
    }
}
