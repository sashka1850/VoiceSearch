package com.voicesearch.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.app.feature.auth.yandex.YandexAuthEvents
import com.voicesearch.app.navigation.VoiceSearchNavHost
import com.voicesearch.app.ui.settings.SettingsViewModel
import com.voicesearch.core.domain.repository.ThemeMode
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
            AppRoot()
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

/**
 * Top-level composable. Pulls the chosen [ThemeMode] from preferences once,
 * applies it to [VoiceSearchTheme], then hands off to the navigation graph.
 *
 * Lives at activity level so theme changes apply across every destination
 * instantly — no per-screen wiring needed.
 */
@Composable
private fun AppRoot() {
    val themeVm: SettingsViewModel = hiltViewModel()
    val mode by themeVm.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    VoiceSearchTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            VoiceSearchNavHost()
        }
    }
}
