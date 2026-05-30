package com.voicesearch.core.data.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvParserTest {

    private val parser = CsvParser()

    @Test
    fun `comma-delimited file parses headers and rows`() {
        val csv = """
            id,name,phone
            1,Иванов И.И.,89001234567
            2,Петров П.П.,89001234568
        """.trimIndent()

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.headers).containsExactly("id", "name", "phone").inOrder()
        assertThat(table.rows).hasSize(2)
        assertThat(table.rows[0]).containsExactly("1", "Иванов И.И.", "89001234567").inOrder()
        assertThat(table.rows[1]).containsExactly("2", "Петров П.П.", "89001234568").inOrder()
    }

    @Test
    fun `semicolon-delimited file is auto-detected`() {
        val csv = """
            id;name
            1;Иванов
            2;Петров
        """.trimIndent()

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.headers).containsExactly("id", "name").inOrder()
        assertThat(table.rows).containsExactly(
            listOf("1", "Иванов"),
            listOf("2", "Петров"),
        ).inOrder()
    }

    @Test
    fun `quoted field with embedded comma survives`() {
        val csv = "id,full_name\n" +
            "1,\"Иванов, И.И.\"\n" +
            "2,\"Петров П.П.\"\n"

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.rows[0]).containsExactly("1", "Иванов, И.И.").inOrder()
        assertThat(table.rows[1]).containsExactly("2", "Петров П.П.").inOrder()
    }

    @Test
    fun `quoted field with escaped double quote`() {
        val csv = "id,note\n" +
            "1,\"Он сказал \"\"привет\"\".\"\n"

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.rows[0]).containsExactly("1", "Он сказал \"привет\".").inOrder()
    }

    @Test
    fun `rows shorter than header are padded with empty cells`() {
        val csv = """
            id,name,phone
            1,Иванов
            2,Петров,89001234567
        """.trimIndent()

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.rows[0]).containsExactly("1", "Иванов", "").inOrder()
        assertThat(table.rows[1]).containsExactly("2", "Петров", "89001234567").inOrder()
    }

    @Test
    fun `UTF-8 BOM is stripped from first header`() {
        val bom = "﻿"
        val csv = "${bom}id,name\n1,Иванов\n"

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.headers.first()).isEqualTo("id")
    }

    @Test(expected = TableParseException::class)
    fun `empty input throws TableParseException`() {
        parser.parse("".byteInputStream())
    }

    @Test
    fun `trailing blank lines are dropped`() {
        val csv = "id,name\n1,a\n2,b\n\n\n"

        val table = parser.parse(csv.byteInputStream())

        assertThat(table.rows).hasSize(2)
    }
}
