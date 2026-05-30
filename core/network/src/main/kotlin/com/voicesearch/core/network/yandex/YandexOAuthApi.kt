package com.voicesearch.core.network.yandex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * OAuth token exchange — separate base URL from the Disk API.
 *
 * Authorization-code flow:
 *  1. App opens Custom Tab at `oauth.yandex.ru/authorize?response_type=code&client_id=...`
 *  2. User grants → Yandex redirects to `yandexta://<clientId>/?code=AUTH_CODE`
 *  3. App posts AUTH_CODE here with the client secret and gets an access token.
 *
 * Implicit flow (`response_type=token`) is also possible and skips this call,
 * but the code flow gives us a longer-lived token and a refresh option, so
 * it's worth the extra round trip.
 */
interface YandexOAuthApi {

    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
    ): YandexTokenResponse
}

@Serializable
data class YandexTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)
