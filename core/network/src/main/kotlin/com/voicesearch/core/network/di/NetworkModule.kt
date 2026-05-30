package com.voicesearch.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.voicesearch.core.domain.network.FileDownloader
import com.voicesearch.core.domain.network.FileUploader
import com.voicesearch.core.network.http.OkHttpFileDownloader
import com.voicesearch.core.network.http.OkHttpFileUploader
import com.voicesearch.core.network.yandex.YandexDiskApi
import com.voicesearch.core.network.yandex.YandexOAuthApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val YANDEX_DISK_BASE = "https://cloud-api.yandex.net/"
    private const val YANDEX_OAUTH_BASE = "https://oauth.yandex.ru/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // file downloads can be slow
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named(YANDEX_DISK_RETROFIT)
    fun provideDiskRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(YANDEX_DISK_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named(YANDEX_OAUTH_RETROFIT)
    fun provideOAuthRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(YANDEX_OAUTH_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideYandexDiskApi(@Named(YANDEX_DISK_RETROFIT) retrofit: Retrofit): YandexDiskApi =
        retrofit.create(YandexDiskApi::class.java)

    @Provides
    @Singleton
    fun provideYandexOAuthApi(@Named(YANDEX_OAUTH_RETROFIT) retrofit: Retrofit): YandexOAuthApi =
        retrofit.create(YandexOAuthApi::class.java)

    private const val YANDEX_DISK_RETROFIT = "yandex_disk_retrofit"
    private const val YANDEX_OAUTH_RETROFIT = "yandex_oauth_retrofit"
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {

    @Binds
    @Singleton
    abstract fun bindFileDownloader(impl: OkHttpFileDownloader): FileDownloader

    @Binds
    @Singleton
    abstract fun bindFileUploader(impl: OkHttpFileUploader): FileUploader
}
