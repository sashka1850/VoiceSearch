package com.voicesearch.app.ui.tableslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TablesListViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    /**
     * Builds the row list: for every imported table, observe its marked-count
     * so the badge stays current as the user marks rows on another screen.
     * Combined with the persisted currentTableId to mark which row is active.
     */
    val state: StateFlow<TablesListUiState> = combine(
        tableRepository.observeAll(),
        appPreferences.currentTableId,
    ) { tables, currentId -> tables to currentId }
        .flatMapLatest { (tables, currentId) ->
            if (tables.isEmpty()) {
                flowOf(TablesListUiState(items = emptyList(), isLoading = false))
            } else {
                combine(
                    tables.map { table ->
                        tableRepository.observeMarkedCount(table.id)
                            .map { marked -> TableListItem(table, marked, table.id == currentId) }
                    },
                ) { array -> TablesListUiState(items = array.toList(), isLoading = false) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TablesListUiState(items = emptyList(), isLoading = true),
        )

    fun switchTo(tableId: String) {
        viewModelScope.launch { appPreferences.setCurrentTableId(tableId) }
    }

    fun delete(tableId: String) {
        viewModelScope.launch { tableRepository.delete(tableId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class TablesListUiState(
    val items: List<TableListItem>,
    val isLoading: Boolean,
)

data class TableListItem(
    val table: Table,
    val markedCount: Int,
    val isActive: Boolean,
)
