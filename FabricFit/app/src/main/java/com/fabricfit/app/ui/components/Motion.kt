package com.fabricfit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The app's single motion timeline.
 *
 * Screens do not own their entrances. One [MotionPhase] is published above the `NavHost`
 * by [MotionTimeline], and every screen that animates reads that same value. Because the
 * entering screen, the leaving screen and the shared hero all sample one progress number,
 * navigation reads as one continuous movement rather than a sequence of independent
 * per-screen reveals that happen to look similar.
 *
 * Concretely:
 *  - a screen's rows cascade off [MotionPhase.settled], not off their own
 *    `LaunchedEffect`, so they start when the navigator says so;
 *  - the hero photograph is carried with [Modifier.heroCarry], which keeps it at a stable
 *    scale while the page behind it swaps, so the image appears to persist across the
 *    transition instead of cutting;
 *  - [MotionPhase.direction] lets the outgoing screen foreshadow the movement it is part
 *    of (rising forward, settling back) instead of just fading.
 *
 * There is no frame-loop clock and no per-item timer: the only animation running is the
 * one the navigator is already doing.
 */

/** Shared durations, in milliseconds. One place to change the tempo of the whole app. */
object MotionSpec {
    /** Time for a screen's content to settle after it becomes the current destination. */
    const val SCREEN_SETTLE_MILLIS = 420

    /**
     * Hero travel. Deliberately longer than the page crossfade so the photograph is still
     * moving after the background has finished swapping — that overlap is the thing that
     * reads as continuity.
     */
    const val HERO_TRAVEL_MILLIS = 520

    /** Gap between consecutive rows on a settling screen. */
    const val ROW_STAGGER_MILLIS = 55

    /** How many rows the stagger is spread over before it stops compounding. */
    const val ROW_STAGGER_STEPS = 5

    /** How far a settling row travels, in dp. */
    const val ROW_RISE_DP = 16

    /** How far the outgoing page drifts back, in dp, so the two pages do not overlap flat. */
    const val PAGE_PARALLAX_DP = 28
}

/**
 * How the current navigation is progressing.
 *
 * @property settled 0f immediately after a navigation starts, 1f once the destination has
 *   arrived. Driven by the navigator, shared by every screen.
 * @property direction +1 forward, -1 back, 0 idle. Lets a screen lean into the movement.
 * @property route the destination being animated to, so screens can tell whether they are
 *   the subject of this transition.
 */
data class MotionPhase(
    val settled: Float = 1f,
    val direction: Int = 0,
    val route: String? = null,
)

val LocalMotionPhase = staticCompositionLocalOf { MotionPhase() }

/** Publishes the one phase every screen animates from. Mount above the `NavHost`. */
@Composable
fun MotionTimeline(
    phase: MotionPhase,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMotionPhase provides phase, content = content)
}

/**
 * Reveals content on the shared timeline.
 *
 * [order] staggers rows by a fixed beat, but the last staggered row always finishes on
 * [MotionSpec.SCREEN_SETTLE_MILLIS] together with the hero — so a stack of cards and the
 * banner beside it land on the same frame instead of dribbling in after each other.
 */
@Composable
fun Modifier.revealOnEnter(
    order: Int = 0,
    distance: Int = MotionSpec.ROW_RISE_DP,
): Modifier {
    val progress = settledProgressForRow(LocalMotionPhase.current.settled, order)
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.dp.toPx()
    }
}

/**
 * Where a row of [order] sits at a given point on the shared timeline.
 *
 * Pure so it can be asserted in a JVM test: the invariant worth protecting is that a
 * higher [order] never finishes *after* a lower one, and that every row reaches exactly
 * 1f when the timeline settles. If those break, screens start looking like sequences of
 * independent animations again.
 */
fun settledProgressForRow(settled: Float, order: Int): Float {
    val steps = order.coerceIn(0, MotionSpec.ROW_STAGGER_STEPS)
    val start = steps * MotionSpec.ROW_STAGGER_MILLIS
    val span = (MotionSpec.SCREEN_SETTLE_MILLIS - start).coerceAtLeast(1)
    val elapsed = settled.coerceIn(0f, 1f) * MotionSpec.SCREEN_SETTLE_MILLIS
    return FastOutSlowInEasing.transform(((elapsed - start) / span).coerceIn(0f, 1f))
}

/**
 * Carries a hero photograph across a navigation.
 *
 * During a transition the image scales very slightly and drifts against the page
 * parallax, so it holds the eye while everything behind it changes. Once settled the
 * transform is exactly identity, so this costs nothing at rest.
 */
@Composable
fun Modifier.heroCarry(
    phase: MotionPhase = LocalMotionPhase.current,
    strength: Float = 0.06f,
    driftDp: Int = 10,
): Modifier {
    val leaving = 1f - phase.settled
    val scale = 1f + leaving * strength
    // toPx() needs a Density receiver, which only exists inside the graphicsLayer draw
    // scope - so the dp value is carried in and converted there.
    val driftDpValue = leaving * driftDp * phase.direction
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        val drift = driftDpValue.dp.toPx()
        translationY = -drift * 0.4f
        translationX = drift * 0.15f
    }
}

/**
 * The page-level half of the movement: the surface recedes a few dp while it is entering,
 * against the direction of travel. Applied to the destination root it gives every screen
 * the same sense of being pushed into place, which is what stops navigation reading as a
 * plain crossfade.
 */
@Composable
fun Modifier.pageParallax(
    phase: MotionPhase = LocalMotionPhase.current,
): Modifier {
    val travelling = 1f - phase.settled
    val pxDp = travelling * MotionSpec.PAGE_PARALLAX_DP * phase.direction
    return this.graphicsLayer { translationX = pxDp.dp.toPx() }
}

/**
 * Stacks [content] in a column where each item reveals in sequence on the shared beat.
 */
@Composable
fun StaggeredReveal(
    modifier: Modifier = Modifier,
    count: Int,
    content: @Composable ColumnScope.(index: Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(count) { index ->
            Column(modifier = Modifier.revealOnEnter(order = index)) {
                content(index)
            }
        }
    }
}

/**
 * A slow breathing scale for hero surfaces, so a still image never looks frozen. Runs on
 * its own infinite transition (draw-phase only, so it does not recompose) and is
 * intentionally not tied to navigation — it is ambience, not choreography.
 */
@Composable
fun Modifier.ambientBreath(
    amplitude: Float = 0.012f,
    periodMillis: Int = 9000,
): Modifier {
    val transition = rememberInfiniteTransition(label = "ambientBreath")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
        ),
        label = "ambientBreathWave",
    )
    val scale = 1f + amplitude * kotlin.math.sin(wave)
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
