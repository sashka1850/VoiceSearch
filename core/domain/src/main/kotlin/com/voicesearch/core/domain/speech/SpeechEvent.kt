package com.voicesearch.core.domain.speech

/**
 * Events emitted by a speech recognition session.
 *
 * Concrete engines (Android's `SpeechRecognizer`, or a future offline Vosk-based
 * implementation) only need to map their callbacks into this set.
 */
sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data object BeginningOfSpeech : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Error(val reason: SpeechError) : SpeechEvent
    data object EndOfSpeech : SpeechEvent
}

enum class SpeechError {
    Audio,
    Network,
    NoMatch,
    Timeout,
    InsufficientPermissions,
    Unknown,
}
