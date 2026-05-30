package com.voicesearch.core.domain.network

/**
 * Network surface the import flow depends on.
 * Keeps `:app` and `:core:data` free of any direct OkHttp / Retrofit imports.
 */
interface FileDownloader {
    /**
     * Reads the full body at [url] into memory.
     * Throws on non-2xx responses or I/O failures.
     */
    suspend fun download(url: String): ByteArray
}
