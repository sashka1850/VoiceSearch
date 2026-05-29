package com.voicesearch.core.network.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Hilt bindings for the network layer.
 * Yandex.Disk Retrofit service is added in Stage 2.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @dagger.Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
}
