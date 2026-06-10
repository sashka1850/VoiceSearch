package com.voicesearch.app.feature.sync.workmanager

import android.content.Context
import androidx.work.WorkManager
import com.voicesearch.core.domain.sync.AutoSyncTrigger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the WorkManager-backed auto-sync into the rest of the Hilt graph:
 * provides the system [WorkManager] instance and binds the domain
 * [AutoSyncTrigger] port to its WorkManager implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBindingsModule {

    @Binds
    @Singleton
    internal abstract fun bindAutoSyncTrigger(impl: WorkManagerAutoSyncTrigger): AutoSyncTrigger
}
