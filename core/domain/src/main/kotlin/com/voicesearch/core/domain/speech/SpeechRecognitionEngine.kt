package com.voicesearch.core.domain.speech

import kotlinx.coroutines.flow.Flow

/**
 * Voice-input port.
 *
 * Caller flow:
 *  1. Check [isAvailable] before showing the mic UI at all.
 *  2. Collect [events] in your ViewModel scope.
 *  3. Call [start] when the user presses the mic; [stop] when they release.
 *     The engine emits a [SpeechEvent.Final] once recognition finishes.
 *  4. Call [release] from `onCleared` so the underlying recognizer doesn't leak.
 */
interface SpeechRecognitionEngine {
    val events: Flow<SpeechEvent>
    val isAvailable: Boolean
    fun start(languageTag: String = "ru-RU")
    fun stop()
    fun release()
}
