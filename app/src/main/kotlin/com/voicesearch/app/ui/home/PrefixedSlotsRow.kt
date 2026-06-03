package com.voicesearch.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual "fill-the-blanks" row.
 *
 * Renders the detected prefix verbatim, followed by [slotCount] character
 * slots that show either the next character from [filled] (left-to-right) or
 * an em-dash placeholder.
 *
 * Shared between the keyboard input bar and the live voice transcript display
 * so the user sees the same shape regardless of input modality.
 */
@Composable
fun PrefixedSlotsRow(
    prefix: String,
    filled: String,
    slotCount: Int,
    modifier: Modifier = Modifier,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    placeholderColor: Color = MaterialTheme.colorScheme.outline,
    prefixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (slotCount <= 0) return

    // For variable-length suffixes the user may overshoot the slot count; show
    // the trailing characters that actually fit.
    val displayed = filled.takeLast(slotCount.coerceAtLeast(filled.length))
        .take(slotCount)

    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = monoStyle,
                color = prefixColor,
            )
            Spacer(Modifier.width(8.dp))
        }
        repeat(slotCount) { i ->
            val char = displayed.getOrNull(i)
            Text(
                text = char?.toString() ?: "—",
                style = monoStyle,
                color = if (char != null) filledColor else placeholderColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
