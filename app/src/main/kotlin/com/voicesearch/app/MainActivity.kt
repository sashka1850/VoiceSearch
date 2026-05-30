package com.voicesearch.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.voicesearch.app.feature.auth.yandex.YandexAuthEvents
import com.voicesearch.app.navigation.VoiceSearchNavHost
import com.voicesearch.core.ui.theme.VoiceSearchTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var yandexAuthEvents: YandexAuthEvents

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleYandexRedirect(intent)
        setContent {
            VoiceSearchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceSearchNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleYandexRedirect(intent)
    }

    /**
     * Pulled out so both cold-start (Activity recreated from the deep link)
     * and warm-start (we're already in the foreground) flows reuse one path.
     */
    private fun handleYandexRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (!data.scheme.equals(YANDEX_REDIRECT_SCHEME, ignoreCase = true)) return
        val code = data.getQueryParameter("code") ?: return
        yandexAuthEvents.publishCode(code)
    }

    private companion object {
        const val YANDEX_REDIRECT_SCHEME = "yandexta"
    }
}
