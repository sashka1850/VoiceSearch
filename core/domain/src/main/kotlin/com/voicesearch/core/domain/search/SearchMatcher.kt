package com.voicesearch.core.domain.search

import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.repository.TableRow

/**
 * Pure functions that decide whether a row matches the user's spoken query,
 * given the table's [PrefixHint]. Lives in :core:domain because we want this
 * tested without any Android / DB plumbing.
 *
 * Normalisation strips everything that isn't a letter or digit so the user
 * can say "двенадцать тридцать четыре" (which most speech engines render as
 * "12 34") and we still match "1234" in a cell.
 */
object SearchMatcher {

    /** What the user actually spoke, reduced to a comparable form. */
    fun normaliseQuery(spoken: String): String =
        spoken.filter(Char::isLetterOrDigit)

    /** What's in a cell, reduced to a comparable form. */
    fun normaliseCell(value: String): String =
        value.filter(Char::isLetterOrDigit)

    /**
     * @return rows where the [TableSettings.searchColumnIndex] cell matches
     * the [spokenQuery]. Rows above [TableSettings.startRowIndex] are skipped.
     * Result preserves source order, so the UI sees rows in spreadsheet order.
     */
    fun findMatches(
        rows: List<TableRow>,
        settings: TableSettings,
        spokenQuery: String,
    ): List<TableRow> {
        val query = normaliseQuery(spokenQuery)
        if (query.isEmpty()) return emptyList()

        return rows.asSequence()
            .filter { it.rowIndex >= settings.startRowIndex }
            .filter { row ->
                val cellRaw = row.cells.getOrNull(settings.searchColumnIndex).orEmpty()
                cellMatches(cellRaw, query, settings.prefix ?: PrefixHint.FullMatch)
            }
            .toList()
    }

    /**
     * Single-cell decision. Public so unit tests can poke at corner cases.
     */
    fun cellMatches(cellValue: String, query: String, hint: PrefixHint): Boolean {
        val cell = normaliseCell(cellValue)
        val q = normaliseQuery(query)
        if (q.isEmpty() || cell.isEmpty()) return false

        return when (hint) {
            is PrefixHint.FixedSuffix -> cell.endsWith(q)
            is PrefixHint.VariableSuffix -> cell.endsWith(q)
            PrefixHint.FullMatch -> cell.contains(q, ignoreCase = true)
        }
    }
}
