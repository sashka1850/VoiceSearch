package com.voicesearch.app.feature.imports.domain

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.voicesearch.core.data.parser.TableParseException
import com.voicesearch.core.data.parser.TableParserFactory
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.network.FileDownloader
import com.voicesearch.core.domain.repository.TableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * One-call import: take a [Source], read bytes, parse, persist.
 *
 * The use case is the single seam between the import UI (knows about Uris
 * and OAuth links) and the storage layer (knows nothing about either).
 *
 * Network downloads use the project [OkHttpClient] so logging / future
 * interceptors apply uniformly. Yandex two-step download is intentionally
 * split out — see [ImportTableUseCase.resolveYandexHref].
 */
class ImportTableUseCase @Inject constructor(
    private val contentResolver: ContentResolver,
    private val downloader: FileDownloader,
    private val parserFactory: TableParserFactory,
    private val tableRepository: TableRepository,
    private val yandexResolver: YandexPublicLinkResolver,
) {

    sealed interface Source {
        /** Picked via SAF — we read through ContentResolver. */
        data class LocalFile(val uri: Uri) : Source

        /** Pasted public link like `https://disk.yandex.ru/i/abc...`. */
        data class YandexPublicLink(val url: String) : Source
    }

    sealed interface Result {
        data class Success(val tableId: String) : Result
        data class Failure(val message: String, val cause: Throwable? = null) : Result
    }

    suspend fun import(source: Source): Result = withContext(Dispatchers.IO) {
        runCatching {
            val payload = when (source) {
                is Source.LocalFile -> readLocal(source.uri)
                is Source.YandexPublicLink -> downloadYandexPublic(source.url)
            }
            val parser = parserFactory.parserFor(payload.fileName)
                ?: return@runCatching Result.Failure(
                    "Формат ${payload.fileName.substringAfterLast('.', "?")} пока не поддерживается"
                )

            val parsed = payload.bytes.inputStream().use(parser::parse)

            val tableId = UUID.randomUUID().toString()
            val table = Table(
                id = tableId,
                name = payload.displayName,
                source = payload.toDomainSource(),
                headers = parsed.headers,
                rowCount = parsed.rowCount,
            )
            tableRepository.save(table, parsed.rows)
            Result.Success(tableId = tableId)
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                when (error) {
                    is TableParseException -> Result.Failure(error.message.orEmpty(), error)
                    is IOException -> Result.Failure(
                        "Не удалось скачать или открыть файл: ${error.message}",
                        error,
                    )
                    else -> Result.Failure(
                        "Неожиданная ошибка: ${error.message ?: error::class.simpleName}",
                        error,
                    )
                }
            },
        )
    }

    private fun readLocal(uri: Uri): Payload {
        val name = queryDisplayName(uri) ?: "import.xlsx"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Не удалось открыть файл")
        return Payload(
            bytes = bytes,
            fileName = name,
            displayName = name.substringBeforeLast('.'),
            domainSourceFactory = { TableSource.LocalFile(originalFileName = name) },
        )
    }

    private suspend fun downloadYandexPublic(publicUrl: String): Payload {
        val href = yandexResolver.resolveHref(publicUrl)
        val bytes = downloader.download(href.directUrl)
        val name = href.fileName ?: "yandex.xlsx"
        return Payload(
            bytes = bytes,
            fileName = name,
            displayName = name.substringBeforeLast('.'),
            domainSourceFactory = {
                TableSource.YandexDisk(publicUrl = publicUrl, remotePath = href.path)
            },
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    private data class Payload(
        val bytes: ByteArray,
        val fileName: String,
        val displayName: String,
        private val domainSourceFactory: () -> TableSource,
    ) {
        fun toDomainSource(): TableSource = domainSourceFactory()
    }
}
