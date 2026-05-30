package com.voicesearch.core.domain.prefix

import com.voicesearch.core.domain.model.PrefixHint
import kotlin.math.ceil

/**
 * Looks at the values from a search column and figures out whether users can
 * speak just the *trailing* digits instead of the full ID.
 *
 * Three outcomes:
 *  - [PrefixHint.FixedSuffix] — every value has the same length **and** they
 *    share a non-trivial common prefix. Users can speak the last N characters
 *    deterministically.
 *  - [PrefixHint.VariableSuffix] — values share a common prefix but lengths
 *    differ. Users speak "up to N" trailing characters where N is the longest
 *    suffix in the dataset.
 *  - [PrefixHint.FullMatch] — nothing useful in common; users speak the full
 *    value.
 *
 * Robustness knobs:
 *  - [minPrefixLength] keeps us from claiming "5" is a useful prefix when
 *    every barcode happens to start with a digit by chance.
 *  - [minAgreement] = 0.9 means a couple of typos or stray rows don't destroy
 *    the analysis: we still detect the prefix if ≥90% of values agree.
 *  - Empty / whitespace-only values are dropped before analysis.
 */
object PrefixAnalyzer {

    private const val DEFAULT_MIN_PREFIX_LENGTH = 3
    private const val DEFAULT_MIN_AGREEMENT = 0.9

    fun analyze(
        values: List<String>,
        minPrefixLength: Int = DEFAULT_MIN_PREFIX_LENGTH,
        minAgreement: Double = DEFAULT_MIN_AGREEMENT,
    ): PrefixHint {
        val cleaned = values.asSequence()
            .map { it.trim() }
            .filter { it.length > minPrefixLength } // need at least prefix+1 char to be interesting
            .toList()

        if (cleaned.size < 2) return PrefixHint.FullMatch

        val prefix = findAgreedPrefix(
            values = cleaned,
            minAgreement = minAgreement,
        )
        if (prefix.length < minPrefixLength) return PrefixHint.FullMatch

        val lengthsAfterPrefix = cleaned
            .filter { it.startsWith(prefix) }
            .map { it.length - prefix.length }

        val allSameLength = lengthsAfterPrefix.toSet().size == 1
        val maxSuffix = lengthsAfterPrefix.max()

        return when {
            maxSuffix == 0 -> PrefixHint.FullMatch
            allSameLength -> PrefixHint.FixedSuffix(prefix = prefix, suffixLength = maxSuffix)
            else -> PrefixHint.VariableSuffix(prefix = prefix, maxSuffixLength = maxSuffix)
        }
    }

    /**
     * Find the longest prefix shared by at least [minAgreement] fraction of
     * the input. Walks character by character, dropping out values as soon as
     * they diverge, until fewer than the threshold remain.
     */
    @Suppress("LoopWithTooManyJumpStatements") // state-machine: each break terminates a distinct cause
    private fun findAgreedPrefix(values: List<String>, minAgreement: Double): String {
        if (values.isEmpty()) return ""
        val total = values.size
        // ceil — "at least 90%" means a list of 5 needs all 5 to agree, not 4.
        val threshold = ceil(total * minAgreement).toInt().coerceAtLeast(2)

        // For each character position, find the most common char among
        // still-agreeing values. Stop when the majority falls below threshold.
        var alive = values
        val builder = StringBuilder()
        var pos = 0
        while (true) {
            val candidates = alive.mapNotNull { v -> v.getOrNull(pos) }
            if (candidates.size < threshold) break
            val (winnerChar, winnerCount) = candidates
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?: break
            if (winnerCount < threshold) break
            builder.append(winnerChar)
            alive = alive.filter { it.getOrNull(pos) == winnerChar }
            if (alive.size < threshold) {
                // We just dropped below threshold — undo last char.
                builder.deleteCharAt(builder.length - 1)
                break
            }
            pos++
        }
        return builder.toString()
    }
}
