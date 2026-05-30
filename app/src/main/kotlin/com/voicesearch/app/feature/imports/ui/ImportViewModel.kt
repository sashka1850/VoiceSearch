package com.voicesearch.app.feature.imports.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.app.feature.imports.domain.ImportTableUseCase
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importTable: ImportTableUseCase,
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private val _yandexLinkDraft = MutableStateFlow("")
    val yandexLinkDraft: StateFlow<String> = _yandexLinkDraft.asStateFlow()

    fun onYandexLinkChange(text: String) {
        _yandexLinkDraft.value = text
    }

    fun importLocalFile(uri: Uri) {
        runImport(label = "Читаем файл…", source = ImportTableUseCase.Source.LocalFile(uri))
    }

    fun importYandexLink() {
        val link = _yandexLinkDraft.value.trim()
        if (!link.startsWith("http", ignoreCase = true)) {
            _state.value = ImportUiState.Error("Ссылка должна начинаться с https://")
            return
        }
        runImport(label = "Скачиваем с Яндекс.Диска…", source = ImportTableUseCase.Source.YandexPublicLink(link))
    }

    fun consumeError() {
        if (_state.value is ImportUiState.Error) _state.value = ImportUiState.Idle
    }

    fun consumeSuccess() {
        if (_state.value is ImportUiState.Success) _state.value = ImportUiState.Idle
    }

    private fun runImport(label: String, source: ImportTableUseCase.Source) {
        _state.value = ImportUiState.Loading(label)
        viewModelScope.launch {
            when (val outcome = importTable.import(source)) {
                is ImportTableUseCase.Result.Success -> {
                    appPreferences.setCurrentTableId(outcome.tableId)
                    _state.value = ImportUiState.Success(outcome.tableId)
                }
                is ImportTableUseCase.Result.Failure -> {
                    _state.value = ImportUiState.Error(outcome.message)
                }
            }
        }
    }
}
