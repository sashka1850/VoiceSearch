package com.voicesearch.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.voicesearch.core.data.crash.CrashReporter
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

    // CrashReporter writes uncaught exceptions to a local file the user can
    // share via Settings. There's no telemetry path — see PRIVACY_POLICY.md.
    @Inject lateinit var crashReporter: CrashReporter

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.INFO else android.util.Log.WARN)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Install BEFORE Timber so crashes during DI setup still get a log file.
        crashReporter.install()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
