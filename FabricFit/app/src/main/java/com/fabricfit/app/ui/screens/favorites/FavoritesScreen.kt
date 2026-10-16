package com.fabricfit.app.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.FirstRunTooltip
import com.fabricfit.app.ui.components.ensureSampleKenteFabric
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCreamBright
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.shareImagePath
import com.fabricfit.app.ui.util.timeAgo
import kotlinx.coroutines.launch

private data class RecommendedDressItem(
    val id: String,
    val title: String,
    val price: String,
    val rating: String,
    val reviews: Int,
    val asset: String,
)

private val recommendedDresses = listOf(
    RecommendedDressItem("african_gown", "Elegant Peplum Dress", "$89.00", "4.8", 124, "catalog/heritage/dress_kente_flare.jpg"),
    RecommendedDressItem("straight_dress", "Royal Kente Gown", "$89.00", "4.8", 122, "catalog/heritage/dress_kente_aline.jpg"),
    RecommendedDressItem("peplum", "Asymmetric Peplum", "$59.00", "4.8", 79, "catalog/heritage/dress_kente_asymmetric.jpg"),
    RecommendedDressItem("kaba_slit", "Modern Kente Dress", "$78.00", "4.7", 98, "catalog/heritage/outfit_modern_kente.jpg"),
)

@Composable
fun FavoritesScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: FavoritesViewModel = appViewModel { c, ctx -> FavoritesViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var detailTab by remember { mutableIntStateOf(0) }
    val tooltips = AppContainer.get(context).tooltips
    val tooltipSeen by tooltips.seen(TooltipStore.FAVORITES).collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    fun launchTryOnWithStyle(styleId: String?) {
        scope.launch {
            val fabricId = state.fabrics.firstOrNull()?.id ?: ensureSampleKenteFabric(context)
            nav.navigate(Routes.tryOn(fabricId, styleId ?: "african_gown"))
        }
    }

    fun openColorMatchForScan() {
        scope.launch {
            val fabricId = state.fabrics.firstOrNull()?.id ?: ensureSampleKenteFabric(context)
            nav.navigate(Routes.colorMatch(fabricId))
        }
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
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

            // Main Scan Result Card: Kente Royalty · 98% Match + 4 Palette Dots + Colors/Pattern/Style tabs
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                asset = "catalog/heritage/swatch_kente.jpg",
                                contentDescription = "Kente Royalty",
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Kente Royalty",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF216849),
                                    ) {
                                        Text(
                                            "98% Match",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        )
                                    }
                                    Surface(
                                        onClick = { launchTryOnWithStyle("african_gown") },
                                        shape = RoundedCornerShape(50),
                                        color = HeritageForestDark,
                                        border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.7f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Filled.Checkroom,
                                                contentDescription = null,
                                                tint = HeritageGold,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Try-On",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = HeritageGold,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Dynamic Tab Content (Colors · Pattern · Style)
                        when (detailTab) {
                            0 -> {
                                // Extracted Colour Palette Circles — tapping opens ColorMatchScreen
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    listOf(
                                        Color(0xFF144B2F),
                                        Color(0xFF611425),
                                        Color(0xFFD99B26),
                                        Color(0xFFCC292B),
                                    ).forEach { swatchColor ->
                                        Box(
                                            Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .clickable { openColorMatchForScan() },
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        onClick = { openColorMatchForScan() },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFF3EFE6),
                                        border = BorderStroke(1.dp, Color(0xFFE2DAC8)),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Filled.Palette,
                                                contentDescription = null,
                                                tint = HeritageForest,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                stringResource(R.string.tool_color_ideas),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = HeritageForest,
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                Column {
                                    Text(
                                        "Striped Hand-Loom Weave · High Contrast",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HeritageForest,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Directional warp-and-weft repeat · Structured weight suited for sculpted bodices & flared skirts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            else -> {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Ceremony · Wedding · Festival",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HeritageForest,
                                        )
                                        Text(
                                            "Best silhouettes: Peplum, A-Line Gown, Kaba & Slit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { launchTryOnWithStyle("african_gown") },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HeritageForest,
                                            contentColor = Color.White,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("Launch Try-On", style = MaterialTheme.typography.labelMedium)
                                    }
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

            // Recommended Dresses Horizontal Carousel with 1-tap Virtual Try-On badge
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recommendedDresses, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { nav.navigate(Routes.design(item.id)) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(142.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            ) {
                                AssetImage(
                                    asset = item.asset,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Surface(
                                    onClick = { launchTryOnWithStyle(item.id) },
                                    shape = CircleShape,
                                    color = HeritageForestDark.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Checkroom,
                                            contentDescription = stringResource(R.string.tryon_title),
                                            tint = HeritageGold,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }
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
            if (state.fabrics.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                items(state.fabrics, key = { it.id }) { fabric ->
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
            }
        }

        if (!tooltipSeen) {