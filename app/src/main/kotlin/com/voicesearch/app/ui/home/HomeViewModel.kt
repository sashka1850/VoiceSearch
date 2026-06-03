package com.voicesearch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.app.feature.auth.yandex.YandexAuthEvents
import com.voicesearch.app.feature.auth.yandex.YandexAuthHandler
import com.voicesearch.app.feature.auth.yandex.YandexAuthLauncher
import com.voicesearch.app.feature.export.domain.ExportTableUseCase
import com.voicesearch.app.feature.export.domain.ShareableFile
import com.voicesearch.app.feature.sync.domain.UploadTableUseCase
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableSettingsRepository
import com.voicesearch.core.domain.repository.YandexAuthRepository
import com.voicesearch.core.domain.repository.YandexAuthState
import com.voicesearch.core.domain.search.SearchMatcher
import com.voicesearch.core.domain.search.SearchMatcher.normaliseQuery
import com.voicesearch.core.domain.speech.SpeechError
import com.voicesearch.core.domain.speech.SpeechEvent
import com.voicesearch.core.domain.speech.SpeechRecognitionEngine
import com.voicesearch.core.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the home screen: knows which table is active, listens to the speech
 * engine while the user holds the mic, and resolves the spoken text against
 * the table's search column.
 */
@Suppress("LongParameterList") // Wiring-time ViewModel; each dep is a real collaborator.
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
    private val tableRepository: TableRepository,
    private val settingsRepository: TableSettingsRepository,
    private val speechEngine: SpeechRecognitionEngine,
    private val exportTableUseCase: ExportTableUseCase,
    private val uploadTableUseCase: UploadTableUseCase,
    private val yandexAuthRepository: YandexAuthRepository,
    private val yandexAuthEvents: YandexAuthEvents,
    private val yandexAuthHandler: YandexAuthHandler,
    private val yandexAuthLauncher: YandexAuthLauncher,
    private val clock: Clock,
) : ViewModel() {

    private val activeTable: StateFlow<Table?> = combine(
        appPreferences.currentTableId,
        tableRepository.observeAll(),
    ) { currentId, all ->
        all.firstOrNull { it.id == currentId } ?: all.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    private val activeSettings: StateFlow<TableSettings?> = activeTable
        .flatMapLatest { table ->
            if (table == null) flowOf(null) else settingsRepository.observe(table.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    private val micState = MutableStateFlow(MicState.Idle)
    private val manualInputState = MutableStateFlow(ManualInputState())
    private val lastOutcome = MutableStateFlow<SearchOutcome?>(null)

    /** Live transcript stripped to letters+digits — drives the slot row UI. */
    private val voiceTranscript = MutableStateFlow("")

    /**
     * One-shot side-effect channel for the share intent. UI collects via [shareEvents].
     * Buffered so a rapid "press → press" doesn't drop a request while the previous
     * chooser is still opening.
     */
    private val _shareEvents = Channel<ShareableFile>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val shareEvents: Flow<ShareableFile> = _shareEvents.receiveAsFlow()

    private val yandexAuth: StateFlow<YandexAuthState> = yandexAuthRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), YandexAuthState.NotAuthenticated)

    val state: StateFlow<HomeUiState> = combine(
        listOf(activeTable, activeSettings, micState, manualInputState, lastOutcome, yandexAuth, voiceTranscript),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val table = values[0] as Table?
        @Suppress("UNCHECKED_CAST")
        val settings = values[1] as TableSettings?
        val mic = values[2] as MicState
        val manual = values[3] as ManualInputState
        @Suppress("UNCHECKED_CAST")
        val outcome = values[4] as SearchOutcome?
        val auth = values[5] as YandexAuthState
        val transcript = values[6] as String

        if (table == null) {
            HomeUiState.Empty
        } else {
            HomeUiState.Active(
                tableName = table.name,
                tableId = table.id,
                hint = if (settings == null) PromptHint.NotConfigured
                else PromptHint.fromPrefix(settings.prefix),
                mic = mic,
                manualInput = manual,
                lastOutcome = outcome,
                yandexAuthenticated = auth is YandexAuthState.Authenticated,
                voiceTranscript = transcript,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Empty)

    init {
        speechEngine.events
            .onEach(::handleSpeechEvent)
            .launchIn(viewModelScope)

        // Deep links from the OAuth redirect land here through MainActivity.
        yandexAuthEvents.codes
            .onEach { code ->
                when (val result = yandexAuthHandler.handleAuthCode(code)) {
                    is YandexAuthHandler.Result.Success -> {
                        lastOutcome.value = SearchOutcome.Failed("Вход в Яндекс выполнен")
                    }
                    is YandexAuthHandler.Result.Failure -> {
                        lastOutcome.value = SearchOutcome.Failed(result.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun pressMic() {
        if (activeSettings.value == null) {
            lastOutcome.value = SearchOutcome.NeedsSettings
            return
        }
        if (!speechEngine.isAvailable) {
            lastOutcome.value = SearchOutcome.Failed("Голосовое распознавание недоступно на устройстве")
            return
        }
        lastOutcome.value = null
        voiceTranscript.value = ""
        micState.value = MicState.Listening
        speechEngine.start()
    }

    fun releaseMic() {
        if (micState.value == MicState.Listening) {
            speechEngine.stop()
            micState.value = MicState.Processing
        }
    }

    fun consumeOutcome() {
        lastOutcome.value = null
    }

    // region Manual (keyboard) input — same search pipeline, different trigger.

    fun toggleManualInput() {
        manualInputState.update { it.copy(isVisible = !it.isVisible) }
    }

    fun onManualInputChange(text: String) {
        manualInputState.update { it.copy(text = text) }
    }

    fun submitManualSearch() {
        val draft = manualInputState.value.text.trim()
        if (draft.isEmpty()) return
        // Mic must be idle — otherwise we'd cross the streams (literally).
        if (micState.value != MicState.Idle) return
        manualInputState.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            runSearch(draft)
            manualInputState.update { it.copy(isSearching = false, text = "") }
        }
    }

    fun dismissManualInput() {
        manualInputState.value = ManualInputState()
    }

    // endregion

    // region Export / share

    fun shareCurrentTable() {
        val tableId = activeTable.value?.id ?: run {
            lastOutcome.value = SearchOutcome.Failed("Сначала выберите таблицу")
            return
        }
        viewModelScope.launch {
            when (val outcome = exportTableUseCase.export(tableId)) {
                is ExportTableUseCase.Result.Success -> _shareEvents.trySend(outcome.file)
                is ExportTableUseCase.Result.Failure -> {
                    lastOutcome.value = SearchOutcome.Failed(outcome.message)
                }
            }
        }
    }

    // endregion

    // region Yandex auth + sync

    fun connectYandex() {
        when (val result = yandexAuthLauncher.launch()) {
            YandexAuthLauncher.LaunchResult.Launched -> Unit // wait for redirect
            YandexAuthLauncher.LaunchResult.MissingConfig -> {
                lastOutcome.value = SearchOutcome.Failed(
                    "ClientID не настроен. Добавьте YANDEX_CLIENT_ID в local.properties.",
                )
            }
            is YandexAuthLauncher.LaunchResult.NoBrowser -> {
                lastOutcome.value = SearchOutcome.Failed(result.message)
            }
        }
    }

    fun signOutYandex() {
        viewModelScope.launch {
            yandexAuthRepository.clear()
            lastOutcome.value = SearchOutcome.Failed("Вышли из Яндекса")
        }
    }

    fun syncNow() {
        val tableId = activeTable.value?.id ?: run {
            lastOutcome.value = SearchOutcome.Failed("Сначала выберите таблицу")
            return
        }
        viewModelScope.launch {
            when (val result = uploadTableUseCase.upload(tableId)) {
                is UploadTableUseCase.Result.Success -> {
                    lastOutcome.value = SearchOutcome.Failed("Загружено: ${result.targetPath}")
                }
                UploadTableUseCase.Result.NotAuthenticated -> {
                    lastOutcome.value = SearchOutcome.Failed("Сначала войдите в Я.Диск")
                }
                is UploadTableUseCase.Result.Failure -> {
                    lastOutcome.value = SearchOutcome.Failed(result.message)
                }
            }
        }
    }

    // endregion

    /** User confirmed a subset of rows from the multi-match sheet. */
    fun applyMultipleSelection(rowIds: Collection<Long>) {
        if (rowIds.isEmpty()) {
            lastOutcome.value = null
            return
        }
        viewModelScope.launch {
            tableRepository.setRowsMarked(rowIds, marked = true, markedAt = clock.nowMillis())
            lastOutcome.value = null
        }
    }

    private fun handleSpeechEvent(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Ready, SpeechEvent.BeginningOfSpeech -> voiceTranscript.value = ""
            is SpeechEvent.Partial -> voiceTranscript.value = normaliseQuery(event.text)
            is SpeechEvent.Final -> {
                voiceTranscript.value = normaliseQuery(event.text)
                viewModelScope.launch { runSearch(event.text) }
            }
            is SpeechEvent.Error -> {
                voiceTranscript.value = ""
                micState.value = MicState.Idle
                lastOutcome.value = SearchOutcome.Failed(event.reason.userMessage())
            }
            SpeechEvent.EndOfSpeech -> Unit
        }
    }

    /** Shared by both voice (final transcript) and keyboard (typed query). */
    private suspend fun runSearch(queryText: String) {
        val table = activeTable.value
        val settings = activeSettings.value
        if (table == null || settings == null) {
            micState.value = MicState.Idle
            lastOutcome.value = SearchOutcome.NeedsSettings
            return
        }
        val rows = tableRepository.getRows(table.id)
        val matches = SearchMatcher.findMatches(rows, settings, queryText)
        lastOutcome.value = when (matches.size) {
            0 -> SearchOutcome.NotFound(queryText)
            1 -> {
                val match = matches.first()
                tableRepository.setRowMarked(match.id, marked = true, markedAt = clock.nowMillis())
                SearchOutcome.Marked(
                    cellValue = match.cells.getOrNull(settings.searchColumnIndex).orEmpty(),
                    name = match.cells.getOrNull(settings.nameColumnIndex),
                )
            }
            else -> SearchOutcome.MultipleCandidates(
                candidates = matches.map { it.toCandidate(settings.searchColumnIndex, settings.nameColumnIndex) },
                spokenText = queryText,
            )
        }
        micState.value = MicState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun SpeechError.userMessage(): String = when (this) {
    SpeechError.Audio -> "Ошибка микрофона"
    SpeechError.Network -> "Нет интернета — голосовое распознавание недоступно"
    SpeechError.NoMatch -> "Не разобрал. Попробуйте ещё раз"
    SpeechError.Timeout -> "Слишком долго — попробуйте быстрее"
    SpeechError.InsufficientPermissions -> "Нет разрешения на микрофон"
    SpeechError.Unknown -> "Не получилось распознать речь"
}
