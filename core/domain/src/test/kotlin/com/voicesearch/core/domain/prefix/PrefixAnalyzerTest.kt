package com.voicesearch.core.domain.prefix

import com.google.common.truth.Truth.assertThat
import com.voicesearch.core.domain.model.PrefixHint
import org.junit.Test

class PrefixAnalyzerTest {

    @Test
    fun `same-length values with strong common prefix gives FixedSuffix`() {
        val result = PrefixAnalyzer.analyze(
            listOf("1234567890", "1234500000", "1234511111", "1234599999"),
        )
        assertThat(result).isEqualTo(PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5))
    }

    @Test
    fun `mixed-length values with common prefix gives VariableSuffix`() {
        val result = PrefixAnalyzer.analyze(
            listOf("123450000", "1234500001", "12345000099", "12345"),
        )
        // longest value has 11 chars, prefix length 5 gives max suffix 6
        assertThat(result).isInstanceOf(PrefixHint.VariableSuffix::class.java)
        result as PrefixHint.VariableSuffix
        assertThat(result.prefix).isEqualTo("12345")
        assertThat(result.maxSuffixLength).isEqualTo(6)
    }

    @Test
    fun `no common prefix gives FullMatch`() {
        val result = PrefixAnalyzer.analyze(listOf("apple", "banana", "carrot", "durian"))
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `single value gives FullMatch because nothing to compare against`() {
        val result = PrefixAnalyzer.analyze(listOf("12345678"))
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `empty list gives FullMatch`() {
        val result = PrefixAnalyzer.analyze(emptyList())
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `all empty strings gives FullMatch`() {
        val result = PrefixAnalyzer.analyze(listOf("", "", "  ", ""))
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `whitespace is trimmed before analysis`() {
        val result = PrefixAnalyzer.analyze(
            listOf("  1234567890  ", "1234511111", "  1234599999"),
        )
        assertThat(result).isEqualTo(PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 5))
    }

    @Test
    fun `tiny prefix below threshold gives FullMatch`() {
        // Common prefix is just "12" (length 2). Default minPrefixLength is 3.
        val result = PrefixAnalyzer.analyze(
            listOf("12abcdef00", "12ghijkl11", "12mnopqr99"),
        )
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `prefix exactly at minimum length is accepted`() {
        val result = PrefixAnalyzer.analyze(
            listOf("123abc", "123def", "123ghi"),
            minPrefixLength = 3,
        )
        assertThat(result).isEqualTo(PrefixHint.FixedSuffix(prefix = "123", suffixLength = 3))
    }

    @Test
    fun `one outlier in 10 values still detects prefix at default 90 percent threshold`() {
        val nine = (1..9).map { "12345_$it" }
        val outlier = "99999_X"
        val result = PrefixAnalyzer.analyze(nine + outlier)
        // 9/10 = 90% agreement on prefix "12345_" gives still detected.
        assertThat(result).isInstanceOf(PrefixHint.FixedSuffix::class.java)
        result as PrefixHint.FixedSuffix
        assertThat(result.prefix).isEqualTo("12345_")
    }

    @Test
    fun `two outliers in 10 values fall below threshold gives shorter or no prefix`() {
        val eight = (1..8).map { "12345_$it" }
        val outliers = listOf("99999_X", "88888_Y")
        val result = PrefixAnalyzer.analyze(eight + outliers)
        // 8/10 = 80%, below default 90% gives first char "1"/"9"/"8" doesn't reach threshold either
        // gives result should be FullMatch (no useful prefix).
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `lower threshold relaxes detection`() {
        val seven = (1..7).map { "12345_$it" }
        val outliers = listOf("99999_X", "88888_Y", "77777_Z")
        val result = PrefixAnalyzer.analyze(seven + outliers, minAgreement = 0.7)
        // 7/10 = 70% gives prefix detected.
        assertThat(result).isInstanceOf(PrefixHint.FixedSuffix::class.java)
        result as PrefixHint.FixedSuffix
        assertThat(result.prefix).isEqualTo("12345_")
    }

    @Test
    fun `prefix that equals every value gives FullMatch because suffix length is 0`() {
        val result = PrefixAnalyzer.analyze(listOf("AAAA", "AAAA", "AAAA", "AAAA"))
        assertThat(result).isEqualTo(PrefixHint.FullMatch)
    }

    @Test
    fun `real-world 13-digit barcodes with one shared retail prefix`() {
        // Diverge after position 6 ("4607012"): if all values happened to share
        // a longer common run, the analyzer would correctly pick that up too.
        val barcodes = listOf(
            "4607012001234",
            "4607012005678",
            "4607012099999",
            "4607012187654",
        )
        val result = PrefixAnalyzer.analyze(barcodes)
        assertThat(result).isInstanceOf(PrefixHint.FixedSuffix::class.java)
        result as PrefixHint.FixedSuffix
        assertThat(result.prefix).isEqualTo("4607012")
        assertThat(result.suffixLength).isEqualTo(6)
    }

    @Test
    fun `Cyrillic prefix works the same way`() {
        val result = PrefixAnalyzer.analyze(
            listOf("Артикул-001", "Артикул-042", "Артикул-999"),
        )
        assertThat(result).isInstanceOf(PrefixHint.FixedSuffix::class.java)
        result as PrefixHint.FixedSuffix
        assertThat(result.prefix).isEqualTo("Артикул-")
        assertThat(result.suffixLength).isEqualTo(3)
    }

    @Test
    fun `mostly-fixed length with one shorter gives VariableSuffix`() {
        val values = listOf(
            "1234500001",
            "1234500002",
            "1234500003",
            "1234500004",
            "123456",        // shorter
        )
        val result = PrefixAnalyzer.analyze(values)
        assertThat(result).isInstanceOf(PrefixHint.VariableSuffix::class.java)
        result as PrefixHint.VariableSuffix
        assertThat(result.prefix).isEqualTo("12345")
        assertThat(result.maxSuffixLength).isEqualTo(5)
    }

    @Test
    fun `values shorter than minPrefixLength are excluded`() {
        val result = PrefixAnalyzer.analyze(
            listOf("X", "Y", "12345001", "12345987", "12345xyz"),
        )
        // "X" and "Y" filtered out by length <= minPrefixLength; remaining three
        // agree on "12345" but diverge at position 5, so suffixLength is 3.
        assertThat(result).isEqualTo(PrefixHint.FixedSuffix(prefix = "12345", suffixLength = 3))
    }
}
