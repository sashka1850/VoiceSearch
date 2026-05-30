package com.voicesearch.core.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.voicesearch.core.ui.components.PrimaryButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual contract for [VoiceSearchTheme].
 *
 * Recording: `gradlew core:ui:recordRoborazziDebug`
 * Verifying: `gradlew core:ui:verifyRoborazziDebug` (runs in CI on every PR)
 *
 * Snapshots are written under core/ui/src/test/snapshots/ and committed
 * to git — PR reviewers see exactly what changed visually.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-mdpi")
class ThemeSnapshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theme_light_palette() {
        composeRule.setContent {
            VoiceSearchTheme(darkTheme = false) {
                PaletteSheet()
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/theme_light_palette.png")
    }

    @Test
    fun theme_dark_palette() {
        composeRule.setContent {
            VoiceSearchTheme(darkTheme = true) {
                PaletteSheet()
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/theme_dark_palette.png")
    }
}

@androidx.compose.runtime.Composable
private fun PaletteSheet() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Заголовок (Inter SemiBold)",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Основной текст. Цифры: 1234567890",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Primary action", onClick = {})
        }
    }
}
