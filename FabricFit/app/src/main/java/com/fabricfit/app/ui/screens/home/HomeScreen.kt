package com.fabricfit.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.permissions.rememberCameraPermission
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.GarmentArt
import com.fabricfit.app.ui.components.LocalImage
import com.fabricfit.app.ui.components.SectionHeader
import com.fabricfit.app.ui.components.heritagePrice
import com.fabricfit.app.ui.components.heritageRating
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCreamBright
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.openFabricFlow
import com.fabricfit.app.ui.util.rememberPhotoPicker
import com.fabricfit.app.ui.util.resolveKey
import com.fabricfit.app.ui.util.timeAgo
import kotlinx.coroutines.launch

private data class FeaturedFabricTile(
    val title: String,
    val asset: String,
)

private val featuredFabricTiles = listOf(
    FeaturedFabricTile("Kente", "catalog/heritage/swatch_kente.jpg"),
    FeaturedFabricTile("Ankara", "catalog/heritage/swatch_ankara.jpg"),
    FeaturedFabricTile("Lace", "catalog/heritage/swatch_lace.jpg"),
    FeaturedFabricTile("Batik", "catalog/heritage/swatch_batik.jpg"),
    FeaturedFabricTile("Adinkra", "catalog/fabrics/adinkra.jpg"),
)

@Composable
fun HomeScreen(nav: NavHostController, padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var homeSearchQuery by remember { mutableStateOf("") }

    val requestCamera = rememberCameraPermission { nav.navigate(Routes.CAMERA_FABRIC) }
    val picker = rememberPhotoPicker { uri ->

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // 1. Search bar + Editorial Hero + 3 Quick Action cards (Heritage Luxe Home Dashboard)
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp),
                ) {
                    // Rounded search pill with live query input + handoff to Discover
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = HeritageCreamBright,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = HeritageForest,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        nav.navigate(Routes.discover(homeSearchQuery))
                                    },
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = homeSearchQuery,
                                onValueChange = { homeSearchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(HeritageForest),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        nav.navigate(Routes.discover(homeSearchQuery))
                                    },
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (homeSearchQuery.isEmpty()) {
                                            Text(
                                                "Search fabrics, styles or keywords...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                            if (homeSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { homeSearchQuery = "" },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { nav.navigate(Routes.discover(homeSearchQuery)) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.discover_search_hint),
                                        tint = HeritageForest,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Editorial Hero Banner: "Ghanaian Fabrics · Timeless Style"
                    Card(
                        onClick = { nav.navigate(Routes.DISCOVER) },
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(216.dp),
                    ) {
                        AssetImage(
                            asset = "catalog/heritage/hero_banner_card.jpg",
                            contentDescription = "Ghanaian Fabrics Timeless Style",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Three equal Quick-Action cards: Scan Fabric · AI Recommendations · Virtual Try-On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.CenterFocusStrong,
                            label = "Scan Fabric",
                            onClick = requestCamera,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionTile(
                            icon = Icons.Filled.AutoAwesome,
                            label = "AI Recommendations",
                            onClick = { nav.navigate(Routes.DISCOVER) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Checkroom,
                            label = "Virtual Try-On",
                            onClick = { nav.navigate(Routes.tryOn(null, null)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 2. Featured Fabrics rail (Kente · Ankara · Lace · Batik)
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 22.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Featured Fabrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = HeritageForest,
                        modifier = Modifier.clickable { nav.navigate(Routes.DISCOVER) },
                    )
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(featuredFabricTiles, key = { it.title }) { tile ->
                        Column(
                            modifier = Modifier
                                .width(92.dp)
                                .clickable { nav.navigate(Routes.discover(tile.title)) },
                        ) {
                            AssetImage(
                                asset = tile.asset,
                                contentDescription = tile.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                tile.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }

            // 3. Recommended Outfit Styles
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
                                modifier = Modifier.width(156.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    GarmentArt(
                                        style,
                                        null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(152.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        context.resolveKey(style.titleKey) ?: style.id,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            style.heritagePrice(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = HeritageGold,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            style.heritageRating(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
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

            // Secondary actions: Upload from gallery + Saved looks
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp),
                ) {
                    OutlinedButton(
                        onClick = { picker.launch(pickImagesRequest()) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.home_cta_upload))
                    }
                    Spacer(Modifier.height(10.dp))
                    Card(
                        onClick = { nav.navigate(Routes.FAVORITES) },
                        modifier = Modifier.fillMaxWidth(),
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
            }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = HeritageCreamBright,
        border = BorderStroke(1.dp, Color(0xFFE4DAC6)),
        shadowElevation = 1.dp,
        modifier = modifier.height(92.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HeritageForest,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
private fun pickImagesRequest() = androidx.activity.result.PickVisualMediaRequest(
    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
)