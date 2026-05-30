package com.voicesearch.app.feature.imports.domain

import android.net.Uri
import com.voicesearch.core.network.yandex.YandexDiskApi
import javax.inject.Inject

/**
 * Turns a user-pasted public link (`https://disk.yandex.ru/i/...`,
 * `https://yadi.sk/i/...`, etc.) into a direct download URL.
 *
 * Yandex's API takes the *full* original link as a `public_key` query
 * parameter — it figures out which item to serve. The response includes
 * a one-shot signed URL and, if we ask for it, useful metadata like
 * filename.
 */
class YandexPublicLinkResolver @Inject constructor(
    private val diskApi: YandexDiskApi,
) {
    data class Resolution(
        val directUrl: String,
        val fileName: String?,
        val path: String?,
    )

    suspend fun resolveHref(publicLink: String): Resolution {
        val normalised = normalise(publicLink)
        val href = diskApi.getPublicDownloadHref(publicKey = normalised)
        val fileName = extractFileName(href.href)
        return Resolution(
            directUrl = href.href,
            fileName = fileName,
            path = null,
        )
    }

    private fun normalise(link: String): String {
        val trimmed = link.trim()
        return trimmed
    }

    /**
     * Yandex puts the original filename in the `filename` query of the
     * signed URL — pull it out as a hint so we don't show `download` to
     * the user.
     */
    private fun extractFileName(directUrl: String): String? {
        return runCatching { Uri.parse(directUrl).getQueryParameter("filename") }.getOrNull()
    }
}
