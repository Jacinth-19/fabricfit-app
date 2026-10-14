package com.fabricfit.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.fabricfit.app.domain.model.AnalyzedColor
import com.fabricfit.app.ui.theme.BrandGradients

@Composable
fun SectionHeader(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes actionLabel: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 5.dp, height = 22.dp)
                    .background(BrandGradients.tick(), RoundedCornerShape(2.5.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
            )
        }
        if (actionLabel != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    stringResource(actionLabel),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * Brand hero panel: deep madder diagonal with a soft gold glow and faint diagonal weave
 * lines, rounded at extraLarge. Content should use light text colours (see BrandCream).
 */
@Composable
fun BrandPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
    ) {
        // The content column sizes the panel; gradient + weave layers match it so the
        // panel also works inside unbounded-height contexts (e.g. LazyColumn items).
        Box(Modifier.matchParentSize().background(BrandGradients.hero()))
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // soft gold glow near the top-right corner
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color(0xFFE4C27E).copy(alpha = 0.38f),
                    1f to Color(0xFFE4C27E).copy(alpha = 0f),
                    center = Offset(w * 0.94f, h * 0.02f),
                    radius = w * 0.8f,
                ),
            )
            // faint diagonal weave lines
            val gap = 26.dp.toPx()
            var start = -h
            while (start < w + h) {
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(start, h),
                    end = Offset(start + h, 0f),
                    strokeWidth = 1.5f,
                )
                start += gap
            }
            // one gold thread as the brand accent
            drawLine(
                color = Color(0xFFC8952B).copy(alpha = 0.28f),
                start = Offset(-h * 0.25f, h),
                end = Offset(h * 0.75f, 0f),
                strokeWidth = 2f,
            )
        }
        Column(Modifier.padding(24.dp)) { content() }
    }
}

/** Small woven accent: three overlapping kente-thread discs. Purely decorative. */
@Composable
fun KenteDots(modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(size * 0.5f)) {
        BrandGradients.weaveDots.forEach { c ->
            Box(
                Modifier
                    .size(size)
                    .background(c, CircleShape)
                    .border(1.dp, Color(0xFF14110F).copy(alpha = 0.22f), CircleShape),
            )
        }
    }
}

/** Rounded tonal tile that frames an icon — the app's standard icon treatment. */
@Composable
fun TintedIconTile(
    icon: ImageVector,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Composable
fun ColorDot(color: AnalyzedColor, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(Color(color.argb), CircleShape),
    )
}

@Composable
fun ChipRow(
    chips: List<FilterChipData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            FilterChip(
                selected = chip.selected,
                onClick = chip.onClick,
                label = { Text(stringResource(chip.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

data class FilterChipData(
    val labelRes: Int,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Loading placeholder with a soft moving highlight; mirrors the brand surface tints. */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
            )
            .drawWithContent {
                drawContent()
                val w = size.width
                val sweep = w * 0.7f
                val x = -sweep + (w + 2 * sweep) * phase
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        startX = x - sweep / 2,
                        endX = x + sweep / 2,
                    ),
                )
            },
    )
}
