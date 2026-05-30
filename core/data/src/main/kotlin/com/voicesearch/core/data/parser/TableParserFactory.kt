package com.voicesearch.core.data.parser

import javax.inject.Inject

/**
 * Dispatches to the right [TableParser] based on file extension.
 *
 * When we add new formats later, register them here. The import use case
 * doesn't need to know what extensions are supported — it just asks the
 * factory and gets a parser or null.
 */
class TableParserFactory @Inject constructor(
    private val xlsx: XlsxParser,
    private val csv: CsvParser,
) {
    fun parserFor(fileName: String): TableParser? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "xlsx" -> xlsx
        "csv", "tsv" -> csv
        else -> null
    }

    /** File extensions the importer advertises in the system file picker. */
    val supportedExtensions: List<String> = listOf("xlsx", "csv")

    /**
     * MIME types for `Intent.ACTION_OPEN_DOCUMENT`'s `setType` /
     * `EXTRA_MIME_TYPES`. Some providers report wrong MIME for csv, so
     * we accept the wildcards Android fills in.
     */
    val supportedMimeTypes: Array<String> = arrayOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
        "text/csv",
        "text/comma-separated-values",
        "application/csv",
        "text/plain", // fallback some providers use for csv
    )
}
