package com.voicesearch.app.ui.home

import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.domain.model.TableInfo
import com.voicesearch.core.domain.repository.TableRow

sealed interface HomeUiState {
    data object Empty : HomeUiState

    data class Active(
        val tableName: String,
        val tableId: String,
        val hint: PromptHint,
        val mic: MicState,
        val manualInput: ManualInputState,
        val lastOutcome: SearchOutcome?,
        val yandexAuthenticated: Boolean = false,
        /**
         * Live transcript from the speech engine while the user is holding the
         * mic. Already normalised (letters + digits only) so the slot row can
         * paint it character-by-character.
         */
        val voiceTranscript: String = "",
        /**
         * When true, the Yandex OOB code-entry dialog is shown. Set after the
         * user taps "Войти в Я.Диск" (we launch the browser and prompt for the
         * code page Yandex displays at the end of the consent flow).
         */
        val awaitingYandexCode: Boolean = false,
        val yandexCodeSubmitting: Boolean = false,
        /** Live row counts + sync status; null while the first emission lands. */
        val info: TableInfo? = null,
        /** Current TableSettings.autoSync flag — drives the top-right toggle. */
        val autoSyncEnabled: Boolean = false,
    ) : HomeUiState
}

/**
 * Keyboard-fallback input. Hidden by default; users summon it from the
 * second FAB when voice isn't an option (noisy, offline, etc.).
 */
data class ManualInputState(
    val isVisible: Boolean = false,
    val text: String = "",
    val isSearching: Boolean = false,
)

enum class MicState { Idle, Listening, Processing }

sealed interface PromptHint {
    /** Carry the actual prefix string so the UI can paint it next to the slot row. */
    data class FixedSuffix(val prefix: String, val length: Int) : PromptHint
    data class VariableSuffix(val prefix: String, val maxLength: Int) : PromptHint
    data object FullValue : PromptHint
    data object NotConfigured : PromptHint

    fun toDisplayString(): String = when (this) {
        is FixedSuffix -> "Назовите последние $length"
        is VariableSuffix -> "Назовите последние до $maxLength"
        FullValue -> "Назовите значение"
        NotConfigured -> "Откройте меню → «Настройки таблицы»"
    }

    companion object {
        fun fromPrefix(hint: PrefixHint?): PromptHint = when (hint) {
            is PrefixHint.FixedSuffix -> FixedSuffix(hint.prefix, hint.suffixLength)
            is PrefixHint.VariableSuffix -> VariableSuffix(hint.prefix, hint.maxSuffixLength)
            PrefixHint.FullMatch -> FullValue
            null -> FullValue
        }
    }
}

/**
 * One-shot outcome surfaced briefly after a search. The UI consumes it after
 * showing a toast / sheet so the next mic press starts from a clean slate.
 */
sealed interface SearchOutcome {
    /** Auto-marked a single row. */
    data class Marked(val cellValue: String, val name: String?) : SearchOutcome

    data class NotFound(val spokenText: String) : SearchOutcome

    /** Multiple rows matched — user picks which to mark via [HomeViewModel.applyMultipleSelection]. */
    data class MultipleCandidates(
        val candidates: List<Candidate>,
        val spokenText: String,
    ) : SearchOutcome

    data class Failed(val message: String) : SearchOutcome

    /** Settings are missing — point the user at the config screen. */
    data object NeedsSettings : SearchOutcome
}

data class Candidate(
    val rowId: Long,
    val cellValue: String,
    val name: String?,
    val isAlreadyMarked: Boolean,
)

/** Convenience to convert from domain row + settings. */
internal fun TableRow.toCandidate(searchCol: Int, nameCol: Int): Candidate = Candidate(
    rowId = id,
    cellValue = cells.getOrNull(searchCol).orEmpty(),
    name = cells.getOrNull(nameCol),
    isAlreadyMarked = isMarked,
)
