package com.fabricfit.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.ui.components.BrandPanel
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.GarmentArt
import com.fabricfit.app.ui.components.KenteDots
import com.fabricfit.app.ui.components.LocalImage
import com.fabricfit.app.ui.components.SectionHeader
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.BrandCream
import com.fabricfit.app.ui.theme.BrandMadder
import com.fabricfit.app.ui.util.openFabricFlow
import com.fabricfit.app.ui.util.rememberPhotoPicker
import com.fabricfit.app.ui.util.timeAgo
import com.fabricfit.app.permissions.rememberCameraPermission
import kotlinx.coroutines.launch
import java.util.Calendar
import com.fabricfit.app.ui.util.resolveKey

@Composable
fun HomeScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: HomeViewModel = appViewModel { c, ctx -> HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val requestCamera = rememberCameraPermission { nav.navigate(Routes.CAMERA_FABRIC) }
    val picker = rememberPhotoPicker { uri ->
        context.openFabricFlow(
            uri,
            navigate = { nav.navigate(it) },
            onError = { e -> scope.launch { snackbar.showSnackbar(context.getString(e.titleRes)) } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // edge-to-edge: keep the hero below the status bar
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 24.dp,
            ),
        ) {
            item {
                BrandPanel(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
                    KenteDots(Modifier.padding(bottom = 14.dp))
                    Text(
                        greeting(state.prefs.displayName),
                        style = MaterialTheme.typography.displayMedium,
                        color = BrandCream,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFFFFFF, 0.82f),
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = requestCamera,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandCream,
                            contentColor = BrandMadder,
                        ),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.home_cta_scan), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = { picker.launch(pickImagesRequest()) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFFFF, 0.10f),
                        contentColor = BrandCream,
                        border = BorderStroke(1.dp, Color(0xFFFFFFFF, 0.35f)),
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.home_cta_upload))
                        }
                    }
                    if (!state.prefs.premiumEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(
                                R.string.home_quota_note,
                                state.analysesLeft.coerceAtMost(3),
                                3,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFFFFF, 0.75f),
                        )
                    }
                }
            }

            if (state.recentFabrics.isNotEmpty()) {
                item { SectionHeader(R.string.home_section_recent_fabrics) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.recentFabrics, key = { it.id }) { fabric ->
                            Card(
                                onClick = { nav.navigate(Routes.fabric(fabric.id)) },
                                modifier = Modifier.width(150.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(104.dp)
                                            .clip(MaterialTheme.shapes.small),
                                    ) {
                                        FabricSwatch(
                                            fabric,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        fabric.name ?: context.getString(fabric.category.labelRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                    Text(
                                        context.timeAgo(fabric.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.recommended.isNotEmpty()) {
                item { SectionHeader(R.string.home_section_recommended) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.recommended, key = { it.id }) { style ->
                            Card(
                                onClick = { nav.navigate(Routes.design(style.id)) },
                                modifier = Modifier.width(140.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    GarmentArt(
                                        style,
                                        null,
                                        modifier = Modifier.fillMaxWidth().height(128.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        context.resolveKey(style.titleKey) ?: style.id,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.latestLook != null) {
                item { SectionHeader(R.string.home_section_continue) }
                item {
                    Card(
                        onClick = { nav.navigate(Routes.result(state.latestLook!!.id)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LocalImage(
                                state.latestLook!!.resultImageUri,
                                contentDescription = stringResource(R.string.cd_generated_image),
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.result_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    context.timeAgo(state.latestLook!!.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = { nav.navigate(Routes.FAVORITES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Canvas(Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colorStops = listOf(
                                        Color(0xFFC8952B, 0.28f),
                                        Color(0xFFC8952B, 0f),
                                    ),
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, 0f),
                                    radius = size.width * 0.6f,
                                ),
                            )
                        }
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFFFFFFF, 0.35f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.home_section_favorites),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    stringResource(R.string.favorites_count, state.favoriteCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // Quick tools row
            item { SectionHeader(R.string.home_tools_title) }
            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = {
                            val latest = state.recentFabrics.firstOrNull()
                            if (latest != null) {
                                nav.navigate(Routes.colorMatch(latest.id))
                            } else {
                                nav.navigate(Routes.CREATE)
                            }
                        },
                        label = { Text(stringResource(R.string.tool_color_ideas)) },
                    )
                    AssistChip(
                        onClick = { nav.navigate(Routes.WARDROBE) },
                        label = { Text(stringResource(R.string.tool_wardrobe)) },
                    )
                    AssistChip(
                        onClick = { nav.navigate(Routes.TAILORS) },
                        label = { Text(stringResource(R.string.tool_tailors)) },
                    )
                    AssistChip(
                        onClick = { nav.navigate(Routes.COLLECTIONS) },
                        label = { Text(stringResource(R.string.tool_collections)) },
                    )
                }
            }

            item { SectionHeader(R.string.home_section_tips) }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TipCard(R.string.home_tip_1_title, R.string.home_tip_1_body)
                    TipCard(R.string.home_tip_2_title, R.string.home_tip_2_body)
                    TipCard(R.string.home_tip_3_title, R.string.home_tip_3_body)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(padding))
    }
}

@Composable
private fun TipCard(title: Int, body: Int) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KenteDots(size = 7.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun pickImagesRequest() = androidx.activity.result.PickVisualMediaRequest(
    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
)

@Composable
private fun greeting(name: String): String {
    val context = LocalContext.current
    val base = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> context.getString(R.string.home_greeting_morning)
        in 12..17 -> context.getString(R.string.home_greeting_afternoon)
        else -> context.getString(R.string.home_greeting_evening)
    }
    return if (name.isBlank()) base else "$base, $name"
}
