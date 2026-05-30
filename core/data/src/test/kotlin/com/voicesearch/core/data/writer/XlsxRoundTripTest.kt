package com.voicesearch.core.data.writer

import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.data.parser.XlsxParser
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Writes an in-memory XLSX with [XlsxWriter] then immediately reads it back
 * through our parser. If both stay in sync the test passes — divergence
 * (cell coordinates, escaping, whitespace) shows up loudly.
 *
 * Needs Robolectric because [XlsxParser] uses `android.util.Xml`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class XlsxRoundTripTest {

    private val writer = XlsxWriter()
    private val parser = XlsxParser()

    @Test
    fun `headers and rows preserved`() {
        val headers = listOf("№", "Артикул", "Наименование")
        val rows = listOf(
            listOf("1", "12345 67890", "Иванов И.И."),
            listOf("2", "12345 11111", "Петров П.П."),
            listOf("3", "12345 99999", "Сидоров С."),
        )
        val parsed = roundTrip(headers, rows)
        assertThat(parsed.headers).containsExactlyElementsIn(headers).inOrder()
        assertThat(parsed.rows).hasSize(3)
        assertThat(parsed.rows[0]).containsExactly("1", "12345 67890", "Иванов И.И.").inOrder()
        assertThat(parsed.rows[2]).containsExactly("3", "12345 99999", "Сидоров С.").inOrder()
    }

    @Test
    fun `xml-special chars survive escaping`() {
        val parsed = roundTrip(
            headers = listOf("name", "note"),
            rows = listOf(listOf("A&B", "<script>alert(1)</script>")),
        )
        assertThat(parsed.rows[0][0]).isEqualTo("A&B")
        assertThat(parsed.rows[0][1]).isEqualTo("<script>alert(1)</script>")
    }

    @Test
    fun `wide row exercises the column-letter encoder`() {
        // 30 columns crosses the AA boundary at column 27.
        val headers = (1..30).map { "col$it" }
        val rows = listOf((1..30).map { "v$it" })
        val parsed = roundTrip(headers, rows)
        assertThat(parsed.headers).hasSize(30)
        assertThat(parsed.headers.last()).isEqualTo("col30")
        assertThat(parsed.rows[0].last()).isEqualTo("v30")
    }

    private fun roundTrip(headers: List<String>, rows: List<List<String>>) =
        ByteArrayOutputStream().also { writer.write(headers, rows, it) }
            .toByteArray()
            .let { parser.parse(ByteArrayInputStream(it)) }
}
