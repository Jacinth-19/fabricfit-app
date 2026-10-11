package com.ntoma.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntoma.studio.R
import com.ntoma.studio.domain.model.DressStyle
import com.ntoma.studio.ui.util.resolveKey

/** Actual catalogue silhouettes, clearly labelled templates. No unrelated model
 * photograph is passed off as this particular garment or as generated output. */
@Composable
fun HeritageStyleCard(style: DressStyle, isFavorite: Boolean, onFavorite: () -> Unit,
                      onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(onClick = onOpen, modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box(Modifier.fillMaxWidth()) {
            GarmentArt(style, null, Modifier.fillMaxWidth().height(170.dp).padding(8.dp))
            FilledIconButton(onClick = onFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).testTag("style_favorite_${style.id}"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    stringResource(if (isFavorite) R.string.cd_favorite_on else R.string.cd_favorite_off),
                    tint = MaterialTheme.colorScheme.tertiary)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.heritage_template_card), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(context.resolveKey(style.titleKey) ?: style.id, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp))
            Text(stringResource(style.category.labelRes), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            val fabrics = style.affinity.sortedBy { it.name }.take(2).joinToString(" · ") { context.getString(it.labelRes) }
            if (fabrics.isNotEmpty()) Text(stringResource(R.string.heritage_compatibility, fabrics),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}
