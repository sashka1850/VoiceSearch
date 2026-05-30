package com.voicesearch.app.di

import com.voicesearch.app.feature.search.speech.AndroidSpeechRecognitionEngine
import com.voicesearch.core.domain.speech.SpeechRecognitionEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The engine is intentionally NOT scoped — Hilt hands a fresh instance to each
 * ViewModel that asks for it, so a recreated screen doesn't reuse a recognizer
 * that the previous owner may have torn down.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpeechModule {

    @Binds
    abstract fun bindEngine(impl: AndroidSpeechRecognitionEngine): SpeechRecognitionEngine
}
