package com.fabricfit.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R

@Composable
fun EmptyState(
    @DrawableRes illustration: Int,
    @StringRes title: Int,
    @StringRes body: Int,
    modifier: Modifier = Modifier,
    actionLabel: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                modifier = Modifier.height(104.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction, modifier = Modifier.height(48.dp)) {
                Text(stringResource(actionLabel))
            }
        }
    }
}

@Composable
fun ErrorState(
    @StringRes title: Int,
    @StringRes body: Int,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
    ) {}
}

@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(140.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(16.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
    }
}
