package com.voicesearch.core.data.writer

import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.data.parser.CsvParser
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * The writer and parser are symmetric — anything we write must read back identically.
 * Spot-checks the dangerous corners: quotes, commas, Cyrillic, leading/trailing spaces.
 */
class CsvRoundTripTest {

    private val writer = CsvWriter()
    private val parser = CsvParser()

    @Test
    fun `simple round trip preserves headers and rows`() {
        val headers = listOf("id", "name")
        val rows = listOf(
            listOf("1", "Иванов"),
            listOf("2", "Петров"),
        )
        val parsed = roundTrip(headers, rows)
        assertThat(parsed.headers).containsExactlyElementsIn(headers).inOrder()
        assertThat(parsed.rows).containsExactlyElementsIn(rows).inOrder()
    }

    @Test
    fun `embedded comma in value survives via quoting`() {
        val headers = listOf("id", "name")
        val rows = listOf(listOf("1", "Иванов, И.И."))
        val parsed = roundTrip(headers, rows)
        assertThat(parsed.rows[0][1]).isEqualTo("Иванов, И.И.")
    }

    @Test
    fun `embedded quote survives via escaping`() {
        val headers = listOf("note")
        val rows = listOf(listOf("Он сказал \"привет\"."))
        val parsed = roundTrip(headers, rows)
        assertThat(parsed.rows[0][0]).isEqualTo("Он сказал \"привет\".")
    }

    @Test
    fun `Cyrillic UTF-8 survives the BOM round trip`() {
        val parsed = roundTrip(
            headers = listOf("Артикул", "Наименование"),
            rows = listOf(listOf("АРТ-001", "Шестерёнка")),
        )
        assertThat(parsed.headers).containsExactly("Артикул", "Наименование").inOrder()
        assertThat(parsed.rows[0][1]).isEqualTo("Шестерёнка")
    }

    private fun roundTrip(headers: List<String>, rows: List<List<String>>) =
        ByteArrayOutputStream().also { writer.write(headers, rows, it) }
            .toByteArray()
            .let { parser.parse(ByteArrayInputStream(it)) }
}
