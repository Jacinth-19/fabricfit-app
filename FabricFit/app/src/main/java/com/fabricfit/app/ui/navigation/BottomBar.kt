package com.fabricfit.app.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold

private fun isTabSelected(currentRoute: String?, tabRoute: String): Boolean =
    currentRoute == tabRoute ||
        (tabRoute == Routes.DISCOVER && currentRoute == Routes.DISCOVER_ROUTE_PATTERN)

internal data class Tab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCentre: Boolean = false,
)

/**
 * Heritage Luxe bottom navigation:
 *  Home · Discover · [Camera FAB] · Try-On · Profile
 *  The centre Camera tab is a raised Deep Forest circle with a white camera glyph
 *  and routes to the Create / capture hub (which then leads to FitScan camera).
 */
internal val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.DISCOVER, R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore),
    Tab(Routes.CREATE, R.string.nav_camera, Icons.Filled.CameraAlt, Icons.Filled.CameraAlt, isCentre = true),
    Tab(Routes.FAVORITES, R.string.nav_tryon, Icons.Filled.Checkroom, Icons.Outlined.Checkroom),
    Tab(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
internal fun FabricFitBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkChrome = currentRoute == Routes.PROFILE || currentRoute == Routes.CREATE
    val barColor = if (isDarkChrome) HeritageForestDark else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier,
        color = barColor,
        tonalElevation = if (isDarkChrome) 0.dp else 8.dp,
        shadowElevation = 8.dp,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(
                    density = androidx.compose.ui.platform.LocalDensity.current.density,
                    fontScale = 1f,
                ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                tabs.forEach { tab ->
                    val isSelected = isTabSelected(currentRoute, tab.route)
                    if (tab.isCentre) {
                        CameraTab(
                            label = stringResource(tab.labelRes),
                            selected = isSelected,
                            darkChrome = isDarkChrome,
                            onClick = { onNavigate(tab.route) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        StandardTab(
                            tab = tab,
                            selected = isSelected,
                            darkChrome = isDarkChrome,
                            onClick = { onNavigate(tab.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardTab(
    tab: Tab,
    selected: Boolean,
    darkChrome: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tab.labelRes)
    val tint = when {
        darkChrome && selected -> Color.White
        darkChrome -> HeritageCream.copy(alpha = 0.65f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
            }
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun CameraTab(
    label: String,
    selected: Boolean,
    darkChrome: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cameraScale",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(y = (-8).dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(HeritageForest, CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.2.dp,
                    color = if (selected) {
                        HeritageGold
                    } else {
                        HeritageGold.copy(alpha = if (darkChrome) 0.55f else 0.28f)
                    },
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = HeritageCream,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
