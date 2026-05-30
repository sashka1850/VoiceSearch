package com.voicesearch.core.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * XLSX = a ZIP of XML parts. We read just two of them:
 *  - `xl/sharedStrings.xml` — string table referenced by cell `t="s"` indices.
 *  - `xl/worksheets/sheet1.xml` — the actual rows and cells.
 *
 * Stream parsing via Android's [XmlPullParser] keeps memory flat even on
 * 100k-row files — no DOM tree, no third-party dep.
 *
 * What we deliberately don't try to support (yet):
 *  - Multiple sheets — we always read sheet1.
 *  - Date formatting — Excel stores dates as numbers; we emit the raw number.
 *  - Inline rich text — only the plain `<t>` runs are concatenated.
 *  - Formula results that aren't cached — empty string.
 *
 * Header row = first non-empty row. Following rows are padded / truncated
 * to header width so downstream code can index by column safely.
 */
class XlsxParser @Inject constructor() : TableParser {

    override fun parse(input: InputStream): ParsedTable = try {
        val zip = ZipInputStream(input)
        var sharedStringsXml: String? = null
        var sheetXml: String? = null
        var firstSheetXml: String? = null

        zip.use { z ->
            var entry: ZipEntry? = z.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> sharedStringsXml = z.readText()
                    entry.name == "xl/worksheets/sheet1.xml" -> sheetXml = z.readText()
                    entry.name.startsWith("xl/worksheets/sheet") &&
                        entry.name.endsWith(".xml") &&
                        firstSheetXml == null -> firstSheetXml = z.readText()
                }
                z.closeEntry()
                entry = z.nextEntry
            }
        }

        val sheetSource = sheetXml ?: firstSheetXml
            ?: throw TableParseException("В файле нет листа sheet1.xml")
        val strings = sharedStringsXml?.let(::parseSharedStrings) ?: emptyList()

        val rawRows = parseSheet(sheetSource, strings)
        if (rawRows.isEmpty()) throw TableParseException("Лист пуст")

        val headers = rawRows.first()
        val width = headers.size
        val data = rawRows.drop(1)
            .filter { row -> row.any(String::isNotBlank) }
            .map { row ->
                when {
                    row.size == width -> row
                    row.size < width -> row + List(width - row.size) { "" }
                    else -> row.take(width)
                }
            }
        ParsedTable(headers = headers, rows = data)
    } catch (e: TableParseException) {
        throw e
    } catch (e: Exception) {
        throw TableParseException("Не удалось прочитать XLSX: ${e.message}", e)
    }

    private fun ZipInputStream.readText(): String =
        readBytes().toString(Charsets.UTF_8)

    /** sharedStrings.xml is a flat `<sst><si><t>value</t></si>…</sst>`. */
    private fun parseSharedStrings(xml: String): List<String> {
        val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
        val strings = ArrayList<String>()
        val currentSi = StringBuilder()
        var inSi = false
        var inT = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inSi = true; currentSi.setLength(0) }
                    "t" -> if (inSi) inT = true
                }
                XmlPullParser.TEXT -> if (inT) currentSi.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> { strings.add(currentSi.toString()); inSi = false }
                }
            }
        }
        return strings
    }

    /**
     * sheet1.xml row layout:
     *   <row r="2">
     *     <c r="A2" t="s"><v>5</v></c>      ← string ref into sharedStrings
     *     <c r="B2"><v>12345</v></c>        ← inline number
     *     <c r="C2" t="inlineStr"><is><t>literal</t></is></c>
     *   </row>
     *
     * Column index is derived from the `r` attribute (e.g. `B2` → col 1)
     * because rows may skip empty cells entirely.
     */
    private fun parseSheet(xml: String, strings: List<String>): List<List<String>> {
        val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
        val rows = ArrayList<List<String>>()
        var currentRow: ArrayList<String>? = null
        var maxColInRow = -1

        var cellType: String? = null
        var cellColumn = -1
        var inV = false
        var inInlineT = false
        val cellText = StringBuilder()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> { currentRow = ArrayList(); maxColInRow = -1 }
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t")
                        cellColumn = columnIndexFromRef(parser.getAttributeValue(null, "r"))
                        cellText.setLength(0)
                    }
                    "v" -> inV = true
                    "t" -> if (cellType == "inlineStr") inInlineT = true
                }
                XmlPullParser.TEXT -> when {
                    inV -> cellText.append(parser.text)
                    inInlineT -> cellText.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inV = false
                    "t" -> inInlineT = false
                    "c" -> {
                        val row = currentRow ?: continue
                        // Pad empty cells if this cell is past where we are.
                        while (row.size < cellColumn) row.add("")
                        val value = when (cellType) {
                            "s" -> {
                                val idx = cellText.toString().toIntOrNull()
                                if (idx != null && idx in strings.indices) strings[idx] else ""
                            }
                            "b" -> if (cellText.toString() == "1") "TRUE" else "FALSE"
                            else -> cellText.toString()
                        }
                        row.add(value)
                        maxColInRow = cellColumn
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
        }
        return rows
    }

    /** "B2" → 1, "AA1" → 26, "BC10" → 54. */
    private fun columnIndexFromRef(ref: String?): Int {
        if (ref.isNullOrEmpty()) return 0
        var col = 0
        for (c in ref) {
            if (c in 'A'..'Z') col = col * 26 + (c - 'A' + 1)
            else if (c in 'a'..'z') col = col * 26 + (c - 'a' + 1)
            else break
        }
        return col - 1
    }
}
