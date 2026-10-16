package com.fabricfit.app.ui.onboarding

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.StylePreference
import com.fabricfit.app.domain.model.UserPreferences
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.HeritageLuxeMark
import com.fabricfit.app.ui.components.LocalImage
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.theme.HeritageGoldSoft
import com.fabricfit.app.ui.util.rememberPhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class Page(
    @DrawableRes val illustration: Int?,
    Page(R.drawable.illust_onboarding_save, R.string.onboarding_step4_title, R.string.onboarding_step4_body),
)

internal val HERITAGE_AVATAR_PRESETS = listOf(
    "catalog/heritage/avatar_ama_serwaa.jpg",
    "catalog/heritage/outfit_modern_kente.jpg",
    "catalog/heritage/outfit_elegant_peplum.jpg",
)

internal val HERITAGE_ROLE_PRESETS = listOf(
    "Fashion Lover",
    "Bespoke Tailor",
    "Kente Collector",
    "Style Creator",
)

@Composable
fun OnboardingScreen(onFinished: (scanNow: Boolean) -> Unit) {
    val context = LocalContext.current
    val container = AppContainer.get(context)
    val prefs by container.settings.preferences
        .collectAsStateWithLifecycle(initialValue = UserPreferences())
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val pageIndex by remember { derivedStateOf { pagerState.currentPage } }
    val page = pages[pageIndex]
    var loginDialogOpen by remember { mutableStateOf(false) }

    val bgColor = if (page.dark) HeritageForest else MaterialTheme.colorScheme.background

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(Modifier.fillMaxSize()) {
            // Top bar with "Skip for now" accessible on all pages
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onFinished(false) }) {
                    Text(
                        stringResource(R.string.onboarding_cta_skip),
                        color = if (page.dark) {
                            HeritageGoldSoft.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            HorizontalPager(
            ) { index ->
                val p = pages[index]
                if (p.dark) {
                    WelcomeDarkPage()
                } else {
                    LightPage(p)
                }
            }

            // Indicator dots (shown on step pages 1..4)
            if (!page.dark) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pages.size) { i ->
                        val isCurrent = i == pagerState.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 5.dp)
                                .size(if (isCurrent) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
            }

            // Bottom actions
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
            ) {
                val last = pagerState.currentPage == pages.size - 1
                when {
                    page.dark -> {
                        Button(
                            onClick = {
                                scope.launch {
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B4D3B),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { loginDialogOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HeritageGoldSoft,
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                HeritageGoldSoft.copy(alpha = 0.6f),
                            ),
                    last -> {
                        Button(
                            onClick = { onFinished(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.onboarding_cta_scan))
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.onboarding_next))
            }
        }
    }

    if (loginDialogOpen) {
        ProfileIdentityDialog(
            title = stringResource(R.string.onboarding_cta_login),
            confirmLabel = stringResource(R.string.onboarding_cta_get_started),
            initialName = prefs.displayName.ifBlank { "Ama Serwaa" },
            initialBio = prefs.profileBio.ifBlank { "Fashion Lover" },
            initialAvatar = prefs.avatarUri.ifBlank { HERITAGE_AVATAR_PRESETS.first() },
            initialStylePreference = prefs.stylePreference,
            onDismiss = { loginDialogOpen = false },
            onConfirm = { name, bio, avatarUri, stylePref ->
                loginDialogOpen = false
                scope.launch {
                    container.settings.update {
                        it.copy(
                            displayName = name.trim().ifBlank { "Ama Serwaa" },
                            profileBio = bio.trim().ifBlank { "Fashion Lover" },
                            avatarUri = avatarUri,
                            stylePreference = stylePref,
                            onboardingCompleted = true,
                        )
                    }
                    onFinished(false)
                }
            },
        )
    }
}

@Composable
fun ProfileIdentityDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialBio: String,
    initialAvatar: String,
    initialStylePreference: StylePreference,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bio: String, avatarUri: String, stylePreference: StylePreference) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var avatarUri by remember { mutableStateOf(initialAvatar.ifBlank { HERITAGE_AVATAR_PRESETS.first() }) }
    var stylePref by remember { mutableStateOf(initialStylePreference) }

    val photoPicker = rememberPhotoPicker { pickedUri ->
        scope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val out = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(pickedUri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (out.exists() && out.length() > 0L) out.absolutePath else null
                } catch (_: Exception) {
                    null
                }
            }
            if (savedPath != null) {
                avatarUri = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeritageLuxeMark(modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Avatar preview + preset strip + gallery button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileAvatarImage(
                        avatarUri = avatarUri,
                        contentDescription = name,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(2.dp, HeritageGold, CircleShape),
                    )
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HERITAGE_AVATAR_PRESETS.forEach { preset ->
                                val selected = avatarUri == preset
                                AssetImage(
                                    asset = preset,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) HeritageGold else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape,
                                        )
                                        .clickable { avatarUri = preset },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 4.dp,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.tryon_photo_gallery),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Role / Style Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HERITAGE_ROLE_PRESETS.forEach { role ->
                        SuggestionChip(
                            onClick = { bio = role },
                            label = { Text(role, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.profile_preference_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StylePreference.entries.forEach { pref ->
                        FilterChip(
                            selected = stylePref == pref,
                            onClick = { stylePref = pref },
                            label = { Text(stringResource(pref.labelRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, bio, avatarUri, stylePref) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeritageForest,
                    contentColor = HeritageCream,
                ),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun ProfileAvatarImage(
    avatarUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val resolved = avatarUri.ifBlank { HERITAGE_AVATAR_PRESETS.first() }
    if (resolved.startsWith("catalog/")) {
        AssetImage(
            asset = resolved,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        LocalImage(
            uri = resolved,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun WelcomeDarkPage() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        HeritageForestDark,
                        HeritageForest,
                        Color(0xFF083023),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            // Brand mark — gold adinkra monogram
            HeritageLuxeMark(modifier = Modifier.size(116.dp))

            Spacer(Modifier.height(16.dp))

            // Wordmark — "Heritage Luxe" in royal gold serif
            Text(
                stringResource(R.string.onboarding_brand_heritage_luxe),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = HeritageGold,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Tagline — "Scan. Discover. Try. Wear."
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                ),
                color = HeritageCream,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Stacked Kente textile folds hero
            AssetImage(
                asset = "catalog/heritage/hero_kente_folds.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
@Composable
private fun LightPage(page: Page) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {