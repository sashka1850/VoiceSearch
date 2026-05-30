package com.voicesearch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Watches the active table (chosen at import time, persisted in DataStore) and
 * surfaces it as a [HomeUiState]. Re-emits whenever the user imports / picks
 * a different table or whenever the underlying Room row changes.
 *
 * If the persisted id no longer maps to a stored table (e.g. the user deleted
 * it), we fall through the picker logic: pick the most-recent table if any,
 * otherwise Empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    appPreferences: AppPreferencesRepository,
    tableRepository: TableRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        appPreferences.currentTableId,
        tableRepository.observeAll(),
    ) { currentId, all ->
        val active = all.firstOrNull { it.id == currentId } ?: all.firstOrNull()
        active
    }
        .flatMapLatest { active ->
            if (active == null) flowOf(HomeUiState.Empty)
            else flowOf(HomeUiState.WithTable(tableName = active.name))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = HomeUiState.Empty,
        )
}
