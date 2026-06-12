package com.voicesearch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.core.data.crash.CrashReporter
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ThemeMode.SYSTEM,
        )

    // Crash logs aren't observable — they're written from a Thread.UncaughtExceptionHandler
    // that runs after this VM has died. So we just snapshot on demand and let
    // the UI refresh after explicit user actions (share / clear).
    private val _crashLogs = MutableStateFlow<List<File>>(emptyList())
    val crashLogs: StateFlow<List<File>> = _crashLogs.asStateFlow()

    init {
        refreshCrashLogs()
    }

    fun refreshCrashLogs() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { crashReporter.recentCrashes() }
            _crashLogs.value = files
        }
    }

    fun clearCrashLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { crashReporter.clearAll() }
            _crashLogs.value = emptyList()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
