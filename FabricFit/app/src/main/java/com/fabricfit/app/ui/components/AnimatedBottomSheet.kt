package com.fabricfit.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Animated bottom sheet with smooth slide-in animation and customizable content.
 * 
 * Features:
 * - Smooth spring animation (damping ratio 0.75, stiffness medium)
 * - Rounded top corners (24dp)
 * - Optional title
 * - Automatic state management
 * - Heritage Luxe theming
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    var shouldShow by remember { mutableStateOf(false) }
    
    // Animate in when visible becomes true
    LaunchedEffect(visible) {
        if (visible) {
            shouldShow = true
        }
    }
    
    // Animate out when visible becomes false
    LaunchedEffect(visible, sheetState.isVisible) {
        if (!visible && sheetState.isVisible) {
            sheetState.hide()
            shouldShow = false
        }
    }
    
    if (shouldShow) {
        ModalBottomSheet(
            onDismissRequest = {
                onDismiss()
                shouldShow = false
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Optional title
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Content
                content()
                
                // Bottom spacing for safe area
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Quick action button for use in bottom sheets.
 */
@Composable
fun BottomSheetAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )
        }
    }
}

/**
 * Divider for bottom sheets.
 */
@Composable
fun BottomSheetDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.height(8.dp))
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    ) {}
    Spacer(modifier.height(8.dp))
}
