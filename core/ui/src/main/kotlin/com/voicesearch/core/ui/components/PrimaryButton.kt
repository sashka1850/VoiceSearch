package com.voicesearch.core.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voicesearch.core.ui.theme.LocalElevation

/**
 * App-wide primary CTA. Blue, rounded, with a real shadow that lifts
 * when idle and presses down on touch — the "floating button" feel.
 *
 * Use for: Save, Next, primary navigation actions.
 * Do NOT use for: mic / search activation — that's a green ActionFab.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val elevation = LocalElevation.current

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = elevation.high,
            pressedElevation = elevation.low,
            disabledElevation = elevation.none,
            hoveredElevation = elevation.floating,
            focusedElevation = elevation.high,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
