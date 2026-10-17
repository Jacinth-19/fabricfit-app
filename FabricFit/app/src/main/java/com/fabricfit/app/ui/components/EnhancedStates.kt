package com.fabricfit.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R
import com.fabricfit.app.ui.theme.HeritageGold

/**
 * Enhanced empty state with subtle floating animation and action-oriented CTA.
 */
@Composable
fun EnhancedEmptyState(
    @DrawableRes illustration: Int,
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: Int? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    
    // Fade in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "fadeIn"
    )

    Column(
        modifier = modifier
            .padding(32.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated illustration
        Image(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier
                .height(180.dp)
                .offset(y = offsetY.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Title with emphasis
        Text(
            stringResource(title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Body text
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Primary action button with gradient
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                stringResource(actionLabel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Optional secondary action
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(secondaryActionLabel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Shimmer effect for skeleton loaders.
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1200,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing)
        ),
        label = "shimmer"
    )
    
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, translateAnim - 200f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
    )
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )
    }
}

/**
 * Enhanced skeleton card with shimmer effect.
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
        Spacer(Modifier.height(12.dp))
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )
        Spacer(Modifier.height(8.dp))
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp)
        )
    }
}

/**
 * Grid of shimmer cards for list loading states.
 */
@Composable
fun ShimmerGrid(
    columns: Int = 2,
    rows: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(columns) {
                    ShimmerCard(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
