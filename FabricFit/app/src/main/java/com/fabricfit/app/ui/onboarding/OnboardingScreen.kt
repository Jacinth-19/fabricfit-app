package com.fabricfit.app.ui.onboarding

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.StylePreference
import com.fabricfit.app.domain.model.UserPreferences
import com.fabricfit.app.ui.components.BrandMark
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer

private data class Page(
    /** Asset path of a real catalogue photo, or null for the branded opening page. */
    val photo: String?,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val dark: Boolean = false,
)

/**
 * The opening pages show the app's own photography rather than flat vector art.
 *
 * The previous illustrations were generic line drawings that shared no visual language with
 * the rest of the app, which is built on real Ghanaian fabric and outfit imagery. Showing
 * actual swatches tells a new user what the app is for far faster than an icon does.
 */
private val pages = listOf(
    Page(null, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body, dark = true),
    Page("catalog/heritage/scan_kente_closeup.jpg", R.string.onboarding_step1_title, R.string.onboarding_step1_body),
    Page("catalog/heritage/outfit_modern_kente.jpg", R.string.onboarding_step2_title, R.string.onboarding_step2_body),
    Page("catalog/heritage/dress_kente_flare.jpg", R.string.onboarding_step3_title, R.string.onboarding_step3_body),
    Page("catalog/heritage/outfit_elegant_peplum.jpg", R.string.onboarding_step4_title, R.string.onboarding_step4_body),
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
                state = pagerState,
                modifier = Modifier.weight(1f),
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
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B4D3B),
                                contentColor = HeritageCream,
                            ),
                        ) {
                            Text(
                                stringResource(R.string.onboarding_cta_get_started),
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
                        ) {
                            Text(
                                stringResource(R.string.onboarding_cta_login),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    last -> {
                        Button(
                            onClick = { onFinished(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.onboarding_cta_scan))
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
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
            }
        }
    }

    if (loginDialogOpen) {
        ProfileIdentityDialog(
            title = stringResource(R.string.onboarding_cta_login),
            confirmLabel = stringResource(R.string.onboarding_cta_get_started),
            initialName = prefs.displayName.ifBlank { "Ama Serwaa" },
            initialBio = prefs.profileBio.ifBlank { "Fashion Lover" },
            initialAvatarUri = prefs.avatarUri.ifBlank { HERITAGE_AVATAR_PRESETS.first() },
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
    initialAvatarUri: String,
    initialStylePreference: StylePreference,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bio: String, avatarUri: String, stylePreference: StylePreference) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var avatarUri by remember { mutableStateOf(initialAvatarUri) }
    var stylePref by remember { mutableStateOf(initialStylePreference) }

    val avatarPicker = rememberPhotoPicker { uri ->
        scope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val out = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    out.takeIf { it.exists() && it.length() > 0L }?.absolutePath
                } catch (_: Exception) {
                    null
                }
            }
            if (savedPath != null) avatarUri = savedPath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileAvatarImage(
                        avatarUri = avatarUri,
                        contentDescription = name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, HeritageGold, CircleShape),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Choose Avatar",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HERITAGE_AVATAR_PRESETS.forEach { preset ->
                                val selected = avatarUri == preset
                                AssetImage(
                                    asset = preset,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) HeritageGold else Color.Transparent,
                                            shape = CircleShape,
                                        )
                                        .clickable { avatarUri = preset },
                                )
                            }
                            Surface(
                                onClick = {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
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
                    text = "Style Preference",
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
    // Shopping-app welcome: the editorial photograph owns the top of the screen, and the
    // copy sits on a clean cream panel beneath it. Keeping them apart means the headline
    // is always legible, whatever the photo is doing, and the model's head stays in frame.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        shown = true
    }
    val rise by animateDpAsState(
        targetValue = if (shown) 0.dp else 26.dp,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "welcomeRise",
    )
    val fade by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "welcomeFade",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(HeritageCream),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)),
        ) {
            AssetImage(
                asset = "catalog/photos/women_kente_01.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            // Soft top scrim so the status bar and wordmark stay readable over the photo.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(HeritageForestDark.copy(alpha = 0.55f), Color.Transparent),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HeritageCream,
                    modifier = Modifier.size(38.dp),
                ) {
                    HeritageLuxeMark(modifier = Modifier.fillMaxSize().padding(6.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    stringResource(R.string.onboarding_brand_heritage_luxe),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = HeritageCream,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HeritageCream.copy(alpha = 0.22f),
                ) {
                    Text(
                        stringResource(R.string.onboarding_brand_badge),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeritageCream,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = rise)
                .padding(horizontal = 26.dp)
                .padding(top = 22.dp, bottom = 10.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_welcome_headline),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 33.sp,
                lineHeight = 38.sp,
                color = HeritageForestDark,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                stringResource(R.string.onboarding_welcome_sub),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = HeritageForestDark.copy(alpha = 0.74f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { alpha = fade },
            ) {
                WelcomeProofChip(R.string.onboarding_proof_offline)
                WelcomeProofChip(R.string.onboarding_proof_tryon)
                WelcomeProofChip(R.string.onboarding_proof_free)
            }
        }
    }
}

@Composable
private fun WelcomeProofChip(@StringRes text: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = HeritageForest.copy(alpha = 0.10f),
    ) {
        Text(
            stringResource(text),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = HeritageForestDark,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
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
        if (page.photo != null) {
            // A framed photo card, echoing the catalogue tiles elsewhere in the app, so the
            // onboarding reads as part of the same product rather than a separate slideshow.
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp),
            ) {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/${page.photo}")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Soft top-down scrim so the gold hairline reads on pale fabrics.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.18f),
                                    ),
                                ),
                            ),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BrandMark(modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
