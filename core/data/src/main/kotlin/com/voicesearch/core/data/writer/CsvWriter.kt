package com.voicesearch.core.data.writer

import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

/**
 * Plain-CSV writer.
 *
 * - Comma delimiter (default Excel + parser auto-detects either way).
 * - Fields quoted only when they contain `,`, `"`, or a newline — keeps
 *   the file readable when nothing dangerous is in there.
 * - Always emits CRLF line endings per RFC 4180 so Excel-on-Windows is happy.
 * - UTF-8 with BOM so Excel-on-Windows shows Cyrillic correctly.
 */
class CsvWriter @Inject constructor() : TableWriter {

    override fun write(headers: List<String>, rows: List<List<String>>, output: OutputStream) {
        try {
            // BOM helps Excel-on-Windows pick UTF-8.
            output.write(BOM)
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.appendRow(headers)
                rows.forEach { writer.appendRow(it) }
                writer.flush()
            }
        } catch (e: Exception) {
            throw TableWriteException("Не удалось записать CSV: ${e.message}", e)
        }
    }

    private fun OutputStreamWriter.appendRow(cells: List<String>) {
        cells.joinTo(this, separator = ",", postfix = "\r\n", transform = ::quote)
    }

    private fun quote(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private companion object {
        // 0xEF 0xBB 0xBF
        val BOM: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
