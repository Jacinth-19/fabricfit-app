package com.fabricfit.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.StyleCategory
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.CardSkeleton
import com.fabricfit.app.ui.components.EmptyState
import com.fabricfit.app.ui.components.FabricSwatch
import com.fabricfit.app.ui.components.GarmentArt
import com.fabricfit.app.ui.components.heritagePrice
import com.fabricfit.app.ui.components.heritageRating
import com.fabricfit.app.ui.components.heritageReviews
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

enum class DiscoverGenderFilter(val label: String, val gender: GenderCategory?) {
    ALL("All", null),
    WOMEN("Women", GenderCategory.WOMEN),
    MEN("Men", GenderCategory.MEN),
}

enum class DiscoverSortOption(val label: String) {
    BEST_MATCH("Best Match"),
    TOP_RATED("Top Rated"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
}

@Composable
fun DiscoverScreen(
    nav: NavHostController,
    padding: PaddingValues,
    initialQuery: String = "",
) {
    val vm: DiscoverViewModel = appViewModel { c, ctx -> DiscoverViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by remember { mutableStateOf(DiscoverFilter.ALL) }
    var genderFilter by remember { mutableStateOf(DiscoverGenderFilter.ALL) }
    var sortOption by remember { mutableStateOf(DiscoverSortOption.BEST_MATCH) }
    var query by remember { mutableStateOf(initialQuery) }
    var searchOpen by remember { mutableStateOf(initialQuery.isNotBlank()) }
    var refineOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            vm.setQuery(initialQuery)
            searchOpen = true
            filter = DiscoverFilter.ALL
        }
    }

    val hasActiveRefine = genderFilter != DiscoverGenderFilter.ALL || sortOption != DiscoverSortOption.BEST_MATCH

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
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { refineOpen = !refineOpen }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = "Filter & Sort",
                            tint = HeritageForest,
                        )
                    }
                    if (hasActiveRefine) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(HeritageGold),
                        )
                    }
                }
                IconButton(onClick = { searchOpen = !searchOpen }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.discover_search_hint),
                        tint = HeritageForest,
                    )
                }
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

                if (refineOpen) {
                    Spacer(Modifier.height(8.dp))
                    // Gender Filter Row: All · Women · Men
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Gender:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DiscoverGenderFilter.entries.forEach { item ->
                            FilterChip(
                                selected = genderFilter == item,
                                onClick = { genderFilter = item },
                                label = { Text(item.label) },
                            )
                        }
                    }
                    // Sort Selector Row: Best Match · Top Rated · Price: Low to High · Price: High to Low
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Sort:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DiscoverSortOption.entries.forEach { item ->
                            FilterChip(
                                selected = sortOption == item,
                                onClick = { sortOption = item },
                                label = { Text(item.label) },
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

        val q = query.trim()
        if (filter == DiscoverFilter.FABRICS) {
            val cats = FabricCategory.values().filter {
                q.isEmpty() || context.getString(it.labelRes).contains(q, ignoreCase = true)
            }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(cats.toList()) { cat ->
                    Card(
                        onClick = {
                            query = context.getString(cat.labelRes)
                            vm.setQuery(query)
                            filter = DiscoverFilter.ALL
                            searchOpen = true
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
                Column(Modifier.padding(20.dp)) { CardSkeleton(); Spacer(Modifier.height(16.dp)); CardSkeleton() }
            } else {
                val baseStyles = smartFilterStyles(state.styles, q, context)
                    .filter { style ->
                        val g = genderFilter.gender
                        g == null || style.gender == g || style.gender == GenderCategory.UNISEX
                    }
                val categoryFiltered = when (filter) {
                    DiscoverFilter.ALL -> orderFeaturedFirst(baseStyles)
                    DiscoverFilter.CASUAL -> baseStyles.filter { it.category == StyleCategory.CASUAL }
                    DiscoverFilter.FORMAL -> baseStyles.filter { it.category == StyleCategory.FORMAL }
                    DiscoverFilter.OCCASION -> baseStyles.filter { it.category == StyleCategory.TRADITIONAL }
                    DiscoverFilter.FABRICS -> baseStyles
                }
                val styles = applySortOption(categoryFiltered, sortOption)

                if (styles.isEmpty()) {
                    EmptyState(
                        R.drawable.illust_empty_collection,
                        R.string.discover_empty_search,
                        R.string.discover_empty_search_body,
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(166.dp),
                                    ) {
                                        GarmentArt(
                                            style,
                                            null,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        if ( isFav) {
                                            Surface(
                                                onClick = { vm.toggleFavoriteStyle(style.id) },
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(28.dp),
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.Favorite,
                                                        contentDescription = stringResource(R.string.cd_favorite_on),
                                                        tint = HeritageForest,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            displayTitleFor(style, context),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(
                                            onClick = { vm.toggleFavoriteStyle(style.id) },
                                            modifier = Modifier.size(22.dp),
                                        ) {
                                            Icon(
                                                if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = stringResource(
                                                    if (isFav) R.string.cd_favorite_on else R.string.cd_favorite_off,
                                                ),
                                                tint = if (isFav) HeritageForest else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                    }
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

private fun applySortOption(
    styles: List<DressStyle>,
    sortOption: DiscoverSortOption,
): List<DressStyle> = when (sortOption) {
    DiscoverSortOption.BEST_MATCH -> styles
    DiscoverSortOption.TOP_RATED -> styles.sortedWith(
        compareByDescending<DressStyle> { it.heritageRating().toFloatOrNull() ?: 0f }
            .thenByDescending { it.heritageReviews() },
    )
    DiscoverSortOption.PRICE_LOW_HIGH -> styles.sortedBy {
        it.heritagePrice().removePrefix("$").toFloatOrNull() ?: 0f
    }
    DiscoverSortOption.PRICE_HIGH_LOW -> styles.sortedByDescending {
        it.heritagePrice().removePrefix("$").toFloatOrNull() ?: 0f
    }
}

private fun orderFeaturedFirst(
    styles: List<DressStyle>,
): List<DressStyle> {
    val priority = listOf("kaba_slit", "modern_kaba", "maxi_dress", "co_ords", "peplum", "african_gown")
    val byId = styles.associateBy { it.id }
    val featured = priority.mapNotNull { byId[it] }
    val rest = styles.filter { it.id !in priority }
    return featured + rest
}

private fun displayTitleFor(
    style: DressStyle,
    context: android.content.Context,
): String = when (style.id) {
    "kaba_slit" -> "Modern Kente Dress"
    "modern_kaba" -> "Elegant Peplum Dress"
    "maxi_dress" -> "Maxi Dress"
    "co_ords" -> "Two-Piece Set"
    else -> context.resolveKey(style.titleKey) ?: style.id
}

/** Builds a display-only swatch for the fabric library without pretending to be a scan. */
private fun representativeFabric(cat: FabricCategory): com.fabricfit.app.domain.model.Fabric {
    val palette = when (cat) {
 * when nothing in the query maps to a known filter.
 */
private fun smartFilterStyles(
    styles: List<DressStyle>,
    q: String,
    context: android.content.Context,
): List<DressStyle> {
    if (q.isEmpty()) return styles
    val intent = com.fabricfit.app.domain.engine.SearchIntentParser.parse(q)
    val hasStructured = intent.occasions.isNotEmpty() || intent.gender != null ||
        intent.category != null || intent.fabricKeywords.isNotEmpty()
    if (!hasStructured) {
        return styles.filter {
            displayTitleFor(it, context).contains(q, ignoreCase = true) ||
                context.resolveKey(it.titleKey).orEmpty().contains(q, ignoreCase = true) ||
                context.resolveKey(it.descriptionKey).orEmpty().contains(q, ignoreCase = true)
        }
    }
    val fabricCats = intent.fabricKeywords.mapNotNull { kw ->
        FabricCategory.values()
            .firstOrNull { it.name.lowercase().startsWith(kw) }
    }.toSet()
    return styles.filter { style ->
        (intent.gender == null || style.gender == intent.gender ||
            style.gender == GenderCategory.UNISEX) &&
            (intent.occasions.isEmpty() || style.occasions.intersect(intent.occasions).isNotEmpty()) &&
            (intent.category == null || style.category == intent.category) &&
            (fabricCats.isEmpty() || style.affinity.intersect(fabricCats).isNotEmpty()) &&
            (intent.remainder.isBlank() ||
                displayTitleFor(style, context).contains(intent.remainder, ignoreCase = true) ||
                context.resolveKey(style.titleKey).orEmpty().contains(intent.remainder, ignoreCase = true) ||
                context.resolveKey(style.descriptionKey).orEmpty().contains(intent.remainder, ignoreCase = true))
    }