package com.voicesearch.core.network.http

import com.voicesearch.core.domain.network.FileUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OkHttpFileUploader @Inject constructor(
    private val client: OkHttpClient,
) : FileUploader {

    override suspend fun upload(url: String, bytes: ByteArray, contentType: String) =
        withContext(Dispatchers.IO) {
            val media = contentType.toMediaTypeOrNull()
                ?: throw IOException("Unsupported content type: $contentType")
            val request = Request.Builder()
                .url(url)
                .put(bytes.toRequestBody(media))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Upload failed: HTTP ${response.code}")
                }
            }
        }
}
