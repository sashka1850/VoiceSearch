package com.voicesearch.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt bindings for the data layer.
 * Real providers (Room database, DAOs, DataStore, EncryptedDataStore) land in Stage 1.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule
