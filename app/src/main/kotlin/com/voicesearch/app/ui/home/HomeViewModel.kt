package com.voicesearch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableSettingsRepository
import com.voicesearch.core.domain.search.SearchMatcher
import com.voicesearch.core.domain.speech.SpeechError
import com.voicesearch.core.domain.speech.SpeechEvent
import com.voicesearch.core.domain.speech.SpeechRecognitionEngine
import com.voicesearch.core.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the home screen: knows which table is active, listens to the speech
 * engine while the user holds the mic, and resolves the spoken text against
 * the table's search column.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
    private val tableRepository: TableRepository,
    private val settingsRepository: TableSettingsRepository,
    private val speechEngine: SpeechRecognitionEngine,
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
    private val lastOutcome = MutableStateFlow<SearchOutcome?>(null)

    val state: StateFlow<HomeUiState> = combine(
        activeTable,
        activeSettings,
        micState,
        lastOutcome,
    ) { table, settings, mic, outcome ->
        if (table == null) {
            HomeUiState.Empty
        } else {
            HomeUiState.Active(
                tableName = table.name,
                tableId = table.id,
                hint = if (settings == null) PromptHint.NotConfigured
                else PromptHint.fromPrefix(settings.prefix),
                mic = mic,
                lastOutcome = outcome,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Empty)

    init {
        speechEngine.events
            .onEach(::handleSpeechEvent)
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
            is SpeechEvent.Final -> runSearch(event.text)
            is SpeechEvent.Error -> {
                micState.value = MicState.Idle
                lastOutcome.value = SearchOutcome.Failed(event.reason.userMessage())
            }
            // We don't surface Ready / BeginningOfSpeech / Partial / EndOfSpeech in state yet —
            // the UI's "Слушаю" copy is driven by micState transitions in pressMic/releaseMic.
            else -> Unit
        }
    }

    private fun runSearch(spokenText: String) {
        val table = activeTable.value
        val settings = activeSettings.value
        if (table == null || settings == null) {
            micState.value = MicState.Idle
            lastOutcome.value = SearchOutcome.NeedsSettings
            return
        }
        viewModelScope.launch {
            val rows = tableRepository.getRows(table.id)
            val matches = SearchMatcher.findMatches(rows, settings, spokenText)
            lastOutcome.value = when (matches.size) {
                0 -> SearchOutcome.NotFound(spokenText)
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
                    spokenText = spokenText,
                )
            }
            micState.value = MicState.Idle
        }
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
