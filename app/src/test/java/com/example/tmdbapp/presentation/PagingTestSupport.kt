package com.example.tmdbapp.presentation

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

fun <T : Any> loadedPagingData(items: List<T>): PagingData<T> {
    val done = LoadState.NotLoading(true)
    return PagingData.from(items, LoadStates(done, done, done))
}

fun <T : Any> testPager(load: suspend () -> PagingSource.LoadResult<Int, T>): Flow<PagingData<T>> = Pager(
    config = PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false),
    pagingSourceFactory = {
        object : PagingSource<Int, T>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = load()
            override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
        }
    },
).flow

fun <T : Any> TestScope.presentPaging(
    flow: Flow<PagingData<T>>,
    dispatcher: CoroutineDispatcher,
): AsyncPagingDataDiffer<T> {
    val differ = AsyncPagingDataDiffer(
        diffCallback = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
            override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        },
        updateCallback = mockk<ListUpdateCallback>(relaxed = true),
        mainDispatcher = dispatcher,
        workerDispatcher = dispatcher,
    )
    backgroundScope.launch { flow.collectLatest { differ.submitData(it) } }
    return differ
}
