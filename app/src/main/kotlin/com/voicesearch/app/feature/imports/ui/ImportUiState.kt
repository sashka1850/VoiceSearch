package com.voicesearch.app.feature.imports.ui

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Loading(val label: String) : ImportUiState
    data class Success(val tableId: String) : ImportUiState
    data class Error(val message: String) : ImportUiState
}
