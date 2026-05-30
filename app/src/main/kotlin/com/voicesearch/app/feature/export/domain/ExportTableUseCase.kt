package com.voicesearch.app.feature.export.domain

import android.content.Context
import androidx.core.content.FileProvider
import com.voicesearch.core.data.writer.ExportFormat
import com.voicesearch.core.data.writer.TableWriterFactory
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Bakes a fresh spreadsheet file from current Room rows and returns a
 * FileProvider-backed [ShareableFile] callers can hand to ACTION_SEND or
 * to the Yandex upload pipeline in Stage 5b.
 *
 * Marks live only in Room (each row's `isMarked`); export applies them
 * to the configured `markColumnIndex` so the user sees them in Excel
 * exactly as they'd see them in our UI.
 */
class ExportTableUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tableRepository: TableRepository,
    private val settingsRepository: TableSettingsRepository,
    private val writerFactory: TableWriterFactory,
) {

    sealed interface Result {
        data class Success(val file: ShareableFile) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun export(tableId: String): Result = withContext(Dispatchers.IO) {
        val table = tableRepository.get(tableId)
            ?: return@withContext Result.Failure("Таблица не найдена")
        val settings = settingsRepository.get(tableId)
            ?: return@withContext Result.Failure("Сначала настройте таблицу")

        val rows = tableRepository.getRows(tableId)
        val materialised = applyMarks(
            rows = rows.map { it.cells },
            isMarkedByRow = rows.map { it.isMarked },
            markColumnIndex = settings.markColumnIndex,
            successMarker = settings.successMarker,
            columnCount = table.headers.size,
        )

        val format = pickFormat(table.source)
        val baseName = (table.name.ifBlank { "table" }).safeFileName()
        val outFile = File(exportDir(), "$baseName.${format.extension}")
        if (outFile.exists()) outFile.delete()

        runCatching {
            outFile.outputStream().use { stream ->
                writerFactory.writerFor(format).write(
                    headers = table.headers,
                    rows = materialised,
                    output = stream,
                )
            }
        }.onFailure { error ->
            return@withContext Result.Failure(
                "Не удалось записать файл: ${error.message ?: error::class.simpleName}",
            )
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, outFile)
        Result.Success(
            ShareableFile(
                uri = uri,
                mimeType = format.mimeType,
                displayName = outFile.name,
            ),
        )
    }

    /**
     * For each marked row, place [successMarker] at [markColumnIndex] (padding
     * the row with empty cells first if it's shorter than the column index).
     */
    private fun applyMarks(
        rows: List<List<String>>,
        isMarkedByRow: List<Boolean>,
        markColumnIndex: Int,
        successMarker: String,
        columnCount: Int,
    ): List<List<String>> {
        require(rows.size == isMarkedByRow.size) { "rows and marks must align" }
        return rows.mapIndexed { idx, original ->
            val padded = if (original.size >= columnCount) {
                original
            } else {
                original + List(columnCount - original.size) { "" }
            }
            if (!isMarkedByRow[idx]) return@mapIndexed padded

            padded.mapIndexed { col, value ->
                if (col == markColumnIndex) successMarker else value
            }
        }
    }

    private fun pickFormat(source: TableSource): ExportFormat = when (source) {
        is TableSource.LocalFile -> writerFactory.formatFromFileName(source.originalFileName)
        is TableSource.YandexDisk -> ExportFormat.XLSX
    }

    private fun exportDir(): File {
        val dir = File(context.cacheDir, EXPORT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun String.safeFileName(): String =
        replace(Regex("""[\\/:*?"<>|]"""), "_").take(MAX_NAME_LEN)

    private companion object {
        const val EXPORT_DIR = "exports"
        const val MAX_NAME_LEN = 80
    }
}
