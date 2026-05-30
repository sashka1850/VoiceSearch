package com.voicesearch.core.network.yandex

import android.net.Uri

/**
 * URL builders for the OAuth flow. Pulled out so :app can call them
 * without depending on Retrofit internals.
 */
object YandexAuthUris {

    const val AUTHORIZE_BASE = "https://oauth.yandex.ru/authorize"

    /**
     * Builds the URL we open in Chrome Custom Tabs to ask the user
     * to authenticate. Yandex will redirect to `redirectUri` carrying
     * either `?code=...` (code flow) or `#access_token=...` (implicit).
     */
    fun authorize(
        clientId: String,
        redirectUri: String,
        scopes: List<String>,
        state: String? = null,
    ): Uri {
        val builder = Uri.parse(AUTHORIZE_BASE).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("force_confirm", "yes")
        if (scopes.isNotEmpty()) {
            builder.appendQueryParameter("scope", scopes.joinToString(" "))
        }
        if (state != null) {
            builder.appendQueryParameter("state", state)
        }
        return builder.build()
    }

    /**
     * Common Yandex.Disk scopes the project needs.
     * Keep in sync with the rights ticked in the Yandex OAuth dashboard.
     */
    val DISK_SCOPES: List<String> = listOf(
        "cloud_api:disk.read",
        "cloud_api:disk.write",
        "cloud_api:disk.info",
    )
}
