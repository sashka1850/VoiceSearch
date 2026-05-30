package com.voicesearch.core.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voicesearch.core.data.datastore.YandexAuthRepositoryImpl.Companion.YANDEX_AUTH_PREFS
import com.voicesearch.core.data.db.VoiceSearchDatabase
import com.voicesearch.core.data.db.dao.TableDao
import com.voicesearch.core.data.db.dao.TableRowDao
import com.voicesearch.core.data.db.dao.TableSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

/**
 * Single source of truth for data-layer wiring.
 *
 * Note: the project-wide [Json] is provided by `:core:network`'s NetworkModule;
 * Hilt finds it transitively through `app` depending on both modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val PREFS_FILE = "voicesearch_prefs"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VoiceSearchDatabase =
        Room.databaseBuilder(context, VoiceSearchDatabase::class.java, VoiceSearchDatabase.NAME)
            // Migrations land here as we bump schema version.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideTableDao(db: VoiceSearchDatabase): TableDao = db.tableDao()

    @Provides fun provideTableRowDao(db: VoiceSearchDatabase): TableRowDao = db.tableRowDao()

    @Provides fun provideTableSettingsDao(db: VoiceSearchDatabase): TableSettingsDao = db.tableSettingsDao()

    @Provides
    @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile(PREFS_FILE) },
        )

    /**
     * Encrypted backing store for the Yandex.Disk OAuth token.
     *
     * Uses Android Keystore as the master-key source; values are AES-256-GCM
     * encrypted on disk. Keys (the prefs entry names) are AES-256-SIV — same
     * key always produces the same ciphertext, so `getString(KEY_TOKEN, …)`
     * still works.
     */
    @Provides
    @Singleton
    @Named(YANDEX_AUTH_PREFS)
    fun provideYandexAuthPrefs(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            YANDEX_AUTH_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // Json is provided by `:core:network`'s NetworkModule — see VoiceSearchApp's Hilt graph.
}
