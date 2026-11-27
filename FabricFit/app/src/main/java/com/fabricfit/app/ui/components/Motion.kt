package com.fabricfit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Fade-and-rise entrance used across the app so screens settle in instead of snapping
 * into place.
 *
 * The effect runs once per composition: the first frame is drawn transparent and offset,
 * then a short tween brings it home. [delayMillis] lets stacked items cascade - pass
 * `index * 60` for a staggered list.
 *
 * Only transform and alpha are animated, both of which the renderer composites on the
 * GPU, so a screen with twenty revealed rows still scrolls smoothly.
 */
@Composable
fun Modifier.revealOnEnter(
    delayMillis: Int = 0,
    distance: Int = 18,
    durationMillis: Int = 420,
): Modifier {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        started = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "revealOnEnter",
    )
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.dp.toPx()
    }
}

/**
 * Stacks [content] in a column where each item reveals in sequence. Use for the first few
 * rows of a screen; repeating it for every row of a long list costs more than it adds.
 */
@Composable
fun StaggeredReveal(
    modifier: Modifier = Modifier,
    count: Int,
    stepMillis: Int = 70,
    content: @Composable ColumnScope.(index: Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(count) { index ->
            Column(modifier = Modifier.revealOnEnter(delayMillis = index * stepMillis)) {
                content(index)
            }
        }
    }
}
