package com.example.voicesearch


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val SPREADSHEET_ID = "17UMoC3eob4075SsYqPK5NWHNLsAkbpKAap8C4NLDyLU"

    private lateinit var micButton: Button
    private lateinit var numberInput: EditText
    private lateinit var searchButton: Button
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var progressBar: ProgressBar

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentSpeech = ""

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
        }

        TextView(this).apply {
            text = "Поиск по таблице"
            textSize = 26f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
            layout.addView(this)
        }

        TextView(this).apply {
            text = "ЗАЖМИТЕ кнопку и говорите 8 цифр"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
            layout.addView(this)
        }

        micButton = Button(this).apply {
            text = "🎤 ЗАЖМИТЕ ДЛЯ ГОЛОСА"
            textSize = 18f
            setPadding(20, 40, 20, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(micButton)

        numberInput = EditText(this).apply {
            hint = "Или введите 8 цифр вручную"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
            textSize = 22f
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layout.addView(this)
        }

        searchButton = Button(this).apply {
            text = "НАЙТИ"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layout.addView(this)
        }

        progressBar = ProgressBar(this).apply {
            visibility = android.view.View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                topMargin = 20
            }
            layout.addView(this)
        }

        statusText = TextView(this).apply {
            text = "ЗАЖМИТЕ кнопку и говорите 8 цифр"
            setBackgroundColor(android.graphics.Color.LTGRAY)
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }
            layout.addView(this)
        }

        resultText = TextView(this).apply {
            text = ""
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            layout.addView(this)
        }

        setContentView(layout)

        checkMicrophonePermission()
        initSpeechRecognizer()

        micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startListening()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopListening()
                    true
                }
                else -> false
            }
        }

        searchButton.setOnClickListener {
            val input = numberInput.text.toString().trim()
            if (input.length == 8 && input.matches(Regex("\\d+"))) {
                performSearch(input)
            } else {
                statusText.text = "Введите ровно 8 цифр"
                resultText.text = ""
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Голосовое распознавание не доступно"
            micButton.isEnabled = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru", "RU"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                currentSpeech = ""
                micButton.text = "🎙️ ГОВОРИТЕ..."
                statusText.text = "Говорите 8 цифр..."
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Не останавливаем здесь, ждем onResults
            }

            override fun onError(error: Int) {
                isListening = false
                micButton.text = "🎤 ЗАЖМИТЕ ДЛЯ ГОЛОСА"
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Не удалось распознать"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Речь не detected"
                    else -> "Ошибка: $error"
                }
                statusText.text = errorMsg
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                micButton.text = "🎤 ЗАЖМИТЕ ДЛЯ ГОЛОСА"

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: ""

                if (spokenText.isNotEmpty()) {
                    val digits = spokenText.replace(Regex("[^0-9]"), "")

                    if (digits.length >= 8) {
                        val last8 = digits.takeLast(8)
                        numberInput.setText(last8)
                        statusText.text = "Распознано: \"$spokenText\" -> ищу $last8"
                        performSearch(last8)
                    } else if (digits.isNotEmpty()) {
                        statusText.text = "Распознано только ${digits.length} цифр: $digits"
                        numberInput.setText(digits)
                        Toast.makeText(this@MainActivity, "Нужно 8 цифр. Сказано: $spokenText", Toast.LENGTH_LONG).show()
                    } else {
                        statusText.text = "В сказанном не найдено цифр"
                    }
                } else {
                    statusText.text = "Не удалось распознать речь"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotEmpty()) {
                    currentSpeech = partialText
                    val digits = partialText.replace(Regex("[^0-9]"), "")
                    statusText.text = "🎙️ $partialText"

                    // Если уже есть 8 цифр, можно остановить запись
                    if (digits.length >= 8) {
                        stopListening()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
        speechRecognizer?.stopListening()
    }

    private fun startListening() {

        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru", "RU"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
        }
    }

    private fun performSearch(number: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                launch(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.VISIBLE
                    statusText.text = "Загружаем таблицу..."
                    resultText.text = ""
                }

                val url = URL("https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/export?format=csv")
                val connection = url.openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val csvText = connection.getInputStream().bufferedReader().readText()

                launch(Dispatchers.Main) {
                    statusText.text = "Ищем $number..."
                }

                val lines = csvText.lines()
                var foundRow = -1
                var foundFullValue = ""

                for ((index, line) in lines.withIndex()) {
                    if (index == 0) continue

                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val fullNumber = parts[1].trim().trim('"')

                        if (fullNumber.isNotEmpty()) {
                            val last8Digits = if (fullNumber.length >= 8) fullNumber.takeLast(8) else ""

                            if (last8Digits == number) {
                                foundRow = index + 1
                                foundFullValue = fullNumber
                                break
                            }
                        }
                    }
                }

                if (foundRow != -1) {
                    markRowInSheet(foundRow)
                }

                launch(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE

                    if (foundRow != -1) {
                        statusText.text = "НАЙДЕНО И ОТМЕЧЕНО!"
                        resultText.text = """
                            НАЙДЕНО И ОТМЕЧЕНО!
                            
                            Номер в таблице: $foundFullValue
                            Последние 8 цифр: ${foundFullValue.takeLast(8)}
                            Строка: $foundRow
                            
                            В колонку A поставлена отметка "Есть"
                        """.trimIndent()
                        numberInput.text.clear()
                    } else {
                        val sample = StringBuilder()
                        var count = 0
                        for ((index, line) in lines.withIndex()) {
                            if (index > 0 && count < 5) {
                                val parts = line.split(",")
                                if (parts.size >= 2) {
                                    val num = parts[1].trim().trim('"')
                                    if (num.isNotEmpty()) {
                                        val last8 = if (num.length >= 8) num.takeLast(8) else num
                                        sample.append("Строка ${index + 1}: $num -> последние 8: $last8\n")
                                        count++
                                    }
                                }
                            }
                        }

                        resultText.text = """
                            НЕ НАЙДЕНО
                            
                            Искали: $number
                            
                            Первые номера в таблице:
                            $sample
                        """.trimIndent()
                        statusText.text = "Не найдено"
                    }
                }

            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    statusText.text = "Ошибка"
                    resultText.text = """
                        Ошибка: ${e.message}
                        
                        Проверьте интернет и ID таблицы
                    """.trimIndent()
                }
            }
        }
    }

    private fun markRowInSheet(rowIndex: Int) {
        try {
            val scriptUrl = "https://script.google.com/macros/s/AKfycbwEKKeHgscW830Fh11paF8tUdaGrq2NpHD_Ol2pblhChLm-4WG06-t6bRBMQZROP3mQHQ/exec"
            val url = URL("$scriptUrl?row=$rowIndex&value=${URLEncoder.encode("Есть", "UTF-8")}")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.getInputStream().close()
        } catch (e: Exception) {
            println("Ошибка отметки: ${e.message}")
        }
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    statusText.text = "Доступ к микрофону разрешён"
                    initSpeechRecognizer()
                } else {
                    statusText.text = "Без микрофона голосовой ввод не работает"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}