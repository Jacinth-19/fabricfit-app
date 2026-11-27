package com.fabricfit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForestDark

/**
 * One editorial card in the Discover ribbon. Asset, title and caption are bundled, so the
 * ribbon renders instantly and works offline like the rest of the catalogue.
 */
private data class Editorial(val asset: String, val title: String, val caption: String)

/** Photography already shipped in the catalogue — no network fetch, no new downloads. */
private val editorial: List<Editorial> = listOf(
    Editorial("catalog/photos/women_kente_01.jpg", "Kente, reimagined", "Off-shoulder floor length"),
    Editorial("catalog/photos/men_agbada_01.jpg", "The agbada", "Embroidered cream"),
    Editorial("catalog/photos/women_ankara_01.jpg", "Ankara peplum", "Fitted waist, flared"),
    Editorial("catalog/photos/men_smock_01.jpg", "Batakari smock", "Hand-loomed stripes"),
    Editorial("catalog/photos/women_lace_01.jpg", "Lace gown", "Scalloped sleeves"),
    Editorial("catalog/photos/men_kente_01.jpg", "Kente kaftan", "Tailored, matching set"),
)

/**
 * Horizontally scrolling strip of full-body editorial photographs. Gives Discover the
 * look of a shopping feed, and shows both women's and men's looks side by side.
 */
@Composable
fun EditorialRibbon(modifier: Modifier = Modifier) {
    val items = remember { editorial }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { entry ->
            Column(modifier = Modifier.width(148.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    AssetImage(
                        asset = entry.asset,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(196.dp),
                    )
                    // Legibility scrim under the caption.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        HeritageForestDark.copy(alpha = 0.72f),
                                    ),
                                ),
                            ),
                    )
                    Text(
                        entry.caption,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = HeritageCream,
                        maxLines = 1,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomStart)
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    entry.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HeritageForestDark,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
