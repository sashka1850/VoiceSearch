package com.voicesearch.core.data.mapper

import com.voicesearch.core.data.db.entity.TableEntity
import com.voicesearch.core.data.db.entity.TableRowEntity
import com.voicesearch.core.data.db.entity.TableSettingsEntity
import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.repository.TableRow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Pure functions between persistence entities and domain models.
 *
 * Kept stateless and side-effect-free so they're trivial to test and reuse
 * (e.g. mapping a list with `map { it.toDomain(json) }` without any DI gymnastics).
 *
 * The [Json] instance is injected — production wiring uses the project-wide
 * configuration from `:core:network`'s NetworkModule for consistency.
 */

private val stringListSerializer = ListSerializer(String.serializer())

// region Table

fun TableEntity.toDomain(json: Json): Table = Table(
    id = id,
    name = name,
    source = json.decodeFromString(TableSource.serializer(), sourceJson),
    headers = json.decodeFromString(stringListSerializer, headersJson),
    rowCount = rowCount,
)

fun Table.toEntity(json: Json, importedAt: Long, originalFileName: String?): TableEntity = TableEntity(
    id = id,
    name = name,
    sourceJson = json.encodeToString(TableSource.serializer(), source),
    headersJson = json.encodeToString(stringListSerializer, headers),
    rowCount = rowCount,
    importedAt = importedAt,
    originalFileName = originalFileName,
)

// endregion

// region TableRow

fun TableRowEntity.toDomain(json: Json): TableRow = TableRow(
    id = id,
    tableId = tableId,
    rowIndex = rowIndex,
    cells = json.decodeFromString(stringListSerializer, cellsJson),
    isMarked = isMarked,
    markedAt = markedAt,
)

fun buildRowEntity(
    tableId: String,
    rowIndex: Int,
    cells: List<String>,
    json: Json,
): TableRowEntity = TableRowEntity(
    tableId = tableId,
    rowIndex = rowIndex,
    cellsJson = json.encodeToString(stringListSerializer, cells),
)

// endregion

// region TableSettings

fun TableSettingsEntity.toDomain(json: Json): TableSettings = TableSettings(
    tableId = tableId,
    searchColumnIndex = searchColumnIndex,
    markColumnIndex = markColumnIndex,
    nameColumnIndex = nameColumnIndex,
    startRowIndex = startRowIndex,
    successMarker = successMarker,
    prefix = prefixHintJson?.let { json.decodeFromString(PrefixHint.serializer(), it) },
    autoSync = autoSync,
)

fun TableSettings.toEntity(json: Json): TableSettingsEntity = TableSettingsEntity(
    tableId = tableId,
    searchColumnIndex = searchColumnIndex,
    markColumnIndex = markColumnIndex,
    nameColumnIndex = nameColumnIndex,
    startRowIndex = startRowIndex,
    successMarker = successMarker,
    prefixHintJson = prefix?.let { json.encodeToString(PrefixHint.serializer(), it) },
    autoSync = autoSync,
)

// endregion
