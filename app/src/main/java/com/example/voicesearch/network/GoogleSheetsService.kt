package com.example.voicesearch.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleSheetsApi {
    @GET("/spreadsheets/d/{spreadsheetId}/gviz/tq")
    suspend fun getSheetData(
        @Path("spreadsheetId") spreadsheetId: String,
        @Query("sheet") sheetName: String,
        @Query("tqx") tqx: String = "out:json"
    ): String
}

class GoogleSheetsService {

    private val api: GoogleSheetsApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://docs.google.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GoogleSheetsApi::class.java)
    }

    suspend fun getSheetData(spreadsheetId: String, sheetName: String): List<List<Any?>> {
        return withContext(Dispatchers.IO) {
            try {
                val rawResponse = api.getSheetData(spreadsheetId, sheetName)
                // Убираем префикс google.visualization.Query.setResponse( и суффикс )
                val jsonString = rawResponse
                    .substringAfter("Query.setResponse(")
                    .dropLast(1)

                val response = Gson().fromJson(jsonString, SheetResponse::class.java)
                return@withContext response.table.rows.mapNotNull { row ->
                    row.c?.map { cell ->
                        cell?.v
                    }
                }
            } catch (e: Exception) {
                throw Exception("Ошибка получения данных: ${e.message}")
            }
        }
    }

    suspend fun updateCell(spreadsheetId: String, cell: String, value: String) {
        withContext(Dispatchers.IO) {
            // Для обновления используем Google Sheets API v4
            // Этот пример требует настройки авторизации
            // Упрощенная версия через URL: https://docs.google.com/spreadsheets/d/{spreadsheetId}/edit
            // В реальном проекте используйте Google Sheets API с OAuth
        }
    }

    // Классы для парсинга JSON-ответа от Google Sheets
    private data class SheetResponse(
        val table: Table
    )

    private data class Table(
        val rows: List<Row>,
        val cols: List<Column>
    )

    private data class Row(
        val c: List<Cell?>?
    ) {
        fun getValues(): List<Any?> = c?.mapNotNull { it?.v } ?: emptyList()
    }

    private data class Cell(
        val v: Any?
    )

    private data class Column(
        val id: String,
        val label: String,
        val type: String
    )
}