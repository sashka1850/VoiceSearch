package com.voicesearch.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.voicesearch.app.R

/**
 * Three-state Lottie indicator baked into `sync_status_anim.json`:
 *
 *   frames 0–30  → disconnect animation, settles on "offline cloud"
 *   frames 30–60 → connect animation, settles on "online cloud"
 *   frames 60–90 → upload spin, replayed once after each successful sync
 *
 * We don't loop any of the segments — each state resolves to a static
 * pose. [SyncIndicatorState.Syncing] plays the spin once then falls back
 * to the connected frame; callers drive that with a short-lived bool
 * (e.g. the existing `pulsing` flag in TableInfoCard).
 */
@Composable
fun SyncStatusLottie(
    state: SyncIndicatorState,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.sync_status_anim),
    )
    val animatable = rememberLottieAnimatable()

    LaunchedEffect(composition, state) {
        val comp = composition ?: return@LaunchedEffect
        val total = comp.durationFrames.takeIf { it > 0f } ?: TOTAL_FRAMES
        when (state) {
            SyncIndicatorState.NoConnection -> {
                animatable.snapTo(comp, progress = OFFLINE_FRAME / total)
            }
            SyncIndicatorState.Connected -> {
                animatable.snapTo(comp, progress = CONNECTED_FRAME / total)
            }
            SyncIndicatorState.Syncing -> {
                // Replay the upload-spin segment once, then leave the cloud
                // visible. If `state` flips back to Connected mid-animation
                // the LaunchedEffect restarts and snapTo wins — safe.
                animatable.animate(
                    composition = comp,
                    clipSpec = LottieClipSpec.Frame(SPIN_FROM, SPIN_TO),
                    iterations = 1,
                )
                animatable.snapTo(comp, progress = CONNECTED_FRAME / total)
            }
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { animatable.progress },
        modifier = modifier,
    )
}

/**
 * Three discrete states the indicator can show. Kept after [SyncStatusLottie]
 * so detekt's `MatchingDeclarationName` is happy with the file name.
 */
enum class SyncIndicatorState { NoConnection, Connected, Syncing }

private const val TOTAL_FRAMES = 90f
private const val OFFLINE_FRAME = 30f
private const val CONNECTED_FRAME = 60f
private const val SPIN_FROM = 60
private const val SPIN_TO = 90
