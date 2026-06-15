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
    /**
     * Non-empty cell count in the currently selected search column (data rows
     * only — headers live in [headers] and aren't counted here).
     * Must be ≥1 to enable saving: there must be at least one value to find.
     */
    val searchColumnFilledCount: Int = 0,
    /**
     * Non-empty cell count in the currently selected mark column. Must be 0:
     * we refuse to use a column that already has user data, because marking
     * a row writes into it and would overwrite whatever was there.
     */
    val markColumnFilledCount: Int = 0,
    /** Total data rows we counted across — used to phrase the helper text. */
    val totalDataRows: Int = 0,
) {
    /** Aggregated gate for the Save button. */
    val canSave: Boolean
        get() = !isLoading &&
            !isSaving &&
            searchColumnFilledCount > 0 &&
            markColumnFilledCount == 0 &&
            successMarker.isNotBlank()
}
