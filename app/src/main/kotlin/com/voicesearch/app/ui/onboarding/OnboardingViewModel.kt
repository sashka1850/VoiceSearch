package com.voicesearch.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    /**
     * Source of truth for "should we show onboarding first".
     * Null while the first emission is pending — the NavHost waits.
     */
    val hasSeenOnboarding: StateFlow<Boolean?> = appPreferences.hasSeenOnboarding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    fun markSeen() {
        viewModelScope.launch { appPreferences.setHasSeenOnboarding(true) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
