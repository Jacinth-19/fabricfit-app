package com.ntoma.studio.ui.screens.home

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.ui.components.AssetImage
import com.ntoma.studio.ui.components.FabricSwatch
import com.ntoma.studio.ui.theme.KenteGold
import com.ntoma.studio.ui.components.GarmentArt
import com.ntoma.studio.ui.components.LocalImage
import com.ntoma.studio.ui.components.SectionHeader
import com.ntoma.studio.ui.navigation.Routes
import com.ntoma.studio.ui.navigation.appViewModel
import com.ntoma.studio.ui.util.openFabricFlow
import com.ntoma.studio.ui.util.rememberPhotoPicker
import com.ntoma.studio.ui.util.timeAgo
import com.ntoma.studio.permissions.rememberCameraPermission
import kotlinx.coroutines.launch
import java.util.Calendar
import com.ntoma.studio.ui.util.resolveKey

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
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)) {
                    Text(
                        greeting(state.prefs.displayName),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Spacer(Modifier.height(14.dp))
                    // Search — routes into Discover's intent parser.
                    Surface(
                        onClick = { nav.navigate(Routes.DISCOVER) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.discover_search_hint),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.discover_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    // Heritage hero: catalog kente photo, serif headline, gold CTA.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(MaterialTheme.shapes.large),
                    ) {
                        AssetImage(
                            asset = "catalog/fabrics/kente.jpg",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                    ),
                                ),
                        )
                        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(
                                stringResource(R.string.home_hero_line1),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White,
                            )
                            Text(
                                stringResource(R.string.home_hero_line2),
                                style = MaterialTheme.typography.displayMedium,
                                color = KenteGold,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = requestCamera,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KenteGold,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.home_hero_cta))
                                }
                                OutlinedButton(onClick = { picker.launch(pickImagesRequest()) }) {
                                    Text(stringResource(R.string.home_cta_upload))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Three action tiles from the brand board.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeTile(
                            label = R.string.home_tile_scan,
                            icon = Icons.Outlined.CenterFocusWeak,
                            modifier = Modifier.weight(1f),
                            onClick = requestCamera,
                        )
                        HomeTile(
                            label = R.string.home_tile_recs,
                            icon = Icons.Outlined.AutoAwesome,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val latest = state.recentFabrics.firstOrNull()
                                if (latest != null) {
                                    nav.navigate(Routes.recommendations(latest.id))
                                } else {
                                    nav.navigate(Routes.DISCOVER)
                                }
                            },
                        )
                        HomeTile(
                            label = R.string.home_tile_tryon,
                            icon = Icons.Outlined.Checkroom,
                            modifier = Modifier.weight(1f),
                            onClick = { nav.navigate(Routes.TRYON_HUB) },
                        )
                    }
                    if (!state.prefs.premiumEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(
                                R.string.home_quota_note,
                                state.analysesLeft.coerceAtMost(3),
                                3,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    R.string.home_featured,
                    actionLabel = R.string.action_see_all,
                    onAction = { nav.navigate(Routes.DISCOVER) },
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(featuredCategories, key = { it.name }) { cat ->
                        Card(
                            onClick = { nav.navigate(Routes.DISCOVER) },
                            modifier = Modifier.width(110.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                AssetImage(
                                    asset = cat.asset.orEmpty(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(88.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    stringResource(cat.labelRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                            }
                        }
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    FabricSwatch(
                                        fabric,
                                        modifier = Modifier.fillMaxWidth().height(96.dp),
                                    )
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    GarmentArt(
                                        style,
                                        null,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LocalImage(
                                state.latestLook!!.resultImageUri,
                                contentDescription = stringResource(R.string.cd_generated_image),
                                modifier = Modifier.size(72.dp),
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
                Card(
                    onClick = { nav.navigate(Routes.FAVORITES) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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

private val featuredCategories = listOf(
    com.ntoma.studio.domain.model.FabricCategory.KENTE,
    com.ntoma.studio.domain.model.FabricCategory.ANKARA,
    com.ntoma.studio.domain.model.FabricCategory.LACE,
    com.ntoma.studio.domain.model.FabricCategory.BATIK,
)

@Composable
private fun HomeTile(
    label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TipCard(title: Int, body: Int) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
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
