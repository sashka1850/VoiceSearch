package com.voicesearch.core.data.di

import com.voicesearch.core.data.datastore.AppPreferencesRepositoryImpl
import com.voicesearch.core.data.datastore.YandexAuthRepositoryImpl
import com.voicesearch.core.data.repository.TableRepositoryImpl
import com.voicesearch.core.data.repository.TableSettingsRepositoryImpl
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableSettingsRepository
import com.voicesearch.core.domain.repository.YandexAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their data-layer implementations.
 * Tests can override these bindings via Hilt's `@TestInstallIn(replaces = ...)`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindTableRepository(impl: TableRepositoryImpl): TableRepository

    @Binds
    @Singleton
    internal abstract fun bindTableSettingsRepository(impl: TableSettingsRepositoryImpl): TableSettingsRepository

    @Binds
    @Singleton
    internal abstract fun bindAppPreferencesRepository(impl: AppPreferencesRepositoryImpl): AppPreferencesRepository

    @Binds
    @Singleton
    internal abstract fun bindYandexAuthRepository(impl: YandexAuthRepositoryImpl): YandexAuthRepository
}
