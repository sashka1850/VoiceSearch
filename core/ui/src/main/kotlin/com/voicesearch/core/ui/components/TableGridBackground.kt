package com.voicesearch.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Paints faint, evenly-spaced horizontal + vertical hairlines behind the
 * caller — gives the screen a "fresh spreadsheet" look that thematically
 * matches what the app actually deals with, without any visual noise that
 * would compete with foreground content.
 *
 * Defaults are tuned to be barely visible on light theme and slightly more
 * present on dark theme (where the contrast goes the other way).
 */
@Composable
@ReadOnlyComposable
fun rememberTableGridColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

fun Modifier.tableGridBackground(
    lineColor: Color,
    cellSize: Dp = 28.dp,
    strokeWidth: Dp = 0.6.dp,
): Modifier = this.drawBehind {
    val cellPx = cellSize.toPx()
    val strokePx = strokeWidth.toPx()
    if (cellPx <= 0f) return@drawBehind
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokePx,
        )
        x += cellPx
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx,
        )
        y += cellPx
    }
}
