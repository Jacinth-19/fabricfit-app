package com.fabricfit.app.ui.screens.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.StyleCategory
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.CardSkeleton
import com.fabricfit.app.ui.components.EnhancedEmptyState
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.GarmentArt
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.components.heritagePrice
import com.fabricfit.app.ui.components.heritageRating
import com.fabricfit.app.ui.components.heritageReviews
import com.fabricfit.app.ui.components.heritageTitle
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCreamBright
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.resolveKey

private enum class DiscoverFilter(val label: String) {
    ALL("All"),
    CASUAL("Casual"),
    FORMAL("Formal"),
    OCCASION("Occasion"),
    FABRICS("Fabrics"),
}

private enum class DiscoverSort(val label: String) {
    FEATURED("Best Match"),
    TRENDING("Trending"),
    POPULAR("Popular"),
    NEWEST("Newest"),
}

private enum class GenderFilter(val label: String) {
    ALL("All"),
    WOMEN("Women"),
    MEN("Men"),
}

@Composable
fun DiscoverScreen(nav: NavHostController, padding: PaddingValues, initialQuery: String = "") {
    val vm: DiscoverViewModel = appViewModel { c, ctx -> DiscoverViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by remember { mutableStateOf(DiscoverFilter.ALL) }
    var genderFilter by remember { mutableStateOf(GenderFilter.ALL) }
    var sortMode by remember { mutableStateOf(DiscoverSort.FEATURED) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(initialQuery) }
    var searchOpen by remember { mutableStateOf(initialQuery.isNotBlank()) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            searchOpen = true
            vm.setQuery(initialQuery)
        }
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        // Editorial Serif Header matching Reference Image 5 ("Outfit Recommendations")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Outfit Recommendations",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
                color = HeritageForest,
            )
            IconButton(onClick = { searchOpen = !searchOpen }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.discover_search_hint),
                    tint = HeritageForest,
                )
            }
        }

        // Filter pills container card: All · Casual · Formal · Occasion · Fabrics
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = HeritageCreamBright,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DiscoverFilter.values().forEach { item ->
                        val selected = filter == item
                        Surface(
                            onClick = { filter = item },
                            shape = RoundedCornerShape(50),
                            color = if (selected) HeritageForest else Color(0xFFEDE7DA),
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            )
                        }
                    }
                }

                if (searchOpen) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; vm.setQuery(it) },
                        placeholder = { Text(stringResource(R.string.discover_search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = HeritageForest) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = ""; vm.setQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = HeritageForest,
                            unfocusedBorderColor = Color(0xFFE0D6C3),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                    )
                }
            }
        }

        // Secondary row: Gender filter chips + Sort selector
        if (filter != DiscoverFilter.FABRICS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GenderFilter.entries.forEach { g ->
                        FilterChip(
                            selected = genderFilter == g,
                            onClick = { genderFilter = g },
                            label = { Text(g.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Box {
                    Surface(
                        onClick = { sortMenuOpen = true },
                        shape = RoundedCornerShape(50),
                        color = HeritageCreamBright,
                        border = BorderStroke(1.dp, Color(0xFFE0D6C3)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.SwapVert,
                                contentDescription = null,
                                tint = HeritageForest,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                sortMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = HeritageForest,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                    ) {
                        DiscoverSort.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    sortMode = mode
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }

        val q = query.trim()
        if (filter == DiscoverFilter.FABRICS) {
            val cats = FabricCategory.values().filter {
                q.isEmpty() || context.getString(it.labelRes).contains(q, ignoreCase = true)
            }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(cats.toList()) { cat ->
                    Card(
                        onClick = {
                            query = cat.name.lowercase()
                            vm.setQuery(query)
                            filter = DiscoverFilter.ALL
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val catAsset = cat.asset
                            if (catAsset != null) {
                                AssetImage(
                                    asset = catAsset,
                                    contentDescription = stringResource(cat.labelRes),
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                            } else {
                                FabricSwatch(representativeFabric(cat), modifier = Modifier.width(64.dp).height(64.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(stringResource(cat.labelRes), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(cat.descriptionRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (!state.loaded) {
                ShimmerGrid(
                    columns = 2,
                    rows = 4,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                val baseStyles = smartFilterStyles(state.styles, q, context).filter { s ->
                    when (genderFilter) {
                        GenderFilter.ALL -> true
                        GenderFilter.WOMEN -> s.gender == GenderCategory.WOMEN || s.gender == GenderCategory.UNISEX
                        GenderFilter.MEN -> s.gender == GenderCategory.MEN || s.gender == GenderCategory.UNISEX
                    }
                }
                val categoryFiltered = when (filter) {
                    DiscoverFilter.ALL -> orderFeaturedFirst(baseStyles)
                    DiscoverFilter.CASUAL -> baseStyles.filter { it.category == StyleCategory.CASUAL }
                    DiscoverFilter.FORMAL -> baseStyles.filter { it.category == StyleCategory.FORMAL }
                    DiscoverFilter.OCCASION -> baseStyles.filter { it.category == StyleCategory.TRADITIONAL }
                    DiscoverFilter.FABRICS -> baseStyles
                }
                val styles = when (sortMode) {
                    DiscoverSort.FEATURED -> categoryFiltered
                    DiscoverSort.TRENDING -> categoryFiltered.sortedByDescending { it.heritageRating() }
                    DiscoverSort.POPULAR -> categoryFiltered.sortedByDescending { it.heritageReviews() }
                    DiscoverSort.NEWEST -> categoryFiltered.reversed()
                }
                if (styles.isEmpty()) {
                    EnhancedEmptyState(
                        illustration = R.drawable.illust_empty_collection,
                        title = R.string.discover_empty_search,
                        body = R.string.discover_empty_search_body,
                        actionLabel = R.string.empty_action_clear_filters,
                        onAction = {
                            query = ""
                            genderFilter = GenderFilter.ALL
                            filter = DiscoverFilter.ALL
                            vm.setQuery("")
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(styles, key = { it.id }) { style ->
                            val isFav = style.id in state.favoriteStyleIds
                            Card(
                                onClick = { nav.navigate(Routes.design(style.id)) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = HeritageCreamBright),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Box {
                                        GarmentArt(
                                            style,
                                            null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(166.dp),
                                        )
                                        Surface(
                                            onClick = { vm.toggleFavoriteStyle(style.id) },
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.88f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                    contentDescription = "Save Style",
                                                    tint = if (isFav) HeritageGold else HeritageForest,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        displayTitleFor(style, context),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            style.heritagePrice(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = HeritageGold,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            style.heritageRating(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun orderFeaturedFirst(
    styles: List<com.fabricfit.app.domain.model.DressStyle>,
): List<com.fabricfit.app.domain.model.DressStyle> {
    val priority = listOf("kaba_slit", "modern_kaba", "maxi_dress", "co_ords", "peplum", "african_gown")
    val byId = styles.associateBy { it.id }
    val featured = priority.mapNotNull { byId[it] }
    val rest = styles.filter { it.id !in priority }
    return featured + rest
}

private fun displayTitleFor(
    style: com.fabricfit.app.domain.model.DressStyle,
    context: android.content.Context,
): String = style.heritageTitle(context)

/** Builds a display-only swatch for the fabric library without pretending to be a scan. */
private fun representativeFabric(cat: FabricCategory): com.fabricfit.app.domain.model.Fabric {
    val palette = when (cat) {
        FabricCategory.KENTE -> listOf(0xFFC8952B, 0xFF8C2F39, 0xFF1F5450, 0xFF14110F)
        FabricCategory.ADINKRA -> listOf(0xFF4A2C17, 0xFF14110F, 0xFFB0803F)
        FabricCategory.WAX, FabricCategory.ANKARA -> listOf(0xFF2F7D4F, 0xFFC8952B, 0xFF8C2F39, 0xFFFDF8F2)
        FabricCategory.BATIK -> listOf(0xFF31556E, 0xFF7FA3BC, 0xFF1D3446)
        FabricCategory.TIEDYE -> listOf(0xFF274B8F, 0xFF8FB0E8, 0xFFFDF8F2)
        FabricCategory.LACE -> listOf(0xFFE8DED2, 0xFFC9B7A4, 0xFFFDF8F2)
        FabricCategory.BROCADE -> listOf(0xFF2E6E6A, 0xFFC8952B)
        FabricCategory.DENIM -> listOf(0xFF2B3A5C, 0xFF3E5379)
        FabricCategory.LINEN -> listOf(0xFFD9CDBB, 0xFFC9BCA7)
        FabricCategory.SILK -> listOf(0xFFB02E43, 0xFFD06A7B)
        FabricCategory.CHIFFON -> listOf(0xFFE7C8D2, 0xFFF6E7EC)
        FabricCategory.VELVET -> listOf(0xFF4A0E1C, 0xFF6E1B2C)
        FabricCategory.COTTON_PLAIN -> listOf(0xFFF3EFE7)
        FabricCategory.FUGU -> listOf(0xFF3A4250, 0xFF14110F, 0xFFF2EFE9)
        FabricCategory.GONJA -> listOf(0xFFB8763A, 0xFFF2EFE9, 0xFF14110F)
        FabricCategory.KENTE_PRINT -> listOf(0xFFC8952B, 0xFF1F5450, 0xFF8C2F39)
        FabricCategory.WOOL -> listOf(0xFF8B7355, 0xFFA0826D, 0xFFD4C4B7)
        FabricCategory.UNKNOWN -> listOf(0xFF9A8F86)
    }
    return com.fabricfit.app.domain.model.Fabric(
        imageUri = "",
        name = null,
        category = cat,
        colors = palette.map { com.fabricfit.app.domain.model.AnalyzedColor(it.toLong() or 0xFF000000L, com.fabricfit.app.domain.model.ColorName.GOLD, 0.25f) },
        pattern = when (cat) {
            FabricCategory.KENTE, FabricCategory.KENTE_PRINT,
            FabricCategory.FUGU, FabricCategory.GONJA -> com.fabricfit.app.domain.model.PatternType.STRIPED
            FabricCategory.ADINKRA -> com.fabricfit.app.domain.model.PatternType.SYMBOLIC
            FabricCategory.BATIK, FabricCategory.TIEDYE -> com.fabricfit.app.domain.model.PatternType.ORGANIC
            FabricCategory.COTTON_PLAIN, FabricCategory.LINEN, FabricCategory.SILK, FabricCategory.VELVET, FabricCategory.CHIFFON -> com.fabricfit.app.domain.model.PatternType.SOLID
            else -> com.fabricfit.app.domain.model.PatternType.GEOMETRIC
        },
        texture = com.fabricfit.app.domain.model.TextureType.WOVEN,
        confidence = 1f,
        createdAt = 0,
    )
}

/**
 * Natural-language aware filtering: "kente for a wedding" narrows by fabric affinity and
 * occasion instead of demanding an exact title match. Falls back to plain substring search
 * when nothing in the query maps to a known filter.
 */
private fun smartFilterStyles(
    styles: List<com.fabricfit.app.domain.model.DressStyle>,
    q: String,
    context: android.content.Context,
): List<com.fabricfit.app.domain.model.DressStyle> {
    if (q.isEmpty()) return styles
    val intent = com.fabricfit.app.domain.engine.SearchIntentParser.parse(q)
    val hasStructured = intent.occasions.isNotEmpty() || intent.gender != null ||
        intent.category != null || intent.fabricKeywords.isNotEmpty()
    if (!hasStructured) {
        return styles.filter {
            it.heritageTitle(context).contains(q, ignoreCase = true) ||
                context.resolveKey(it.titleKey).orEmpty().contains(q, ignoreCase = true) ||
                context.resolveKey(it.descriptionKey).orEmpty().contains(q, ignoreCase = true)
        }
    }
    val fabricCats = intent.fabricKeywords.mapNotNull { kw ->
        com.fabricfit.app.domain.model.FabricCategory.values()
            .firstOrNull { it.name.lowercase().startsWith(kw) }
    }.toSet()
    return styles.filter { style ->
        (intent.gender == null || style.gender == intent.gender ||
            style.gender == com.fabricfit.app.domain.model.GenderCategory.UNISEX) &&
            (intent.occasions.isEmpty() || style.occasions.intersect(intent.occasions).isNotEmpty()) &&
            (intent.category == null || style.category == intent.category) &&
            (fabricCats.isEmpty() || style.affinity.intersect(fabricCats).isNotEmpty()) &&
            (intent.remainder.isBlank() ||
                style.heritageTitle(context).contains(intent.remainder, ignoreCase = true) ||
                context.resolveKey(style.titleKey).orEmpty().contains(intent.remainder, ignoreCase = true) ||
                context.resolveKey(style.descriptionKey).orEmpty().contains(intent.remainder, ignoreCase = true))
    }
}
