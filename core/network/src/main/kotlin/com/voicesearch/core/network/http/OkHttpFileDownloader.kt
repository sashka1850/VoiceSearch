package com.voicesearch.core.network.http

import com.voicesearch.core.domain.network.FileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OkHttpFileDownloader @Inject constructor(
    private val client: OkHttpClient,
) : FileDownloader {

    override suspend fun download(url: String): ByteArray = withContext(Dispatchers.IO) {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.bytes() ?: throw IOException("Пустой ответ от сервера")
        }
    }
}
