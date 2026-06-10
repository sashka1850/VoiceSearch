package com.voicesearch.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.data.db.VoiceSearchDatabase
import com.voicesearch.core.data.mapper.toEntity
import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.sync.AutoSyncTrigger
import com.voicesearch.core.domain.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [TableRepositoryImpl] against a real in-memory Room database
 * via Robolectric — catches schema and FK issues that fakes wouldn't.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TableRepositoryImplTest {

    private lateinit var db: VoiceSearchDatabase
    private lateinit var repository: TableRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }
    private var fixedClock: Long = 1_700_000_000_000L
    private val syncRequests = mutableListOf<String>()
    private val recordingTrigger = AutoSyncTrigger { tableId -> syncRequests += tableId }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VoiceSearchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TableRepositoryImpl(
            database = db,
            tableDao = db.tableDao(),
            rowDao = db.tableRowDao(),
            settingsDao = db.tableSettingsDao(),
            json = json,
            clock = Clock { fixedClock },
            autoSyncTrigger = recordingTrigger,
        )
        syncRequests.clear()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `save then observe returns table with same fields`() = runTest {
        val table = sampleTable("t1")
        repository.save(table, rows = listOf(listOf("a", "b"), listOf("c", "d")))

        val retrieved = repository.get("t1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved).isEqualTo(table)
    }

    @Test
    fun `observeAll emits new tables as they are saved`() = runTest {
        repository.observeAll().test {
            assertThat(awaitItem()).isEmpty()

            repository.save(sampleTable("t1"), rows = emptyList())
            assertThat(awaitItem().map { it.id }).containsExactly("t1")

            repository.save(sampleTable("t2"), rows = emptyList())
            assertThat(awaitItem().map { it.id }).containsExactly("t1", "t2")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRows reflects inserted cells in order`() = runTest {
        val rows = listOf(
            listOf("1", "alpha"),
            listOf("2", "beta"),
            listOf("3", "gamma"),
        )
        repository.save(sampleTable("t1"), rows = rows)

        val stored = repository.getRows("t1")
        assertThat(stored.map { it.cells }).containsExactlyElementsIn(rows).inOrder()
        assertThat(stored.map { it.rowIndex }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `setRowMarked toggles row state`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a"), listOf("b")))
        val rowId = repository.getRows("t1").first().id

        repository.setRowMarked(rowId, marked = true, markedAt = 42L)
        val updated = repository.getRows("t1").first { it.id == rowId }
        assertThat(updated.isMarked).isTrue()
        assertThat(updated.markedAt).isEqualTo(42L)

        repository.setRowMarked(rowId, marked = false, markedAt = null)
        val cleared = repository.getRows("t1").first { it.id == rowId }
        assertThat(cleared.isMarked).isFalse()
        assertThat(cleared.markedAt).isNull()
    }

    @Test
    fun `setRowsMarked marks every row in the collection`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a"), listOf("b"), listOf("c")))
        val rows = repository.getRows("t1")
        val toMark = rows.take(2).map { it.id }

        repository.setRowsMarked(toMark, marked = true, markedAt = 100L)

        val after = repository.getRows("t1")
        assertThat(after.filter { it.isMarked }.map { it.id }).containsExactlyElementsIn(toMark)
    }

    @Test
    fun `delete cascades into rows`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a"), listOf("b")))
        assertThat(repository.getRows("t1")).hasSize(2)

        repository.delete("t1")

        assertThat(repository.get("t1")).isNull()
        assertThat(repository.getRows("t1")).isEmpty()
    }

    @Test
    fun `setRowMarked does NOT request sync when autoSync is off`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a")))
        db.tableSettingsDao().upsert(settingsFor("t1", autoSync = false))
        val rowId = repository.getRows("t1").first().id

        repository.setRowMarked(rowId, marked = true, markedAt = 1L)

        assertThat(syncRequests).isEmpty()
    }

    @Test
    fun `setRowMarked requests sync when autoSync is on`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a")))
        db.tableSettingsDao().upsert(settingsFor("t1", autoSync = true))
        val rowId = repository.getRows("t1").first().id

        repository.setRowMarked(rowId, marked = true, markedAt = 1L)

        assertThat(syncRequests).containsExactly("t1")
    }

    @Test
    fun `setRowsMarked requests one sync per batch`() = runTest {
        repository.save(sampleTable("t1"), rows = listOf(listOf("a"), listOf("b"), listOf("c")))
        db.tableSettingsDao().upsert(settingsFor("t1", autoSync = true))
        val rows = repository.getRows("t1")

        repository.setRowsMarked(rows.map { it.id }, marked = true, markedAt = 1L)

        assertThat(syncRequests).containsExactly("t1")
    }

    @Test
    fun `saving same id replaces rows`() = runTest {
        val table = sampleTable("t1")
        repository.save(table, rows = listOf(listOf("old1"), listOf("old2"), listOf("old3")))
        assertThat(repository.getRows("t1")).hasSize(3)

        repository.save(table, rows = listOf(listOf("new1")))

        val rows = repository.getRows("t1")
        assertThat(rows).hasSize(1)
        assertThat(rows.first().cells).containsExactly("new1")
    }

    private fun sampleTable(id: String) = Table(
        id = id,
        name = "Table $id",
        source = TableSource.LocalFile(originalFileName = "file.xlsx"),
        headers = listOf("col1", "col2"),
        rowCount = 0,
    )

    private fun settingsFor(tableId: String, autoSync: Boolean) = TableSettings(
        tableId = tableId,
        searchColumnIndex = 0,
        markColumnIndex = 1,
        nameColumnIndex = 0,
        startRowIndex = 0,
        successMarker = "Есть",
        prefix = PrefixHint.FullMatch,
        autoSync = autoSync,
    ).toEntity(json)
}
