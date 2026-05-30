package com.voicesearch.app.feature.search.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicesearch.core.domain.speech.SpeechError
import com.voicesearch.core.domain.speech.SpeechEvent
import com.voicesearch.core.domain.speech.SpeechRecognitionEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import javax.inject.Inject

/**
 * Wraps the platform [SpeechRecognizer] behind the [SpeechRecognitionEngine]
 * port. All recognizer interaction happens on the main thread (SpeechRecognizer
 * requires it) — we post via a [Handler] when called from a coroutine.
 *
 * Hilt provides a fresh instance per ViewModel (no scoping). The owner calls
 * [release] from `onCleared` so we don't leak the recognizer.
 */
class AndroidSpeechRecognitionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechRecognitionEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    private val _events = MutableSharedFlow<SpeechEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<SpeechEvent> = _events.asSharedFlow()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(languageTag: String) {
        runOnMain {
            ensureRecognizer()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, parseLocale(languageTag))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer?.startListening(intent)
        }
    }

    override fun stop() {
        runOnMain { recognizer?.stopListening() }
    }

    override fun release() {
        runOnMain {
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        r.setRecognitionListener(listener)
        recognizer = r
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun parseLocale(tag: String): Locale = Locale.forLanguageTag(tag).takeIf {
        it.language.isNotEmpty()
    } ?: Locale("ru", "RU")

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _events.tryEmit(SpeechEvent.Ready)
        }

        override fun onBeginningOfSpeech() {
            _events.tryEmit(SpeechEvent.BeginningOfSpeech)
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _events.tryEmit(SpeechEvent.EndOfSpeech)
        }

        override fun onError(error: Int) {
            _events.tryEmit(SpeechEvent.Error(error.toDomainError()))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            _events.tryEmit(SpeechEvent.Final(text))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotEmpty()) {
                _events.tryEmit(SpeechEvent.Partial(text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Int.toDomainError(): SpeechError = when (this) {
        SpeechRecognizer.ERROR_AUDIO -> SpeechError.Audio
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError.Network
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NoMatch
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.Timeout
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.InsufficientPermissions
        else -> SpeechError.Unknown
    }
}
