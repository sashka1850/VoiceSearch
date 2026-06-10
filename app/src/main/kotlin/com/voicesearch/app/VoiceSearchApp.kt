package com.voicesearch.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Implements [Configuration.Provider] so WorkManager's on-demand init picks up
 * the Hilt-aware [HiltWorkerFactory] — required for any `@HiltWorker` to be
 * instantiated (otherwise WorkManager builds workers with the default
 * no-arg factory and constructor injection silently breaks).
 */
@HiltAndroidApp
class VoiceSearchApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.INFO else android.util.Log.WARN)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
