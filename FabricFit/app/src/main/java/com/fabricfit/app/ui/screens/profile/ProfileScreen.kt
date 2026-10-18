package com.fabricfit.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.onboarding.HERITAGE_AVATAR_PRESETS
import com.fabricfit.app.ui.onboarding.ProfileAvatarImage
import com.fabricfit.app.ui.onboarding.ProfileIdentityDialog
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold

@Composable
fun ProfileScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: ProfileViewModel = appViewModel { c, ctx -> ProfileViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var editProfileOpen by remember { mutableStateOf(false) }

    val displayName = state.prefs.displayName.ifBlank { "Ama Serwaa" }
    val profileBio = state.prefs.profileBio.ifBlank { "Fashion Lover" }
    val avatarUri = state.prefs.avatarUri.ifBlank { HERITAGE_AVATAR_PRESETS.first() }
    val scansCount = state.fabricCount
    val favsCount = state.favoriteCount
    val tryOnsCount = state.lookCount

    Column(
        Modifier
            .fillMaxSize()
            .background(HeritageForestDark)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))

        // Header: Centered "Profile" + Settings gear icon (Reference Image 6)
        Box(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = HeritageCream,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = { nav.navigate(Routes.SETTINGS) },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.profile_row_settings),
                    tint = HeritageCream,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Avatar + Name ("Ama Serwaa") + Role ("Fashion Lover")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatarImage(
                avatarUri = avatarUri,
                contentDescription = displayName,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(2.dp, HeritageGold.copy(alpha = 0.6f), CircleShape)
                    .clickable { editProfileOpen = true },
            )
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { editProfileOpen = true },
            ) {
                Text(
                    displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profileBio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HeritageCream.copy(alpha = 0.72f),
                )
            }
            IconButton(onClick = { editProfileOpen = true }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.profile_name_hint),
                    tint = HeritageCream.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Stats row with vertical dividers: 12 Scans | 8 Favorites | 3 Try-Ons
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DarkStatColumn(value = scansCount.toString(), label = "Scans", modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(HeritageCream.copy(alpha = 0.18f)),
            )
            DarkStatColumn(value = favsCount.toString(), label = "Favorites", modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(HeritageCream.copy(alpha = 0.18f)),
            )
            DarkStatColumn(value = tryOnsCount.toString(), label = "Try-Ons", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Translucent dark card container with menu rows (Reference Image 6)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF14342B),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                DarkProfileMenuRow(
                    icon = Icons.Outlined.Checkroom,
                    title = stringResource(R.string.profile_row_wardrobe),
                    onClick = { nav.navigate(Routes.WARDROBE) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Saved Styles",
                    onClick = { nav.navigate(Routes.FAVORITES) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Scan History",
                    onClick = { nav.navigate(Routes.HISTORY) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.profile_row_settings),
                    onClick = { nav.navigate(Routes.SETTINGS) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help & Support",
                    onClick = { nav.navigate(Routes.HELP) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Additional account / legal rows
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF14342B),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                DarkProfileMenuRow(
                    icon = Icons.Outlined.Straighten,
                    title = stringResource(R.string.profile_row_measurements),
                    onClick = { nav.navigate(Routes.MEASUREMENTS) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = stringResource(R.string.profile_row_premium),
                    onClick = { nav.navigate(Routes.PREMIUM) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.profile_row_privacy),
                    onClick = { nav.navigate(Routes.PRIVACY) },
                )
                MenuDivider()
                DarkProfileMenuRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.profile_row_about),
                    onClick = { nav.navigate(Routes.ABOUT) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (editProfileOpen) {
        ProfileIdentityDialog(
            title = stringResource(R.string.profile_title),
            confirmLabel = stringResource(R.string.action_save),
            initialName = displayName,
            initialBio = profileBio,
            initialAvatarUri = avatarUri,
            initialStylePreference = state.prefs.stylePreference,
            onDismiss = { editProfileOpen = false },
            onConfirm = { name, bio, chosenAvatar, stylePref ->
                editProfileOpen = false
                vm.updateProfile(
                    name = name.ifBlank { "Ama Serwaa" },
                    bio = bio.ifBlank { "Fashion Lover" },
                    avatarUri = chosenAvatar,
                    stylePreference = stylePref,
                )
            },
        )
    }
}

@Composable
private fun DarkStatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = HeritageCream.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DarkProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = title
            }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HeritageCream,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = HeritageCream.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 0.8.dp,
        color = HeritageCream.copy(alpha = 0.1f),
    )
}
