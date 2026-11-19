package com.example.tmdbapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.tmdbapp.data.mapper.toDomain
import com.example.tmdbapp.data.remote.TmdbApiService
import com.example.tmdbapp.domain.model.MovieReview
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class MovieReviewsPagingSource(
    private val api: TmdbApiService,
    private val movieId: Int,
) : PagingSource<Int, MovieReview>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieReview> {
        val page = params.key ?: 1
        return try {
            val response = api.getMovieReviews(movieId = movieId, page = page)
            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (page < response.totalPages && response.results.isNotEmpty()) page + 1 else null,
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        } catch (exception: SerializationException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MovieReview>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
