package com.example.tmdbapp.di

import com.example.tmdbapp.data.repository.MovieRepositoryImpl
import com.example.tmdbapp.domain.repository.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(implementation: MovieRepositoryImpl): MovieRepository
}
