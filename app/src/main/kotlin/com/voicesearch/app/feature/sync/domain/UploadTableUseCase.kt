package com.voicesearch.app.feature.sync.domain

import com.voicesearch.app.feature.export.domain.ExportTableUseCase
import com.voicesearch.core.domain.model.TableSource
import com.voicesearch.core.domain.network.FileUploader
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.YandexAuthRepository
import com.voicesearch.core.domain.repository.YandexAuthState
import com.voicesearch.core.network.yandex.YandexDiskApi
import javax.inject.Inject

/**
 * Pushes the current Room state of a table back to Yandex.Disk.
 *
 * Two-step dance per Yandex's API:
 *  1. POST/GET an upload href tied to a target path on the user's disk.
 *  2. PUT the bytes to that href.
 *
 * Upload target = `/VoiceSearch/<table-name>.<ext>`. Yandex auto-creates the
 * folder on first upload via the `app:/` prefix… actually it doesn't, so we
 * just use root-level path. Users can move the file in Yandex UI if they want.
 *
 * Bearer header format is `OAuth <token>` (Yandex-specific, not `Bearer`).
 */
class UploadTableUseCase @Inject constructor(
    private val exportTableUseCase: ExportTableUseCase,
    private val tableRepository: TableRepository,
    private val authRepository: YandexAuthRepository,
    private val diskApi: YandexDiskApi,
    private val fileUploader: FileUploader,
) {

    sealed interface Result {
        data class Success(val targetPath: String) : Result
        data object NotAuthenticated : Result
        data class Failure(val message: String) : Result
    }

    suspend fun upload(tableId: String): Result {
        val state = authRepository.current()
        if (state !is YandexAuthState.Authenticated) return Result.NotAuthenticated

        val table = tableRepository.get(tableId)
            ?: return Result.Failure("Таблица не найдена")

        val export = exportTableUseCase.export(tableId)
        if (export is ExportTableUseCase.Result.Failure) return Result.Failure(export.message)
        val success = export as ExportTableUseCase.Result.Success

        // Prefer the disk path the user originally imported from when it's
        // an authenticated source they own; otherwise drop a fresh copy in
        // a known folder.
        val remotePath = (table.source as? TableSource.YandexDisk)?.remotePath
            ?: "/${success.file.displayName}"

        return runCatching {
            val href = diskApi.getUploadHref(
                bearer = "OAuth ${state.accessToken}",
                path = remotePath,
                overwrite = true,
            )
            fileUploader.upload(
                url = href.href,
                bytes = success.artifact.readBytes(),
                contentType = success.mimeType,
            )
            Result.Success(targetPath = remotePath)
        }.getOrElse { error ->
            Result.Failure(humanise(error))
        }
    }

    /**
     * Map common Yandex / HTTP error codes to actionable Russian messages.
     * Falls back to the raw exception text for everything else.
     */
    private fun humanise(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("HTTP 423") -> "Файл занят: открыт в Яндекс.Документах " +
                "или предыдущая синхронизация ещё не завершилась. Закройте файл в браузере " +
                "и попробуйте через минуту."
            raw.contains("HTTP 401") -> "Токен Я.Диска недействителен. Выйдите и войдите снова."
            raw.contains("HTTP 403") -> "Нет права на запись. Проверьте, что в Яндекс-кабинете " +
                "у приложения есть доступ cloud_api:disk.write."
            raw.contains("HTTP 404") -> "Путь на Я.Диске не найден."
            raw.contains("HTTP 507") -> "На Я.Диске закончилось место."
            raw.contains("HTTP 5") -> "Сервер Я.Диска временно недоступен. WorkManager повторит позже."
            else -> "Загрузка не удалась: ${raw.ifBlank { error::class.simpleName.orEmpty() }}"
        }
    }
}
