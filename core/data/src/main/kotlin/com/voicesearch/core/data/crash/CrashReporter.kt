package com.voicesearch.core.data.crash

import java.io.File

/**
 * Local-only crash collector. There is no telemetry path — when the app crashes
 * we write a `.txt` file to internal storage and the user can choose to share
 * it via the system share sheet from Settings.
 *
 * This is the privacy-preserving alternative to AppMetrica / Crashlytics that
 * the PRIVACY_POLICY.md commits to ("никакой аналитики").
 */
interface CrashReporter {
    /**
     * Hooks the default uncaught-exception handler. Idempotent — safe to call
     * from `Application.onCreate()` every process start. The original handler
     * is preserved and invoked after we've written our log, so the system
     * still gets to show ANR / crash dialogs.
     */
    fun install()

    /**
     * Crash logs currently on disk, newest first. Returns an empty list when
     * the user has never hit a crash on this device (the happy path).
     */
    fun recentCrashes(): List<File>

    /**
     * Deletes every crash log file. Called from Settings → "Очистить логи".
     */
    fun clearAll()
}
