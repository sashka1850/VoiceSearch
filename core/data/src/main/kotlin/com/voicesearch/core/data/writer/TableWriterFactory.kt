package com.voicesearch.core.data.writer

import javax.inject.Inject

/** Format the exporter emits; surfaced to the UI as MIME + extension. */
enum class ExportFormat(val extension: String, val mimeType: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv"),
}

class TableWriterFactory @Inject constructor(
    private val xlsx: XlsxWriter,
    private val csv: CsvWriter,
) {
    fun writerFor(format: ExportFormat): TableWriter = when (format) {
        ExportFormat.XLSX -> xlsx
        ExportFormat.CSV -> csv
    }

    /**
     * Best-effort guess based on the source file's original extension.
     * Anything we don't recognise → XLSX, which Yandex.Документы opens
     * natively and Excel/LibreOffice both handle.
     */
    fun formatFromFileName(name: String?): ExportFormat = when (name?.substringAfterLast('.', "")?.lowercase()) {
        "csv", "tsv" -> ExportFormat.CSV
        else -> ExportFormat.XLSX
    }
}
