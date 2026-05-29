package com.voicesearch.app.ui.home

/**
 * State surface for [HomeScreen]. Stage 4 introduces the real listening/result
 * states; for now we only model "no tables yet" vs "table picked".
 */
sealed interface HomeUiState {
    data object Empty : HomeUiState
    data class WithTable(val tableName: String) : HomeUiState
}
