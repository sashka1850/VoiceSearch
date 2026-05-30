package com.voicesearch.core.data.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Hand-builds the minimum subset of XLSX parts the parser actually reads:
 *   - xl/sharedStrings.xml
 *   - xl/worksheets/sheet1.xml
 * Other parts (content types, rels, workbook.xml) aren't touched by our
 * reader, so we omit them — keeps the fixture readable.
 *
 * Needs Robolectric because [com.voicesearch.core.data.parser.XlsxParser]
 * uses `android.util.Xml.newPullParser()` which only exists on the
 * Android classpath.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class XlsxParserTest {

    private val parser = XlsxParser()

    @Test
    fun `reads headers and rows with shared strings`() {
        val xlsx = buildXlsx(
            sharedStrings = listOf("№", "Артикул", "Наименование", "Иванов И.И.", "Петров П.П."),
            sheetXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="s"><v>1</v></c>
                      <c r="C1" t="s"><v>2</v></c>
                    </row>
                    <row r="2">
                      <c r="A2"><v>1</v></c>
                      <c r="B2" t="inlineStr"><is><t>12345 67890</t></is></c>
                      <c r="C2" t="s"><v>3</v></c>
                    </row>
                    <row r="3">
                      <c r="A3"><v>2</v></c>
                      <c r="B3" t="inlineStr"><is><t>12345 11111</t></is></c>
                      <c r="C3" t="s"><v>4</v></c>
                    </row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
        )

        val table = parser.parse(ByteArrayInputStream(xlsx))

        assertThat(table.headers).containsExactly("№", "Артикул", "Наименование").inOrder()
        assertThat(table.rows).hasSize(2)
        assertThat(table.rows[0]).containsExactly("1", "12345 67890", "Иванов И.И.").inOrder()
        assertThat(table.rows[1]).containsExactly("2", "12345 11111", "Петров П.П.").inOrder()
    }

    @Test
    fun `pads cells when row skips columns`() {
        val xlsx = buildXlsx(
            sharedStrings = listOf("a", "b", "c"),
            sheetXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="s"><v>1</v></c>
                      <c r="C1" t="s"><v>2</v></c>
                    </row>
                    <row r="2">
                      <c r="A2"><v>1</v></c>
                      <c r="C2"><v>3</v></c>
                    </row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
        )

        val table = parser.parse(ByteArrayInputStream(xlsx))

        // The middle cell is empty, last cell present.
        assertThat(table.rows[0]).containsExactly("1", "", "3").inOrder()
    }

    @Test
    fun `boolean cells become TRUE FALSE`() {
        val xlsx = buildXlsx(
            sharedStrings = listOf("flag"),
            sheetXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1"><c r="A1" t="s"><v>0</v></c></row>
                    <row r="2"><c r="A2" t="b"><v>1</v></c></row>
                    <row r="3"><c r="A3" t="b"><v>0</v></c></row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
        )

        val table = parser.parse(ByteArrayInputStream(xlsx))

        assertThat(table.rows[0][0]).isEqualTo("TRUE")
        assertThat(table.rows[1][0]).isEqualTo("FALSE")
    }

    private fun buildXlsx(sharedStrings: List<String>, sheetXml: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putEntry("xl/sharedStrings.xml", buildSharedStringsXml(sharedStrings))
            zip.putEntry("xl/worksheets/sheet1.xml", sheetXml)
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.putEntry(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun buildSharedStringsXml(strings: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        strings.forEach { s ->
            append("<si><t>").append(s.escapeXml()).append("</t></si>")
        }
        append("</sst>")
    }

    private fun String.escapeXml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
