package com.voicesearch.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voicesearch.core.ui.theme.LocalElevation

/**
 * In-app numeric / alphanumeric keypad — an alternative to the system keyboard
 * that gives us full control of the masked display and keeps the input field
 * visible directly above the keys (it never gets covered).
 *
 * It is purely a UI driver: every tap edits the caller's text via [onKey] /
 * [onBackspace], and the confirm key calls [onDone]. Wire those to the existing
 * ViewModel methods (onManualInputChange / submitManualSearch) — no new VM
 * state is needed.
 *
 * The "АБВ / 123" toggle exposes the Cyrillic + Latin letters that appear in
 * part codes (e.g. 52ОМ139W…) for the tail of an alphanumeric suffix.
 */
@Composable
fun NumericKeypad(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
    canDone: Boolean,
    modifier: Modifier = Modifier,
) {
    var lettersMode by remember { mutableStateOf(false) }

    val digitKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val letterKeys = listOf(
        "А", "В", "Е", "К", "М", "Н", "О", "Р", "С", "Т", "У", "Х",
        "A", "B", "C", "D", "E", "F", "G", "H", "K", "L", "W", "Я",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lettersMode) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(160.dp),
                ) {
                    items(letterKeys) { key -> KeyButton(label = key, onClick = { onKey(key) }) }
                }
            } else {
                // 3 × 3 digits, then 0 spanning the last row's middle.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    digitKeys.take(9).chunked(3).forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowKeys.forEach { key ->
                                KeyButton(label = key, onClick = { onKey(key) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(
                    label = if (lettersMode) "123" else "АБВ",
                    onClick = { lettersMode = !lettersMode },
                    modifier = Modifier.weight(1f),
                    variant = KeyVariant.Util,
                )
                if (!lettersMode) {
                    KeyButton(label = "0", onClick = { onKey("0") }, modifier = Modifier.weight(1f))
                }
                KeyButton(
                    onClick = onBackspace,
                    modifier = Modifier.weight(1f),
                    variant = KeyVariant.Util,
                    icon = { Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Стереть") },
                )
                KeyButton(
                    onClick = { if (canDone) onDone() },
                    modifier = Modifier.weight(1f),
                    variant = KeyVariant.Confirm,
                    enabled = canDone,
                    icon = { Icon(Icons.Default.Check, contentDescription = "Найти") },
                )
            }
        }
    }
}

private enum class KeyVariant { Digit, Util, Confirm }

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    variant: KeyVariant = KeyVariant.Digit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (variant) {
        KeyVariant.Confirm -> scheme.secondary
        KeyVariant.Util -> scheme.surface.copy(alpha = 0.9f)
        KeyVariant.Digit -> scheme.surface
    }
    val content = when (variant) {
        KeyVariant.Confirm -> scheme.onSecondary
        else -> scheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        shadowElevation = if (variant == KeyVariant.Confirm) {
            LocalElevation.current.medium
        } else {
            LocalElevation.current.low
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> icon()
                label != null -> Text(text = label, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/**
 * Caps appended input to [slotCount] and routes through the existing change
 * callback. Pass `slotCount = 0` (or any non-positive value) for unlimited
 * length — used for FullValue tables where the search column has no fixed
 * suffix and the user types arbitrary text.
 */
fun appendCapped(current: String, key: String, slotCount: Int, onChange: (String) -> Unit) {
    if (slotCount > 0 && current.length >= slotCount) return
    onChange(current + key)
}
