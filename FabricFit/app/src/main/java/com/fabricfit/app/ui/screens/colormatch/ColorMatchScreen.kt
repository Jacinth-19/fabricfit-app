package com.fabricfit.app.ui.screens.colormatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.engine.ColorHarmonyEngine
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.LocalImage
import kotlin.math.roundToInt

private data class SkinPreset(
    val label: String,
    val argb: Long?,
)

/**
 * Colour pairing & skin-undertone harmony assistant: combines colour-theory harmonies with
 * wearer skin-undertone scoring ([ColorHarmonyEngine.suggestForWearer] &
 * [ColorHarmonyEngine.skinHarmonyScore]).
 */
@Composable
fun ColorMatchScreen(
    nav: NavHostController,
    padding: PaddingValues,
    fabricId: Long,
) {
    val context = LocalContext.current
    val container = AppContainer.get(context)
    val fabrics by container.fabricAnalysis.observeAll()
        .collectAsState(initial = emptyList<Fabric>())
    val fabric = fabrics.firstOrNull { it.id == fabricId } ?: fabrics.firstOrNull()
    val defaultKenteColors = remember {
        listOf(0xFF144B2FL, 0xFF611425L, 0xFFD99B26L, 0xFFCC292BL)
    }
    val baseColors = remember(fabric) {
        fabric?.colors?.map { c -> c.argb }?.ifEmpty { defaultKenteColors } ?: defaultKenteColors
    }

    val skinPresets = remember {
        listOf(
            SkinPreset("Universal", null),
            SkinPreset("Warm Golden", 0xFF8D5524L),
            SkinPreset("Neutral Rich", 0xFF5C3A21L),
            SkinPreset("Cool Deep", 0xFF3B2F2FL),
        )
    }
    var selectedSkin by remember { mutableStateOf(skinPresets[1]) }

    val suggestions = remember(baseColors, selectedSkin) {
        ColorHarmonyEngine.suggestForWearer(baseColors, selectedSkin.argb)
    }
    val skinHarmonyPct = remember(baseColors, selectedSkin) {
        selectedSkin.argb?.let {
            (ColorHarmonyEngine.skinHarmonyScore(baseColors, it) * 100f).roundToInt()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                stringResource(R.string.color_match_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fabric != null && fabric.imageUri.isNotBlank()) {
                    LocalImage(
                        uri = fabric.imageUri,
                        contentDescription = fabric.name,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    AssetImage(
                        asset = "catalog/heritage/swatch_kente.jpg",
                        contentDescription = "Kente Royalty",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        fabric?.name ?: "Kente Royalty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.color_match_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (skinHarmonyPct != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$skinHarmonyPct% Undertone Harmony (${selectedSkin.label})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Skin-tone undertone selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                skinPresets.forEach { preset ->
                    FilterChip(
                        selected = selectedSkin == preset,
                        onClick = { selectedSkin = preset },
                        leadingIcon = preset.argb?.let { argb ->
                            {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                )
                            }
                        },
                        label = { Text(preset.label) },
                    )
                }
            }

            suggestions.forEach { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(s.argb)),
                        )
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                stringResource(s.role.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                String.format("#%06X", s.argb and 0xFFFFFF),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(s.reasonRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.color_accessory_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}
