package com.ntoma.studio.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ntoma.studio.R

internal data class Tab(val route: String, val labelRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
internal val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.DISCOVER, R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore),
    Tab(Routes.TRY_ON, R.string.nav_try_on, Icons.Filled.Checkroom, Icons.Outlined.Checkroom),
    Tab(Routes.FAVORITES, R.string.nav_favorites, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    Tab(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
)

/** Five equal touch targets; the central Try On action is emphasized, not detached.
 * Labels respect system font scaling. System gesture insets are applied exactly here.
 */
@Composable
internal fun NtomaBottomBar(currentRoute: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp), color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val central = tab.route == Routes.TRY_ON
                val label = stringResource(tab.labelRes)
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp)
                        .testTag("nav_${tab.route}")
                        .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(tab.route) })
                        .semantics { contentDescription = label }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(48.dp, 32.dp).background(
                        when { central -> colors.primary; selected -> colors.primaryContainer; else -> colors.surface },
                        RoundedCornerShape(12.dp)).then(if (central) Modifier.border(1.dp, colors.secondary, RoundedCornerShape(12.dp)) else Modifier), contentAlignment = Alignment.Center) {
                        Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, null,
                            tint = when { central -> colors.onPrimary; selected -> colors.primary; else -> colors.onSurfaceVariant },
                            modifier = Modifier.size(23.dp))
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (selected || central) colors.primary else colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}
