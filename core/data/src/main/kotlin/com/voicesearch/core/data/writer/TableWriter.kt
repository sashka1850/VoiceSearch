package com.voicesearch.core.data.writer

import java.io.OutputStream

/**
 * Symmetric counterpart to [com.voicesearch.core.data.parser.TableParser] —
 * takes a normalised in-memory table and serialises it back to the original
 * spreadsheet format. The exporter pipeline uses this to regenerate a file
 * from Room rows + applied marks before sharing or uploading.
 *
 * Implementations should write directly to the [OutputStream] without holding
 * the entire output in memory — 100k-row exports should be streamable.
 */
interface TableWriter {
    @Throws(TableWriteException::class)
    fun write(headers: List<String>, rows: List<List<String>>, output: OutputStream)
}

class TableWriteException(message: String, cause: Throwable? = null) : Exception(message, cause)
