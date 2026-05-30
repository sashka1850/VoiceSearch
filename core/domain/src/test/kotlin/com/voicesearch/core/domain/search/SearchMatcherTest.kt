package com.voicesearch.core.domain.search

import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.repository.TableRow
import org.junit.Test

class SearchMatcherTest {

    private val baseSettings = TableSettings(
        tableId = "t",
        searchColumnIndex = 0,
        markColumnIndex = 2,
        nameColumnIndex = 1,
        startRowIndex = 0,
        successMarker = "Есть",
        prefix = null,
        autoSync = false,
    )

    // region cellMatches

    @Test
    fun `FixedSuffix matches when cell ends with spoken digits`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "1234567890",
            query = "67890",
            hint = PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5),
        )
        assertThat(ok).isTrue()
    }

    @Test
    fun `FixedSuffix rejects when suffix differs`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "1234567890",
            query = "11111",
            hint = PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5),
        )
        assertThat(ok).isFalse()
    }

    @Test
    fun `VariableSuffix accepts shorter spoken tail`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "12345000099",
            query = "99",
            hint = PrefixHint.VariableSuffix(prefix = "12345", maxSuffixLength = 7),
        )
        assertThat(ok).isTrue()
    }

    @Test
    fun `FullMatch is substring contains`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "Иванов Иван Иванович",
            query = "Иванович",
            hint = PrefixHint.FullMatch,
        )
        assertThat(ok).isTrue()
    }

    @Test
    fun `whitespace and dashes inside cell are ignored`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "АРТ-12345 67890",
            query = "67890",
            hint = PrefixHint.FixedSuffix(prefix = "АРТ1234", suffixLength = 6),
        )
        assertThat(ok).isTrue()
    }

    @Test
    fun `spoken text with words and digits is stripped to chars`() {
        // Speech recognizer in ru-RU often returns "12 34" or "12, 34" — we strip everything
        // that isn't a letter or digit before comparison.
        val ok = SearchMatcher.cellMatches(
            cellValue = "1234",
            query = "12 34",
            hint = PrefixHint.FullMatch,
        )
        assertThat(ok).isTrue()
    }

    @Test
    fun `empty query never matches`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "12345",
            query = "",
            hint = PrefixHint.FullMatch,
        )
        assertThat(ok).isFalse()
    }

    @Test
    fun `empty cell never matches`() {
        val ok = SearchMatcher.cellMatches(
            cellValue = "",
            query = "12345",
            hint = PrefixHint.FullMatch,
        )
        assertThat(ok).isFalse()
    }

    // endregion

    // region findMatches

    @Test
    fun `findMatches returns rows in spreadsheet order`() {
        val rows = listOf(
            row(id = 1, rowIndex = 0, cells = listOf("1234567890", "Иванов")),
            row(id = 2, rowIndex = 1, cells = listOf("9999999999", "Петров")),
            row(id = 3, rowIndex = 2, cells = listOf("1111167890", "Сидоров")),
            row(id = 4, rowIndex = 3, cells = listOf("0000067890", "Соколов")),
        )
        val matches = SearchMatcher.findMatches(
            rows = rows,
            settings = baseSettings.copy(prefix = PrefixHint.FixedSuffix("12345", 5)),
            spokenQuery = "67890",
        )
        assertThat(matches.map { it.id }).containsExactly(1L, 3L, 4L).inOrder()
    }

    @Test
    fun `findMatches respects startRowIndex`() {
        val rows = listOf(
            row(id = 1, rowIndex = 0, cells = listOf("1234567890")),
            row(id = 2, rowIndex = 1, cells = listOf("1234567890")),
            row(id = 3, rowIndex = 2, cells = listOf("1234567890")),
        )
        val matches = SearchMatcher.findMatches(
            rows = rows,
            settings = baseSettings.copy(startRowIndex = 1),
            spokenQuery = "1234567890",
        )
        assertThat(matches.map { it.id }).containsExactly(2L, 3L).inOrder()
    }

    @Test
    fun `findMatches skips rows missing the search column`() {
        val rows = listOf(
            row(id = 1, rowIndex = 0, cells = listOf("12345")),
            row(id = 2, rowIndex = 1, cells = emptyList()),  // too short
        )
        val matches = SearchMatcher.findMatches(
            rows = rows,
            settings = baseSettings.copy(prefix = PrefixHint.FullMatch),
            spokenQuery = "12345",
        )
        assertThat(matches.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `findMatches returns empty when nothing matches`() {
        val rows = listOf(
            row(id = 1, rowIndex = 0, cells = listOf("12345")),
            row(id = 2, rowIndex = 1, cells = listOf("67890")),
        )
        val matches = SearchMatcher.findMatches(
            rows = rows,
            settings = baseSettings.copy(prefix = PrefixHint.FullMatch),
            spokenQuery = "99999",
        )
        assertThat(matches).isEmpty()
    }

    // endregion

    private fun row(id: Long, rowIndex: Int, cells: List<String>) = TableRow(
        id = id,
        tableId = "t",
        rowIndex = rowIndex,
        cells = cells,
        isMarked = false,
        markedAt = null,
    )
}
