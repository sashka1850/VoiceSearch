package com.voicesearch.app.feature.auth.yandex

import com.voicesearch.app.BuildConfig
import com.voicesearch.core.domain.repository.YandexAuthRepository
import com.voicesearch.core.network.yandex.YandexOAuthApi
import javax.inject.Inject

/**
 * Trades an authorization code from the redirect for an access token and
 * stores it in the encrypted [YandexAuthRepository].
 */
class YandexAuthHandler @Inject constructor(
    private val oauthApi: YandexOAuthApi,
    private val authRepository: YandexAuthRepository,
) {

    suspend fun handleAuthCode(code: String): Result {
        val clientId = BuildConfig.YANDEX_CLIENT_ID
        val clientSecret = BuildConfig.YANDEX_CLIENT_SECRET
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Result.Failure("Не настроен YANDEX_CLIENT_ID/SECRET в local.properties")
        }
        return runCatching {
            val token = oauthApi.exchangeCode(
                code = code,
                clientId = clientId,
                clientSecret = clientSecret,
            )
            val expiresAt = token.expiresInSeconds?.let { System.currentTimeMillis() + it * SECONDS }
            authRepository.saveToken(accessToken = token.accessToken, expiresAtEpochMs = expiresAt)
            Result.Success
        }.getOrElse {
            Result.Failure("Не удалось получить токен: ${it.message ?: it::class.simpleName}")
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    private companion object {
        const val SECONDS = 1_000L
    }
}
