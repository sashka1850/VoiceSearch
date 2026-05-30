package com.voicesearch.core.data.writer

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Minimal XLSX writer.
 *
 * Builds a valid Office Open XML SpreadsheetML workbook from scratch:
 *  - `[Content_Types].xml` + relationships (the wrapper that makes a ZIP
 *    "a workbook" instead of just a ZIP).
 *  - A single sheet `xl/worksheets/sheet1.xml` with inline strings (no
 *    sharedStrings.xml — keeps writer logic simple, file size is a few
 *    KB bigger for very long files, acceptable trade-off).
 *  - A minimal `xl/styles.xml` because Excel refuses to open a file
 *    without one.
 *
 * Verified round-trippable through our own [com.voicesearch.core.data.parser.XlsxParser]
 * in unit tests (XlsxRoundTripTest).
 */
class XlsxWriter @Inject constructor() : TableWriter {

    override fun write(headers: List<String>, rows: List<List<String>>, output: OutputStream) {
        try {
            ZipOutputStream(output).use { zip ->
                zip.put("[Content_Types].xml", CONTENT_TYPES_XML)
                zip.put("_rels/.rels", ROOT_RELS_XML)
                zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
                zip.put("xl/workbook.xml", WORKBOOK_XML)
                zip.put("xl/styles.xml", STYLES_XML)
                zip.put("xl/worksheets/sheet1.xml", buildSheetXml(headers, rows))
            }
        } catch (e: Exception) {
            throw TableWriteException("Не удалось записать XLSX: ${e.message}", e)
        }
    }

    private fun buildSheetXml(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder(64 * (rows.size + 1))
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")
        appendRow(sb, headers, oneBasedRow = 1)
        rows.forEachIndexed { idx, cells ->
            appendRow(sb, cells, oneBasedRow = idx + 2)
        }
        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun appendRow(sb: StringBuilder, cells: List<String>, oneBasedRow: Int) {
        sb.append("<row r=\"").append(oneBasedRow).append("\">")
        cells.forEachIndexed { col, value ->
            val ref = columnRef(col) + oneBasedRow
            // Always inlineStr — cell types like number/date would need extra style
            // bookkeeping; keeping everything as text matches how the parser reads.
            sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t")
            if (value.isNotEmpty() && (value.first().isWhitespace() || value.last().isWhitespace())) {
                sb.append(" xml:space=\"preserve\"")
            }
            sb.append('>')
            sb.append(value.escapeXml())
            sb.append("</t></is></c>")
        }
        sb.append("</row>")
    }

    /** 0-based column index → spreadsheet letters: 0=A, 25=Z, 26=AA, 27=AB. */
    private fun columnRef(zeroBased: Int): String {
        var n = zeroBased + 1
        val sb = StringBuilder()
        while (n > 0) {
            n -= 1
            sb.insert(0, ('A' + n % 26))
            n /= 26
        }
        return sb.toString()
    }

    private fun ZipOutputStream.put(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.escapeXml(): String {
        if (none { it == '&' || it == '<' || it == '>' }) return this
        val sb = StringBuilder(length + 8)
        for (c in this) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private companion object {
        const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
    }
}
