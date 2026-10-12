package com.ntoma.studio.ui.screens.recommendations

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.domain.model.GenderCategory
import com.ntoma.studio.domain.model.Occasion
import com.ntoma.studio.domain.model.Recommendation
import com.ntoma.studio.ui.components.EmptyState
import com.ntoma.studio.ui.components.FabricSwatch
import com.ntoma.studio.ui.components.GarmentArt
import com.ntoma.studio.ui.navigation.Routes
import com.ntoma.studio.ui.navigation.appViewModel
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Share
import com.ntoma.studio.ui.util.shareImagePath
import com.ntoma.studio.ui.util.whyText
import com.ntoma.studio.ui.util.resolveKey

@Composable
fun RecommendationsScreen(nav: NavHostController, padding: PaddingValues, fabricId: Long) {
    val vm: RecommendationsViewModel = appViewModel { c, ctx -> RecommendationsViewModel(c, fabricId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.recs_title), style = MaterialTheme.typography.titleLarge)
                if (state.fabric != null) {
                    Text(
                        stringResource(R.string.recs_for_fabric, state.fabric!!.name ?: context.getString(state.fabric!!.category.labelRes)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.fabric != null) {
                FabricSwatch(state.fabric!!, modifier = Modifier.padding(end = 16.dp).width(44.dp).height(44.dp))
            }
        }

        // Gender chips
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GenderCategory.values().forEach { g ->
                FilterChip(
                    selected = state.gender == g,
                    onClick = { vm.setGender(g) },
                    label = { Text(stringResource(g.labelRes)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Occasion chips (scrollable)
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Occasion.values().toList()) { occ ->
                FilterChip(
                    selected = occ in state.occasions,
                    onClick = { vm.toggleOccasion(occ) },
                    label = { Text(stringResource(occ.labelRes)) },
                )
            }
        }
        if (state.gender != null || state.occasions.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    stringResource(R.string.recs_clear_filters),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { vm.clearFilters() }.padding(4.dp),
                )
            }
        }

        if (state.looks.isNotEmpty()) {
            LooksSection(state.looks)
            Spacer(Modifier.height(8.dp))
        }

        when {
            !state.loaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            state.filtered.isEmpty() -> EmptyState(
                illustration = R.drawable.illust_empty_collection,
                title = R.string.recs_empty_title,
                body = R.string.recs_empty_body,
                modifier = Modifier.weight(1f),
            )
            else -> Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.recs_subtitle, state.filtered.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    gridItems(state.filtered, key = { it.style.id }) { rec ->
                        RecommendationCard(
                            rec = rec,
                            isFavorite = rec.style.id in state.favoriteStyles,
                            onFavorite = { vm.toggleFavorite(rec.style) },
                            onDismiss = { vm.dismiss(rec.style) },
                            onOpen = { nav.navigate(Routes.design(rec.style.id)) },
                            onTryOn = { nav.navigate(Routes.tryOn(fabricId, rec.style.id)) },
                        )
                    }
                }
            }
        }
    }
}

/** Compact 2-column card in the Heritage board style: art, heart, title, gold match chip. */
@Composable
private fun RecommendationCard(
    rec: Recommendation,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onTryOn: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box {
                GarmentArt(
                    rec.style,
                    null,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.cd_favorite_on else R.string.cd_favorite_off,
                        ),
                        tint = if (isFavorite) {
                            com.ntoma.studio.ui.theme.Burgundy
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    context.resolveKey(rec.style.titleKey) ?: rec.style.id,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.recs_match_score, rec.score),
                        style = MaterialTheme.typography.labelMedium,
                        color = com.ntoma.studio.ui.theme.BrandGold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.recs_not_interested),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onTryOn,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                ) {
                    Text(
                        stringResource(R.string.recs_try_on),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun LooksSection(looks: List<com.ntoma.studio.domain.model.InspirationLook>) {
    val context = LocalContext.current
    Column {
        Text(
            stringResource(R.string.looks_section_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            stringResource(R.string.looks_section_caption),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(looks, key = { it.id }) { look ->
                Card(Modifier.width(140.dp)) {
                    Column {
                        com.ntoma.studio.ui.components.AssetImage(
                            asset = look.asset,
                            contentDescription = stringResource(R.string.looks_share_cd),
                            modifier = Modifier.fillMaxWidth().height(168.dp),
                        )
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                context.resolveKey(look.titleKey) ?: look.id,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { context.shareInspirationLook(look) }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.looks_share_cd),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun android.content.Context.shareInspirationLook(look: com.ntoma.studio.domain.model.InspirationLook) {
    try {
        val dir = java.io.File(cacheDir, "shared").apply { mkdirs() }
        val out = java.io.File(dir, "${look.id}.jpg")
        assets.open(look.asset).use { input -> out.outputStream().use { input.copyTo(it) } }
        shareImagePath(out.absolutePath, getString(R.string.looks_section_title))
    } catch (_: Exception) {
        // Sharing is best-effort; the photo is still visible in-app.
    }
}
