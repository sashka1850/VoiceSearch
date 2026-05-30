package com.voicesearch.core.data.parser

import java.io.InputStream

/**
 * Reads an external spreadsheet format into our normalised in-memory shape.
 *
 * Implementations should be **streaming** wherever possible — these will be
 * called on 100k-row files and the whole table never has to fit in memory
 * twice at once.
 *
 * Failure surface intentionally narrow: any I/O or format error is rethrown
 * as [TableParseException] so the import use case can show a single
 * "Не удалось прочитать файл" with the underlying reason in details.
 */
interface TableParser {

    /**
     * @param input  the raw bytes of the file. The caller is responsible for closing it.
     * @return a [ParsedTable] with column headers and every data row as a list of cells.
     */
    @Throws(TableParseException::class)
    fun parse(input: InputStream): ParsedTable
}

data class ParsedTable(
    val headers: List<String>,
    val rows: List<List<String>>,
) {
    val rowCount: Int get() = rows.size
    val columnCount: Int get() = headers.size
}

class TableParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
