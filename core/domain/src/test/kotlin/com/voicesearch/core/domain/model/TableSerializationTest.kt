package com.voicesearch.core.domain.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Smoke test for the domain models — also acts as the sanity check that
 * core:domain's JUnit + Truth + kotlinx-serialization wiring works end-to-end.
 */
class TableSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `table round-trips through json`() {
        val original = Table(
            id = "t1",
            name = "Прайс поставщика",
            source = TableSource.YandexDisk(publicUrl = "https://disk.yandex.ru/i/abc", remotePath = null),
            headers = listOf("№", "Артикул", "Наименование"),
            rowCount = 1500,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Table>(encoded)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `prefix hint variants round-trip`() {
        val variants = listOf<PrefixHint>(
            PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5),
            PrefixHint.VariableSuffix(prefix = "12345", maxSuffixLength = 7),
            PrefixHint.FullMatch,
        )

        variants.forEach { hint ->
            val encoded = json.encodeToString(hint)
            val decoded = json.decodeFromString<PrefixHint>(encoded)
            assertThat(decoded).isEqualTo(hint)
        }
    }
}
