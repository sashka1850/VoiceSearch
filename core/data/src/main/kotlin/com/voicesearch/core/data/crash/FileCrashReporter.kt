package com.voicesearch.core.data.crash

import android.content.Context
import android.os.Build
import com.voicesearch.core.domain.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes uncaught exceptions to `filesDir/crashes/yyyy-MM-dd_HH-mm-ss.txt`,
 * keeping at most [MAX_LOGS] files. Anything on top of that gets the oldest
 * file pruned — bounded disk usage even if the user never opens Settings.
 *
 * Implementation note: we explicitly DO NOT call `System.exit()` after our
 * handler runs. The chained-in original handler (usually
 * RuntimeInit$KillApplicationHandler) does that itself, and double-killing
 * shows up as a confusing "process died abnormally" in logcat.
 */
@Singleton
class FileCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : CrashReporter {

    private val installed = AtomicBoolean(false)
    private val crashesDir: File by lazy { File(context.filesDir, "crashes").apply { mkdirs() } }

    override fun install() {
        if (!installed.compareAndSet(false, true)) return

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashLog(thread, throwable) }
            // Defer to whatever was registered before us (system handler kills
            // the process). Without this the app would hang on a crash.
            previous?.uncaughtException(thread, throwable)
        }
    }

    override fun recentCrashes(): List<File> =
        crashesDir.listFiles { f -> f.isFile && f.name.endsWith(SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    override fun clearAll() {
        crashesDir.listFiles()?.forEach { it.delete() }
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        val timestamp = clock.nowMillis()
        val name = FILENAME_FORMAT.format(Date(timestamp)) + SUFFIX
        val file = File(crashesDir, name)

        file.bufferedWriter().use { writer ->
            writer.write("VoiceSearch crash report\n")
            writer.write("Time: ${ISO_FORMAT.format(Date(timestamp))}\n")
            // Thread.threadId() is Java 19+ — JVM target is 11 here, so we keep
            // the deprecated `Thread.id` for the runtime that's actually shipping.
            @Suppress("DEPRECATION")
            writer.write("Thread: ${thread.name} (id=${thread.id})\n")
            writer.write("App: ${context.packageName} build ${Build.VERSION.SDK_INT}\n")
            writer.write("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n")
            writer.write("\n--- Stack trace ---\n")
            writer.flush()
            PrintWriter(writer).use { pw ->
                throwable.printStackTrace(pw)
            }
        }

        pruneOldFiles()
    }

    private fun pruneOldFiles() {
        val files = crashesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= MAX_LOGS) return
        files.drop(MAX_LOGS).forEach { it.delete() }
    }

    companion object {
        private const val SUFFIX = ".txt"
        private const val MAX_LOGS = 10

        private val FILENAME_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
    }
}
