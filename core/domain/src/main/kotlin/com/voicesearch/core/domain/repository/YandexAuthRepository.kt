package com.voicesearch.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Secrets store for the Yandex.Disk OAuth flow.
 *
 * Tokens here are encrypted at rest (AES-256-GCM via Jetpack Security).
 * The actual OAuth dance lands in Stage 2; this interface is the contract
 * that flow will write into.
 */
interface YandexAuthRepository {

    val authState: Flow<YandexAuthState>

    suspend fun current(): YandexAuthState

    suspend fun saveToken(accessToken: String, expiresAtEpochMs: Long?)

    suspend fun clear()
}

sealed interface YandexAuthState {
    data object NotAuthenticated : YandexAuthState
    data class Authenticated(
        val accessToken: String,
        val expiresAtEpochMs: Long?,
    ) : YandexAuthState
}
