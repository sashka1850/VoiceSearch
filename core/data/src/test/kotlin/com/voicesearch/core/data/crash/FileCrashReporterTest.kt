package com.voicesearch.core.data.crash

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.domain.time.Clock
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pure-JVM tests — FileCrashReporter only touches `filesDir`, which we
 * fake out with a TemporaryFolder. Lets the suite run on the regular
 * `testDebugUnitTest` task without Robolectric.
 */
class FileCrashReporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var clock: Clock
    private lateinit var filesDir: File
    private var nowMillis: Long = 1_700_000_000_000L

    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        context = mockk(relaxed = true)
        every { context.filesDir } returns filesDir
        every { context.packageName } returns "com.voicesearch.app.test"
        clock = Clock { nowMillis }

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    @After
    fun tearDown() {
        // Don't leave our test handler installed for the rest of the JVM.
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @Test
    fun `recentCrashes is empty when no logs written`() {
        val reporter = FileCrashReporter(context, clock)
        assertThat(reporter.recentCrashes()).isEmpty()
    }

    @Test
    fun `install delegates to previous handler after writing log`() {
        var delegateInvoked = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> delegateInvoked = true }

        val reporter = FileCrashReporter(context, clock)
        reporter.install()

        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(
            Thread.currentThread(),
            IllegalStateException("boom"),
        )

        assertThat(delegateInvoked).isTrue()
        val logs = reporter.recentCrashes()
        assertThat(logs).hasSize(1)
        val content = logs.first().readText()
        assertThat(content).contains("boom")
        assertThat(content).contains("IllegalStateException")
    }

    @Test
    fun `install is idempotent`() {
        val reporter = FileCrashReporter(context, clock)
        reporter.install()
        val firstHandler = Thread.getDefaultUncaughtExceptionHandler()
        reporter.install()
        val secondHandler = Thread.getDefaultUncaughtExceptionHandler()
        // Second install must be a no-op so we don't stack our handler N times.
        assertThat(secondHandler).isSameInstanceAs(firstHandler)
    }

    @Test
    fun `clearAll wipes existing crash files`() {
        val reporter = FileCrashReporter(context, clock)
        // Drop two files in directly to bypass the install-handler dance.
        val crashesDir = File(filesDir, "crashes").apply { mkdirs() }
        File(crashesDir, "old.txt").writeText("a")
        File(crashesDir, "newer.txt").writeText("b")

        assertThat(reporter.recentCrashes()).hasSize(2)
        reporter.clearAll()
        assertThat(reporter.recentCrashes()).isEmpty()
    }

    @Test
    fun `older logs are pruned past MAX_LOGS`() {
        val reporter = FileCrashReporter(context, clock)
        reporter.install()
        val handler = Thread.getDefaultUncaughtExceptionHandler()!!

        // 12 crashes, each at a distinct timestamp so filenames don't collide.
        repeat(12) { idx ->
            nowMillis += 1_000L + idx
            handler.uncaughtException(Thread.currentThread(), RuntimeException("crash-$idx"))
        }

        val logs = reporter.recentCrashes()
        assertThat(logs.size).isAtMost(10)
        // Newest first — first file should contain the latest crash payload.
        assertThat(logs.first().readText()).contains("crash-11")
    }
}
