package com.voicesearch.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.model.TableSource
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Mapping must be lossless for everything the persistence layer can store —
 * if we add a new domain field that the entity doesn't carry, this is where
 * we'll notice first.
 */
class TableMappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Table round trips through entity`() {
        val original = Table(
            id = "table-1",
            name = "Прайс поставщика",
            source = TableSource.LocalFile(originalFileName = "price.xlsx"),
            headers = listOf("№", "Артикул", "Наименование"),
            rowCount = 1234,
        )

        val entity = original.toEntity(json, importedAt = 1_700_000_000_000L, originalFileName = "price.xlsx")
        val restored = entity.toDomain(json)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `Table with Yandex source round trips`() {
        val original = Table(
            id = "table-2",
            name = "Excel из облака",
            source = TableSource.YandexDisk(publicUrl = "https://disk.yandex.ru/i/abc", remotePath = "/Documents/x.xlsx"),
            headers = listOf("id", "name"),
            rowCount = 10,
        )

        val entity = original.toEntity(json, importedAt = 0L, originalFileName = null)
        val restored = entity.toDomain(json)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `TableSettings with each prefix variant round trip`() {
        val variants = listOf(
            PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5),
            PrefixHint.VariableSuffix(prefix = "1234", maxSuffixLength = 8),
            PrefixHint.FullMatch,
        )

        variants.forEach { prefix ->
            val original = TableSettings(
                tableId = "t",
                searchColumnIndex = 1,
                markColumnIndex = 2,
                nameColumnIndex = 3,
                startRowIndex = 1,
                successMarker = "Есть",
                prefix = prefix,
                autoSync = true,
            )
            val restored = original.toEntity(json).toDomain(json)
            assertThat(restored).isEqualTo(original)
        }
    }

    @Test
    fun `TableSettings with no prefix round trips`() {
        val original = TableSettings(
            tableId = "t",
            searchColumnIndex = 0,
            markColumnIndex = 1,
            nameColumnIndex = 2,
            startRowIndex = 1,
            successMarker = "+",
            prefix = null,
            autoSync = false,
        )
        val restored = original.toEntity(json).toDomain(json)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `Row builder serialises cells as JSON list of strings`() {
        val entity = buildRowEntity(
            tableId = "t",
            rowIndex = 5,
            cells = listOf("1", "12345 67890", "Иванов И."),
            json = json,
        )
        assertThat(entity.tableId).isEqualTo("t")
        assertThat(entity.rowIndex).isEqualTo(5)
        // Decode back and verify content survives unchanged.
        val decoded = json.decodeFromString<List<String>>(entity.cellsJson)
        assertThat(decoded).containsExactly("1", "12345 67890", "Иванов И.").inOrder()
    }
}
