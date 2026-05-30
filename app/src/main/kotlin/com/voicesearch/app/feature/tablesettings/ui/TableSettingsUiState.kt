package com.voicesearch.app.feature.tablesettings.ui

import com.voicesearch.core.domain.model.PrefixHint

/**
 * Snapshot of everything the settings screen renders.
 *
 * [headers] and [previewRows] are the slice of the table we show as the
 * top "what does it look like" strip; the dropdowns reuse [headers].
 */
data class TableSettingsUiState(
    val isLoading: Boolean = true,
    val tableName: String = "",
    val headers: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val searchColumnIndex: Int = 0,
    val markColumnIndex: Int = 0,
    val nameColumnIndex: Int = 0,
    val startRowIndex: Int = 0,
    val successMarker: String = "Есть",
    val prefixHint: PrefixHint = PrefixHint.FullMatch,
    val autoSyncSupported: Boolean = false,
    val autoSync: Boolean = false,
    val isSaving: Boolean = false,
    val savedTableId: String? = null,
    val errorMessage: String? = null,
)
