package com.voicesearch.core.data.parser

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * Minimal RFC-4180-ish CSV reader.
 *
 * Handles:
 *  - Both `,` and `;` as delimiters (auto-detected from the first line —
 *    Excel-from-Russian-locale exports use `;`).
 *  - Double-quote escaped fields including embedded delimiters and newlines.
 *  - UTF-8 with optional BOM.
 *  - Trailing empty rows are dropped.
 *
 * First non-empty row is taken as the header. Subsequent rows are padded /
 * truncated to header length so every row has consistent shape downstream.
 */
class CsvParser @Inject constructor() : TableParser {

    override fun parse(input: InputStream): ParsedTable = try {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val firstLine = reader.readLine()?.stripBom() ?: throw TableParseException("Файл пуст")
        val delimiter = detectDelimiter(firstLine)
        val rest = reader.readText()
        // Reconstitute full text so the tokenizer handles multi-line quoted fields uniformly.
        val rows = tokenise("$firstLine\n$rest", delimiter)
            .dropLastWhile { it.all(String::isBlank) }

        if (rows.isEmpty()) throw TableParseException("Не нашли ни одной строки")

        val headers = rows.first()
        val width = headers.size
        val data = rows.drop(1).map { row ->
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
        throw TableParseException("Не удалось прочитать CSV: ${e.message}", e)
    }

    private fun detectDelimiter(firstLine: String): Char {
        // Count commas vs semicolons in the header. Whichever is more frequent wins.
        val commas = firstLine.count { it == ',' }
        val semis = firstLine.count { it == ';' }
        return if (semis > commas) ';' else ','
    }

    /**
     * Tokenises CSV text into rows of fields. State machine handles quoted fields
     * with embedded delimiters / newlines / escaped quotes.
     */
    private fun tokenise(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == delimiter -> {
                    current.add(field.toString()); field.setLength(0)
                }
                c == '\r' -> Unit // ignore CR — we split on LF
                c == '\n' -> {
                    current.add(field.toString()); field.setLength(0)
                    rows.add(current); current = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        // Tail row (no trailing newline)
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            rows.add(current)
        }
        return rows
    }

    private fun String.stripBom(): String =
        if (isNotEmpty() && this[0] == '﻿') substring(1) else this

    @Suppress("unused")
    private fun BufferedReader.readWithCharset(charset: Charset) = this
}
