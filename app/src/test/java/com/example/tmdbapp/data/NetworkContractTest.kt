package com.example.tmdbapp.data

import com.example.tmdbapp.BuildConfig
import com.example.tmdbapp.data.remote.AuthInterceptor
import com.example.tmdbapp.di.NetworkModule
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException

class NetworkContractTest {
    @Test
    fun `Retrofit sends all endpoint paths queries and bearer headers and decodes nullable fields`() = runTest {
        val requests = CopyOnWriteArrayList<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor { chain ->
                val request = chain.request()
                requests.add(request)
                val body = when (request.url.encodedPath) {
                    "/3/genre/movie/list" -> """{"genres":[{"id":28,"name":"Action"}],"extra":true}"""
                    "/3/discover/movie" -> """{"page":2,"results":[{"id":42,"title":"Movie","poster_path":null}],"total_pages":3,"total_results":41}"""
                    "/3/movie/42" -> """{"id":42,"title":"Movie","overview":null,"runtime":null,"revenue":3000000000}"""
                    "/3/movie/42/reviews" -> """{"page":3,"results":[{"id":"review","author":"Reviewer","content":"Review","author_details":{"rating":null,"avatar_path":null}}],"total_pages":3,"total_results":41}"""
                    "/3/movie/42/videos" -> """{"id":42,"results":[{"id":"video","key":"abc","name":"Trailer","site":"YouTube","type":"Trailer","official":true}]}"""
                    else -> throw AssertionError("Unexpected path ${request.url.encodedPath}")
                }
                response(request, body)
            }
            .build()
        val service = NetworkModule.provideTmdbApiService(
            NetworkModule.provideRetrofit(client, NetworkModule.provideJson()),
        )

        assertEquals(28, service.getGenres().genres.single().id)
        assertNull(service.discoverMovies("28|12", 2).results.single().posterPath)
        assertEquals(3_000_000_000L, service.getMovieDetail(42).revenue)
        assertNull(service.getMovieReviews(42, 3).results.single().authorDetails?.rating)
        assertEquals(true, service.getMovieVideos(42).results.single().official)

        assertEquals("28|12", requests[1].url.queryParameter("with_genres"))
        assertEquals("2", requests[1].url.queryParameter("page"))
        assertEquals("3", requests[3].url.queryParameter("page"))
        requests.forEach { assertEquals("Bearer ${BuildConfig.TMDB_API_TOKEN}", it.header("Authorization")) }
    }

    @Test
    fun `auth interceptor does not attach token to a foreign origin or insecure URL`() {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor { chain ->
                requests.add(chain.request())
                response(chain.request(), "{}")
            }
            .build()

        listOf("https://example.com/", "http://api.themoviedb.org/", "https://api.themoviedb.org:8443/").forEach {
            client.newCall(Request.Builder().url(it).build()).execute().close()
        }
        requests.forEach { assertNull(it.header("Authorization")) }
    }

    @Test
    fun `non success HTTP responses remain HttpException`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain -> response(chain.request(), "{}", 401) }
            .build()
        val service = NetworkModule.provideTmdbApiService(
            NetworkModule.provideRetrofit(client, NetworkModule.provideJson()),
        )

        try {
            service.getMovieDetail(42)
            throw AssertionError("HTTP failure was swallowed")
        } catch (exception: HttpException) {
            assertEquals(401, exception.code())
        }
    }

    private fun response(request: Request, json: String, code: Int = 200): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Unauthorized")
        .body(json.toResponseBody("application/json".toMediaType()))
        .build()
}
