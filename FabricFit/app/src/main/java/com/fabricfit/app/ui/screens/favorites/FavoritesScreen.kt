package com.fabricfit.app.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.ui.util.rememberHapticFeedback
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.ui.components.AnimatedBottomSheet
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.BottomSheetAction
import com.fabricfit.app.ui.components.BottomSheetDivider
import com.fabricfit.app.ui.components.EnhancedEmptyState
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.FirstRunTooltip
import com.fabricfit.app.ui.components.GarmentArt
import com.fabricfit.app.ui.components.LocalImage
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.components.ensureSampleKenteFabric
import com.fabricfit.app.ui.components.heritagePrice
import com.fabricfit.app.ui.components.heritageRating
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.onboarding.ProfileAvatarImage
import com.fabricfit.app.ui.theme.HeritageCreamBright
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.resolveKey
import com.fabricfit.app.ui.util.shareImagePath
import com.fabricfit.app.ui.util.timeAgo
import kotlinx.coroutines.launch

/**
 * Sort options for favorites list.
 */
enum class SortOption(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    NEWEST(
        R.string.sort_newest,
        Icons.Filled.Star,
    ),
    OLDEST(
        R.string.sort_oldest,
        Icons.Filled.Star,
    ),
    NAME_A_Z(
        R.string.sort_name_az,
        Icons.Filled.Star,
    ),
    NAME_Z_A(
        R.string.sort_name_za,
        Icons.Filled.Star,
    ),
}

private data class RecommendedDressItem(
    val id: String,
    val title: String,
    val price: String,
    val rating: String,
    val reviews: Int,
    val asset: String,
    val categoryTag: String = "Kente",
)

private val recommendedDresses = listOf(
    RecommendedDressItem("african_gown", "Elegant Peplum Dress", "$89.00", "4.8", 124, "catalog/heritage/dress_kente_flare.jpg", "Kente"),
    RecommendedDressItem("straight_dress", "Royal Kente Gown", "$89.00", "4.8", 122, "catalog/heritage/dress_kente_aline.jpg", "Kente"),
    RecommendedDressItem("peplum", "Asymmetric Peplum", "$59.00", "4.8", 79, "catalog/heritage/dress_kente_asymmetric.jpg", "Ankara"),
    RecommendedDressItem("kaba_slit", "Modern Kente Dress", "$78.00", "4.7", 98, "catalog/heritage/outfit_modern_kente.jpg", "Kente"),
)

@Composable
fun FavoritesScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: FavoritesViewModel = appViewModel { c, ctx -> FavoritesViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var detailTab by remember { mutableStateOf(0) }
    val tooltips = AppContainer.get(context).tooltips
    val tooltipSeen by tooltips.seen(TooltipStore.FAVORITES).collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val haptics = rememberHapticFeedback()

    // Sort & Filter bottom sheet state
    var showFilterSheet by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(SortOption.NEWEST) }
    var filterCategory by remember { mutableStateOf<String?>(null) }

    val filteredRecommendedDresses = remember(sortBy, filterCategory) {
        val base = if (filterCategory == null) {
            recommendedDresses
        } else {
            recommendedDresses.filter {
                it.categoryTag.equals(filterCategory, ignoreCase = true) ||
                    it.title.contains(filterCategory!!, ignoreCase = true)
            }.ifEmpty { recommendedDresses }
        }
        when (sortBy) {
            SortOption.NEWEST -> base
            SortOption.OLDEST -> base.reversed()
            SortOption.NAME_A_Z -> base.sortedBy { it.title.lowercase() }
            SortOption.NAME_Z_A -> base.sortedByDescending { it.title.lowercase() }
        }
    }

    val filteredFabrics = remember(state.fabrics, sortBy, filterCategory, context) {
        val base = state.fabrics.filter { fabric ->
            filterCategory == null ||
                fabric.category.name.contains(filterCategory!!, ignoreCase = true) ||
                context.getString(fabric.category.labelRes).contains(filterCategory!!, ignoreCase = true) ||
                (fabric.name?.contains(filterCategory!!, ignoreCase = true) == true)
        }
        when (sortBy) {
            SortOption.NEWEST -> base.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> base.sortedBy { it.createdAt }
            SortOption.NAME_A_Z -> base.sortedBy { (it.name ?: context.getString(it.category.labelRes)).lowercase() }
            SortOption.NAME_Z_A -> base.sortedByDescending { (it.name ?: context.getString(it.category.labelRes)).lowercase() }
        }
    }

    val filteredDesigns = remember(state.designs, sortBy, filterCategory, context) {
        val base = state.designs.filter { style ->
            filterCategory == null ||
                style.affinity.any {
                    it.name.contains(filterCategory!!, ignoreCase = true) ||
                        context.getString(it.labelRes).contains(filterCategory!!, ignoreCase = true)
                } ||
                (context.resolveKey(style.titleKey) ?: style.id).contains(filterCategory!!, ignoreCase = true)
        }
        when (sortBy) {
            SortOption.NEWEST -> base
            SortOption.OLDEST -> base.reversed()
            SortOption.NAME_A_Z -> base.sortedBy { (context.resolveKey(it.titleKey) ?: it.id).lowercase() }
            SortOption.NAME_Z_A -> base.sortedByDescending { (context.resolveKey(it.titleKey) ?: it.id).lowercase() }
        }
    }

    val filteredLooks = remember(state.looks, sortBy) {
        when (sortBy) {
            SortOption.NEWEST -> state.looks.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> state.looks.sortedBy { it.createdAt }
            SortOption.NAME_A_Z -> state.looks.sortedBy { it.dressStyleId.lowercase() }
            SortOption.NAME_Z_A -> state.looks.sortedByDescending { it.dressStyleId.lowercase() }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            vm.refresh { isRefreshing = false }
        },
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Top bar: < Scan Results + profile avatar (Reference Image 4)
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { nav.navigate(Routes.HOME) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        "Scan Results",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    // Filter button
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.action_filter),
                            tint = if (filterCategory != null || sortBy != SortOption.NEWEST) HeritageGold else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Surface(
                        onClick = { nav.navigate(Routes.PROFILE) },
                        shape = CircleShape,
                        color = Color(0xFFE5D5B5),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = HeritageForestDark,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            // Main Scan Result Card: dynamic latest scan or Kente Royalty · 98% Match + 4 Palette Dots + Colors/Pattern/Style tabs
            item {
                val activeFabric = state.fabrics.firstOrNull()
                val bannerTitle = activeFabric?.let {
                    it.name?.takeIf { n -> n.isNotBlank() } ?: context.getString(it.category.labelRes)
                } ?: "Kente Royalty"
                val matchPercent = activeFabric?.let {
                    (it.confidence * 100).toInt().coerceIn(78, 99)
                } ?: 98
                val paletteSwatches = activeFabric?.colors?.take(4)?.map { Color(it.argb) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf(
                        Color(0xFF144B2F),
                        Color(0xFF611425),
                        Color(0xFFD99B26),
                        Color(0xFFCC292B),
                    )

                Card(
                    onClick = {
                        scope.launch {
                            val id = activeFabric?.id ?: ensureSampleKenteFabric(context)
                            nav.navigate(Routes.fabric(id))
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeFabric != null) {
                                FabricSwatch(
                                    activeFabric,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                )
                            } else {
                                AssetImage(
                                    asset = "catalog/heritage/swatch_kente.jpg",
                                    contentDescription = bannerTitle,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    bannerTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF216849),
                                ) {
                                    Text(
                                        "$matchPercent% Match",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Extracted Colour Palette Circles + Quick Action Pills (Try-On / Colour Match)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                paletteSwatches.forEach { swatchColor ->
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor),
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val id = activeFabric?.id ?: ensureSampleKenteFabric(context)
                                            nav.navigate(Routes.colorMatch(id))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text("Colour Match", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val id = activeFabric?.id ?: ensureSampleKenteFabric(context)
                                            nav.navigate(Routes.tryOn(id, null))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HeritageForest,
                                        contentColor = Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text("Try-On", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Segmented Pill Bar: Colors · Pattern · Style
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF3EFE6),
                            border = BorderStroke(1.dp, Color(0xFFE2DAC8)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                listOf("Colors", "Pattern", "Style").forEachIndexed { index, label ->
                                    val selected = detailTab == index
                                    Surface(
                                        onClick = { detailTab = index },
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) Color.White else Color.Transparent,
                                        shadowElevation = if (selected) 1.dp else 0.dp,
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (index == 0) {
                                                Box(
                                                    Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(HeritageForest),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (detailTab) {
                                0 -> if (activeFabric != null) {
                                    "Palette: " + activeFabric.colors.take(4).joinToString(" · ") {
                                        context.getString(it.name.labelRes)
                                    }
                                } else {
                                    "Palette: Forest Green · Deep Burgundy · Royal Gold · Kente Red"
                                }
                                1 -> if (activeFabric != null) {
                                    "Pattern: ${context.getString(activeFabric.pattern.labelRes)} · ${context.getString(activeFabric.texture.labelRes)}"
                                } else {
                                    "Pattern: Hand-woven Ashanti strip geometry · High contrast"
                                }
                                else -> "Best matches: Kaba & Slit · Elegant Peplum · Royal Gown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Recommended Dresses Section Header
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Recommended Dresses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "See All",
                        style = MaterialTheme.typography.titleSmall,
                        color = HeritageForest,
                        modifier = Modifier.clickable { nav.navigate(Routes.DISCOVER) },
                    )
                }
            }

            // Recommended Dresses Horizontal Carousel
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredRecommendedDresses, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { nav.navigate(Routes.design(item.id)) },
                        ) {
                            AssetImage(
                                asset = item.asset,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(142.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.price,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = HeritageGold,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${item.rating} (${item.reviews})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Wide Gold-on-Forest CTA: View All Recommendations
            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { nav.navigate(Routes.DISCOVER) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeritageForestDark,
                        contentColor = HeritageGold,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                ) {
                    Text(
                        "View All Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // User-saved fabrics if any have been added
            if (state.isLoading) {
                item {
                    Text(
                        stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                item {
                    ShimmerGrid(
                        columns = 1,
                        rows = 3,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                if (filteredFabrics.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.favorites_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }
                    items(filteredFabrics, key = { "fabric_${it.id}" }) { fabric ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    haptics.heavy()
                                    vm.unfavoriteFabric(fabric.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.action_remove),
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.action_remove),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            },
                            content = {
                                Card(
                                    onClick = { nav.navigate(Routes.fabric(fabric.id)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FabricSwatch(fabric, modifier = Modifier.size(64.dp))
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                fabric.name ?: context.getString(fabric.category.labelRes),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                context.timeAgo(fabric.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = {
                                            context.shareImagePath(
                                                fabric.imageUri,
                                                context.getString(
                                                    R.string.share_fabric_text,
                                                    fabric.name ?: context.getString(fabric.category.labelRes),
                                                    context.getString(fabric.pattern.labelRes),
                                                ),
                                            )
                                        }) {
                                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_share_item))
                                        }
                                        IconButton(onClick = { vm.unfavoriteFabric(fabric.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                if (filteredDesigns.isNotEmpty()) {
                    item {
                        Text(
                            "Saved Styles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }
                    items(filteredDesigns, key = { "design_${it.id}" }) { style ->
                        val styleTitle = context.resolveKey(style.titleKey) ?: style.id
                        Card(
                            onClick = { nav.navigate(Routes.design(style.id)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                GarmentArt(style, null, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        styleTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            style.heritagePrice(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = HeritageGold,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            style.heritageRating(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { vm.unfavoriteDesign(style.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
                                }
                            }
                        }
                    }
                }

                if (filteredLooks.isNotEmpty()) {
                    item {
                        Text(
                            "Saved Try-On Looks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }
                    items(filteredLooks, key = { "look_${it.id}" }) { look ->
                        Card(
                            onClick = { nav.navigate(Routes.result(look.id)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                LocalImage(
                                    look.resultImageUri,
                                    contentDescription = stringResource(R.string.cd_generated_image),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.result_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        context.timeAgo(look.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    context.shareImagePath(
                                        look.resultImageUri,
                                        context.getString(R.string.result_title),
                                    )
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_share_item))
                                }
                                IconButton(onClick = { vm.unfavoriteLook(look.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!tooltipSeen) {
            FirstRunTooltip(
                text = R.string.tooltip_favorites,
                onDismiss = { scope.launch { tooltips.markSeen(TooltipStore.FAVORITES) } },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
    
    // Sort & Filter Bottom Sheet
    AnimatedBottomSheet(
        visible = showFilterSheet,
        onDismiss = { showFilterSheet = false },
        title = stringResource(R.string.filter_title),
    ) {
        // Sort options
        Text(
            text = stringResource(R.string.filter_sort_by),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        
        SortOption.values().forEach { option ->
            BottomSheetAction(
                icon = {
                    Icon(
                        option.icon,
                        contentDescription = null,
                        tint = if (sortBy == option) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = stringResource(option.labelRes),
                onClick = {
                    sortBy = option
                    showFilterSheet = false
                },
            )
            Spacer(Modifier.height(4.dp))
        }
        
        BottomSheetDivider()
        
        // Category filters
        Text(
            text = stringResource(R.string.filter_category),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        
        BottomSheetAction(
            icon = {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (filterCategory == null) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            label = stringResource(R.string.filter_all_categories),
            onClick = {
                filterCategory = null
                showFilterSheet = false
            },
        )
        Spacer(Modifier.height(4.dp))
        
        listOf("Kente", "Ankara", "Lace", "Chiffon").forEach { category ->
            BottomSheetAction(
                icon = {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (filterCategory == category) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = category,
                onClick = {
                    filterCategory = category
                    showFilterSheet = false
                },
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
