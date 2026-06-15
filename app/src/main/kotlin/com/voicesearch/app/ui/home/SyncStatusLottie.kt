package com.voicesearch.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import kotlinx.coroutines.delay

/**
 * TopBar sync button driven by `assets/sync_status_anim.json`. Three states:
 *
 *   NoConnection — frames 0–30, settles on the crossed-out cloud (offline).
 *                  Tapping it launches the Yandex connect flow.
 *   Connected    — frames 30–60, settles on the cloud. Tap = sync now.
 *   Syncing      — frames 60–90, upload spin played once. Triggered by the
 *                  pulsing flag that's set after a successful sync round-trip.
 *
 * The JSON ships with white strokes/fills (designer drew it for a dark
 * surface). Lottie's COLOR_FILTER dynamic-property route didn't reliably
 * descend into the precompositions in this file, so we tint at the Compose
 * layer via `graphicsLayer(Offscreen) + drawWithContent + BlendMode.SrcIn`
 * — that paints `tint` over every non-transparent pixel the Lottie engine
 * drew, working for fills and strokes regardless of layer structure.
 */
@Suppress("LongMethod") // Composable wiring: animation state + dynamic-properties + IconButton.
@Composable
fun SyncStatusButton(
    yandexAuthenticated: Boolean,
    hasSyncError: Boolean,
    lastSuccessAt: Long?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Replay the spin segment once whenever lastSuccessAt advances. Driven
    // here rather than by the caller so TopBar code stays declarative.
    var spinning by remember { mutableStateOf(false) }
    val initialSuccess = remember { lastSuccessAt }
    LaunchedEffect(lastSuccessAt) {
        if (lastSuccessAt != null && lastSuccessAt != initialSuccess) {
            spinning = true
            delay(SPIN_HOLD_MS)
            spinning = false
        }
    }
    val state = when {
        spinning && yandexAuthenticated -> SyncIndicatorState.Syncing
        hasSyncError || !yandexAuthenticated -> SyncIndicatorState.NoConnection
        else -> SyncIndicatorState.Connected
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("sync_status_anim.json"),
    )
    val animatable = rememberLottieAnimatable()

    // Parallel tinting path through Lottie's own dynamic-property system.
    // BlendMode.SrcIn already tints the final raster, but on some devices
    // Lottie's hardware acceleration path drops blend modes after the first
    // composition refresh. Replacing the source colours upstream guarantees
    // the asset already arrives in the right hue.
    val tintArgb = tint.toArgb()
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(tintArgb),
            keyPath = arrayOf("**"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = tintArgb,
            keyPath = arrayOf("**"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = tintArgb,
            keyPath = arrayOf("**"),
        ),
    )

    LaunchedEffect(composition, state) {
        val comp = composition ?: return@LaunchedEffect
        val total = comp.durationFrames.takeIf { it > 0f } ?: TOTAL_FRAMES
        when (state) {
            SyncIndicatorState.NoConnection -> animatable.snapTo(comp, progress = OFFLINE_FRAME / total)
            SyncIndicatorState.Connected -> animatable.snapTo(comp, progress = CONNECTED_FRAME / total)
            SyncIndicatorState.Syncing -> {
                animatable.animate(
                    composition = comp,
                    clipSpec = LottieClipSpec.Frame(SPIN_FROM, SPIN_TO),
                    iterations = 1,
                )
                animatable.snapTo(comp, progress = CONNECTED_FRAME / total)
            }
        }
    }

    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier.size(ICON_SLOT_SIZE.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (composition == null) {
                // Fallback for the brief window between first frame and
                // composition load (or if Lottie ever can't parse the JSON).
                val icon = when (state) {
                    SyncIndicatorState.NoConnection -> Icons.Filled.CloudOff
                    SyncIndicatorState.Connected -> Icons.Filled.Cloud
                    SyncIndicatorState.Syncing -> Icons.Filled.CloudSync
                }
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            } else {
                LottieAnimation(
                    composition = composition,
                    progress = { animatable.progress },
                    // SOFTWARE pins the renderer to a CPU path. AUTOMATIC (default)
                    // promotes to a hardware layer after the first frame, and on
                    // that path our `BlendMode.SrcIn` was dropped silently — the
                    // icon visibly disappeared once Lottie hardware-accelerated.
                    renderMode = RenderMode.SOFTWARE,
                    dynamicProperties = dynamicProperties,
                    modifier = Modifier
                        // Offscreen so the SrcIn blend has a real alpha mask
                        // to clip against (otherwise SrcIn clips against the
                        // whole window and the tint disappears).
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(color = tint, blendMode = BlendMode.SrcIn)
                        },
                )
            }
        }
    }
}

/**
 * Three discrete states the indicator can show. Kept after [SyncStatusButton]
 * so detekt's `MatchingDeclarationName` is happy with the file name.
 */
enum class SyncIndicatorState { NoConnection, Connected, Syncing }

private const val TOTAL_FRAMES = 90f
private const val OFFLINE_FRAME = 30f
private const val CONNECTED_FRAME = 60f
private const val SPIN_FROM = 60
private const val SPIN_TO = 90
private const val ICON_SLOT_SIZE = 24

// Frames 60..90 at 30fps = 1.0s. Hold the spinning flag a hair longer so
// `animate()` definitely finishes before we flip back to Connected.
private const val SPIN_HOLD_MS = 1300L
