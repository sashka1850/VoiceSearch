package com.voicesearch.app.feature.auth.yandex

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide pipe between [com.voicesearch.app.MainActivity]'s deep-link
 * receiver and any ViewModel that wants to act on a returning OAuth code.
 *
 * The activity has no direct handle on ViewModels (they're created later by
 * Compose), so we route through this Hilt singleton.
 */
@Singleton
class YandexAuthEvents @Inject constructor() {

    private val _codes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val codes: Flow<String> = _codes.asSharedFlow()

    fun publishCode(code: String) {
        _codes.tryEmit(code)
    }
}
