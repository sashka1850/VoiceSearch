package com.voicesearch.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * a masked placeholder ([placeholder], default ●).
 *
 * Shared between the keyboard input bar and the live voice transcript display
 * so the user sees the same shape regardless of input modality.
 *
 * When [animateReveal] is true each character gently scales + fades in the
 * moment its slot becomes filled — the "открывается" feel from the design.
 */
@Suppress("LongParameterList") // All colour/animation knobs are intentional design hooks.
@Composable
fun PrefixedSlotsRow(
    prefix: String,
    filled: String,
    slotCount: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "●",
    placeholderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    filledColor: Color = MaterialTheme.colorScheme.onSurface,
    prefixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    animateReveal: Boolean = true,
) {
    if (slotCount <= 0) return

    // For variable-length suffixes the user may overshoot the slot count; show
    // the trailing characters that actually fit.
    val displayed = filled.takeLast(slotCount.coerceAtLeast(filled.length))
        .take(slotCount)

    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 1.sp,
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = monoStyle, color = prefixColor)
            Spacer(Modifier.width(8.dp))
        }
        repeat(slotCount) { i ->
            val char = displayed.getOrNull(i)
            SlotChar(
                char = char,
                placeholder = placeholder,
                style = monoStyle,
                filledColor = filledColor,
                placeholderColor = placeholderColor,
                animateReveal = animateReveal,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SlotChar(
    char: Char?,
    placeholder: String,
    style: TextStyle,
    filledColor: Color,
    placeholderColor: Color,
    animateReveal: Boolean,
    modifier: Modifier = Modifier,
) {
    val isFilled = char != null
    // Scale-in pop for the freshly revealed glyph; placeholder stays at rest.
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.85f,
        label = "SlotChar/scale",
    )
    val effectiveScale = if (animateReveal) scale else 1f

    Text(
        text = char?.toString() ?: placeholder,
        style = style,
        color = if (isFilled) filledColor else placeholderColor,
        modifier = modifier
            .alpha(if (isFilled) 1f else 0.7f)
            .graphicsLayer { scaleX = effectiveScale; scaleY = effectiveScale },
    )
}
