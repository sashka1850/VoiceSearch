package com.voicesearch.core.data.datastore

import android.content.SharedPreferences
import com.voicesearch.core.domain.repository.YandexAuthRepository
import com.voicesearch.core.domain.repository.YandexAuthState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * OAuth token storage for Yandex.Disk.
 *
 * Backed by Jetpack Security's [androidx.security.crypto.EncryptedSharedPreferences]
 * — values are AES-256-GCM encrypted at rest with a key wrapped by Android Keystore.
 *
 * Exposed as a Flow via [SharedPreferences.OnSharedPreferenceChangeListener]
 * so the UI reacts to logout / token refresh without polling.
 */
@Singleton
internal class YandexAuthRepositoryImpl @Inject constructor(
    @Named(YANDEX_AUTH_PREFS) private val prefs: SharedPreferences,
) : YandexAuthRepository {

    override val authState: Flow<YandexAuthState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_TOKEN || key == KEY_EXPIRES) {
                trySend(readState())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .onStart { emit(readState()) }
        .flowOn(Dispatchers.IO)

    override suspend fun current(): YandexAuthState = readState()

    override suspend fun saveToken(accessToken: String, expiresAtEpochMs: Long?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, accessToken)
            if (expiresAtEpochMs != null) putLong(KEY_EXPIRES, expiresAtEpochMs) else remove(KEY_EXPIRES)
            apply()
        }
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }

    private fun readState(): YandexAuthState {
        val token = prefs.getString(KEY_TOKEN, null) ?: return YandexAuthState.NotAuthenticated
        val expires = if (prefs.contains(KEY_EXPIRES)) prefs.getLong(KEY_EXPIRES, 0L) else null
        return YandexAuthState.Authenticated(accessToken = token, expiresAtEpochMs = expires)
    }

    companion object {
        const val YANDEX_AUTH_PREFS = "yandex_auth_prefs"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES = "expires_at_ms"
    }
}
