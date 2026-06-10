package com.voicesearch.app.feature.auth.yandex

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.voicesearch.app.BuildConfig
import com.voicesearch.core.network.yandex.YandexAuthUris
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Opens the Yandex consent page in Chrome Custom Tabs (or falls back to the
 * default browser if Custom Tabs aren't available). Yandex redirects back to
 * us via the `yandexta://<clientId>/?code=...` URI declared in the manifest.
 */
class YandexAuthLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun launch(): LaunchResult {
        val clientId = BuildConfig.YANDEX_CLIENT_ID
        val redirect = BuildConfig.YANDEX_REDIRECT_URI
        if (clientId.isBlank()) return LaunchResult.MissingConfig

        val uri: Uri = YandexAuthUris.authorize(
            clientId = clientId,
            redirectUri = redirect,
            scopes = YandexAuthUris.DISK_SCOPES,
        )

        // Debug aid: paste this exact redirect_uri value into the Yandex OAuth
        // cabinet's "Redirect URI" field. Any mismatch (case, trailing slash,
        // whitespace) returns a 400.
        Timber.i("Yandex OAuth → opening: %s", uri)
        Timber.i("Yandex OAuth → redirect_uri sent: %s", redirect)

        return try {
            CustomTabsIntent.Builder().build().also { it.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                .launchUrl(context, uri)
            LaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            // Some Android variants ship without a browser at all.
            LaunchResult.NoBrowser(e.message ?: "Не нашли подходящий браузер")
        }
    }

    sealed interface LaunchResult {
        data object Launched : LaunchResult
        data object MissingConfig : LaunchResult
        data class NoBrowser(val message: String) : LaunchResult
    }
}
