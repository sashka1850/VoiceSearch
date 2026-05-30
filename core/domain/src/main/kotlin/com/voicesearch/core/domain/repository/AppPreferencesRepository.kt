package com.voicesearch.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Non-sensitive app-level preferences: theme choice, currently-open table, etc.
 * Backed by DataStore (no encryption).
 */
interface AppPreferencesRepository {

    val currentTableId: Flow<String?>
    suspend fun setCurrentTableId(id: String?)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
