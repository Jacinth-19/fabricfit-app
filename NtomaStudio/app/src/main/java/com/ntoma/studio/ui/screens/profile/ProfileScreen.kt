package com.ntoma.studio.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.ui.navigation.Routes
import com.ntoma.studio.ui.navigation.appViewModel
import com.ntoma.studio.ui.theme.DarkSurfaceContainerHigh
import com.ntoma.studio.ui.theme.ForestGreen
import com.ntoma.studio.ui.theme.KenteGold
import kotlinx.coroutines.launch

private val ProfileIvory = androidx.compose.ui.graphics.Color(0xFFF2F3F7)
private val ProfileIvoryDim = androidx.compose.ui.graphics.Color(0xFFB9C1D0)
private val ProfileCard = DarkSurfaceContainerHigh

/** Heritage Luxe profile: deep forest surface, gold-ringed avatar, stats and menu list. */
@Composable
fun ProfileScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: ProfileViewModel = appViewModel { c, ctx -> ProfileViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var renameOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = ForestGreen) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = ProfileIvory,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.profile_row_settings),
                        tint = ProfileIvory,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .border(3.dp, KenteGold, CircleShape)
                        .padding(3.dp)
                        .background(ProfileCard, CircleShape),
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = KenteGold,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.prefs.displayName.ifBlank { stringResource(R.string.profile_guest) },
                        style = MaterialTheme.typography.titleLarge,
                        color = ProfileIvory,
                    )
                    Text(
                        stringResource(R.string.profile_guest_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfileIvoryDim,
                    )
                }
                IconButton(onClick = { renameOpen = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.profile_name_hint),
                        tint = ProfileIvoryDim,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                StatBlock(R.string.profile_stat_scans, state.fabricCount, Modifier.weight(1f))
                StatBlock(R.string.profile_stat_favorites, state.favoriteCount, Modifier.weight(1f))
                StatBlock(R.string.profile_stat_tryons, state.lookCount, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            ProfileRow(
                icon = Icons.Filled.Checkroom,
                label = R.string.profile_row_wardrobe,
                onClick = { nav.navigate(Routes.WARDROBE) },
            )
            ProfileRow(
                icon = Icons.Filled.Bookmark,
                label = R.string.profile_menu_saved,
                onClick = { nav.navigate(Routes.FAVORITES) },
            )
            ProfileRow(
                icon = Icons.Filled.History,
                label = R.string.profile_menu_history,
                onClick = { nav.navigate(Routes.HISTORY) },
            )
            ProfileRow(
                icon = Icons.Filled.Settings,
                label = R.string.profile_row_settings,
                onClick = { nav.navigate(Routes.SETTINGS) },
            )
            ProfileRow(
                icon = Icons.Filled.HelpOutline,
                label = R.string.profile_menu_help,
                onClick = { nav.navigate(Routes.HELP) },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (renameOpen) {
        var text by remember { mutableStateOf(state.prefs.displayName) }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(stringResource(R.string.profile_name_hint)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { vm.setName(text) }
                    renameOpen = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun StatBlock(label: Int, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = KenteGold,
        )
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = ProfileIvoryDim,
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = ProfileCard,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KenteGold,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(label),
                style = MaterialTheme.typography.titleMedium,
                color = ProfileIvory,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ProfileIvoryDim,
            )
        }
    }
}
