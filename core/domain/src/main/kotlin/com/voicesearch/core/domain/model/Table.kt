package com.voicesearch.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Core domain models that the rest of the app talks to.
 * Pure Kotlin — no Android, no DB, no network.
 *
 * Filled out progressively as later stages land. This is the contract surface.
 */

@Serializable
data class Table(
    val id: String,
    val name: String,
    val source: TableSource,
    val headers: List<String>,
    val rowCount: Int,
)

@Serializable
sealed interface TableSource {
    @Serializable
    data class YandexDisk(val publicUrl: String?, val remotePath: String?) : TableSource

    @Serializable
    data class LocalFile(val originalFileName: String) : TableSource
}

@Serializable
data class TableSettings(
    val tableId: String,
    val searchColumnIndex: Int,
    val markColumnIndex: Int,
    val nameColumnIndex: Int,
    val startRowIndex: Int,
    val successMarker: String,
    val prefix: PrefixHint? = null,
    val autoSync: Boolean = false,
)

@Serializable
sealed interface PrefixHint {
    /** All values share a fixed prefix; user speaks the last N chars. */
    @Serializable
    data class FixedSuffix(val prefix: String, val suffixLength: Int) : PrefixHint

    /** Lengths vary; user speaks "up to N" trailing chars. */
    @Serializable
    data class VariableSuffix(val prefix: String, val maxSuffixLength: Int) : PrefixHint

    /** No useful prefix — match full value. */
    @Serializable
    data object FullMatch : PrefixHint
}
