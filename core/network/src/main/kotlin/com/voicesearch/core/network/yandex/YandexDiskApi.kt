package com.voicesearch.core.network.yandex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Yandex.Disk REST API.
 *
 * Two flavours of calls:
 *  - "public" — resolved by a public link, no OAuth required. Used when the
 *    user pastes a link to a shared spreadsheet.
 *  - "personal" — operate on the user's own disk, require `Authorization:
 *    OAuth <token>`. Used by sync/upload from Stage 5.
 *
 * The two-step download dance is mandatory: Yandex never returns the file
 * directly; you ask for a short-lived URL and then GET that URL separately.
 */
interface YandexDiskApi {

    /** Resolve a public link to a one-shot download URL. */
    @GET("v1/disk/public/resources/download")
    suspend fun getPublicDownloadHref(
        @Query("public_key") publicKey: String,
        @Query("path") path: String? = null,
    ): YandexDownloadHref

    /** Resolve a path inside the authenticated user's disk. */
    @GET("v1/disk/resources/download")
    suspend fun getAuthorizedDownloadHref(
        @Header("Authorization") bearer: String,
        @Query("path") path: String,
    ): YandexDownloadHref

    /** Request an upload URL for a path. Overwrites if [overwrite] is true. */
    @GET("v1/disk/resources/upload")
    suspend fun getUploadHref(
        @Header("Authorization") bearer: String,
        @Query("path") path: String,
        @Query("overwrite") overwrite: Boolean = true,
    ): YandexUploadHref

    /**
     * Stream of arbitrary bytes from a previously-resolved Yandex URL.
     * Used both for downloads (the [href] returned above) and as the actual
     * upload target (PUT to the upload href).
     *
     * We don't strongly type the success response — the call site streams
     * the body straight to disk via OkHttp.
     */
    @GET
    suspend fun downloadFile(@Url url: String): okhttp3.ResponseBody

    @PUT
    suspend fun uploadFile(
        @Url url: String,
        @retrofit2.http.Body body: okhttp3.RequestBody,
    ): retrofit2.Response<Unit>
}

@Serializable
data class YandexDownloadHref(
    val href: String,
    val method: String = "GET",
    @SerialName("templated") val templated: Boolean = false,
)

@Serializable
data class YandexUploadHref(
    val href: String,
    val method: String = "PUT",
    @SerialName("templated") val templated: Boolean = false,
)
