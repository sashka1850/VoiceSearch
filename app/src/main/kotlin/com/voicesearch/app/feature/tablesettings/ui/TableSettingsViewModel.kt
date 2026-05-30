package com.voicesearch.app.feature.tablesettings.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.app.navigation.Routes
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.prefix.PrefixAnalyzer
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableRow
import com.voicesearch.core.domain.repository.TableSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TableSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tableRepository: TableRepository,
    private val settingsRepository: TableSettingsRepository,
) : ViewModel() {

    private val tableId: String = checkNotNull(savedStateHandle[Routes.ARG_TABLE_ID]) {
        "TableSettingsScreen requires tableId argument"
    }

    private val _state = MutableStateFlow(TableSettingsUiState())
    val state: StateFlow<TableSettingsUiState> = _state.asStateFlow()

    /** Cached rows so column-selection changes can refresh the prefix hint cheaply. */
    private var cachedRows: List<TableRow> = emptyList()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val table = tableRepository.get(tableId)
            if (table == null) {
                _state.update { it.copy(isLoading = false, errorMessage = "Таблица не найдена") }
                return@launch
            }
            cachedRows = tableRepository.getRows(tableId)
            val existing = settingsRepository.get(tableId)
            val initial = existing ?: defaultsFor(table)

            _state.update {
                it.copy(
                    isLoading = false,
                    tableName = table.name,
                    headers = table.headers,
                    previewRows = cachedRows.take(PREVIEW_ROWS).map { row -> row.cells },
                    searchColumnIndex = initial.searchColumnIndex.coerceIn(0, lastIndex(table)),
                    markColumnIndex = initial.markColumnIndex.coerceIn(0, lastIndex(table)),
                    nameColumnIndex = initial.nameColumnIndex.coerceIn(0, lastIndex(table)),
                    startRowIndex = initial.startRowIndex.coerceAtLeast(0),
                    successMarker = initial.successMarker.ifBlank { "Есть" },
                    autoSyncSupported = table.source is TableSource.YandexDisk,
                    autoSync = initial.autoSync && table.source is TableSource.YandexDisk,
                    prefixHint = initial.prefix ?: analyzePrefix(initial.searchColumnIndex),
                )
            }
        }
    }

    private fun lastIndex(table: Table) = (table.headers.size - 1).coerceAtLeast(0)

    private fun defaultsFor(table: Table): TableSettings {
        val nCols = table.headers.size
        val searchIdx = 0
        val nameIdx = if (nCols > 1) 1 else 0
        val markIdx = (nCols - 1).coerceAtLeast(0)
        return TableSettings(
            tableId = table.id,
            searchColumnIndex = searchIdx,
            markColumnIndex = markIdx,
            nameColumnIndex = nameIdx,
            startRowIndex = 0,
            successMarker = "Есть",
            prefix = null,
            autoSync = false,
        )
    }

    private fun analyzePrefix(columnIndex: Int) = PrefixAnalyzer.analyze(
        values = cachedRows.mapNotNull { it.cells.getOrNull(columnIndex) },
    )

    fun onSearchColumnChange(index: Int) {
        _state.update {
            it.copy(
                searchColumnIndex = index,
                prefixHint = analyzePrefix(index),
            )
        }
    }

    fun onMarkColumnChange(index: Int) = _state.update { it.copy(markColumnIndex = index) }
    fun onNameColumnChange(index: Int) = _state.update { it.copy(nameColumnIndex = index) }
    fun onSuccessMarkerChange(value: String) = _state.update { it.copy(successMarker = value) }
    fun onStartRowChange(value: Int) = _state.update { it.copy(startRowIndex = value.coerceAtLeast(0)) }
    fun onAutoSyncChange(enabled: Boolean) = _state.update { it.copy(autoSync = enabled) }

    fun save() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSaving) return
        if (snapshot.successMarker.isBlank()) {
            _state.update { it.copy(errorMessage = "Маркер успеха не может быть пустым") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            settingsRepository.save(
                TableSettings(
                    tableId = tableId,
                    searchColumnIndex = snapshot.searchColumnIndex,
                    markColumnIndex = snapshot.markColumnIndex,
                    nameColumnIndex = snapshot.nameColumnIndex,
                    startRowIndex = snapshot.startRowIndex,
                    successMarker = snapshot.successMarker.trim(),
                    prefix = snapshot.prefixHint,
                    autoSync = snapshot.autoSync,
                ),
            )
            _state.update { it.copy(isSaving = false, savedTableId = tableId) }
        }
    }

    fun consumeError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun consumeSaved() {
        _state.update { it.copy(savedTableId = null) }
    }

    companion object {
        private const val PREVIEW_ROWS = 5
    }
}
