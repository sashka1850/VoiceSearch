package com.voicesearch.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicesearch.core.domain.repository.AppPreferencesRepository
import com.voicesearch.core.domain.repository.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppPreferencesRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : AppPreferencesRepository {

    private object Keys {
        val CURRENT_TABLE_ID = stringPreferencesKey("current_table_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    override val currentTableId: Flow<String?> =
        store.data.map { it[Keys.CURRENT_TABLE_ID] }

    override suspend fun setCurrentTableId(id: String?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(Keys.CURRENT_TABLE_ID) else prefs[Keys.CURRENT_TABLE_ID] = id
        }
    }

    override val themeMode: Flow<ThemeMode> =
        store.data.map { prefs ->
            prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override val hasSeenOnboarding: Flow<Boolean> =
        store.data.map { it[Keys.HAS_SEEN_ONBOARDING] ?: false }

    override suspend fun setHasSeenOnboarding(seen: Boolean) {
        store.edit { it[Keys.HAS_SEEN_ONBOARDING] = seen }
    }
}
