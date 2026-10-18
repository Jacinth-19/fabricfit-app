package com.fabricfit.app.ui.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.permissions.rememberCameraPermission
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.FirstRunTooltip
import com.fabricfit.app.ui.components.SampleFabrics
import com.fabricfit.app.ui.components.SamplePickerDialog
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.openFabricFlow
import com.fabricfit.app.ui.util.rememberPhotoPicker
import kotlinx.coroutines.launch

private data class StudioBackdropPreset(
    val title: String,
    val subtitle: String,
    val heroAsset: String,
    val swatchAsset: String,
    val sampleIndex: Int,
)

private val STUDIO_PRESETS = listOf(
    StudioBackdropPreset(
        title = "Royal Ashanti Kente",
        subtitle = "Hand-woven silk & cotton strip geometry",
        heroAsset = "catalog/heritage/hero_kente_folds.jpg",
        swatchAsset = "catalog/heritage/swatch_kente.jpg",
        sampleIndex = 0,
    ),
    StudioBackdropPreset(
        title = "Vibrant Ankara Wax",
        subtitle = "High-contrast West African resist print",
        heroAsset = "catalog/heritage/hero_model_kente.jpg",
        swatchAsset = "catalog/heritage/swatch_ankara.jpg",
        sampleIndex = 1,
    ),
    StudioBackdropPreset(
        title = "Ceremonial Guipure Lace",
        subtitle = "Intricate openwork floral embroidery",
        heroAsset = "catalog/heritage/outfit_elegant_peplum.jpg",
        swatchAsset = "catalog/heritage/swatch_lace.jpg",
        sampleIndex = 2,
    ),
)

@Composable
fun CreateScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: CreateViewModel = appViewModel { c, ctx -> CreateViewModel(c) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSamples by remember { mutableStateOf(false) }
    var presetIndex by remember { mutableIntStateOf(0) }
    val activePreset = STUDIO_PRESETS[presetIndex % STUDIO_PRESETS.size]

    val tooltips = AppContainer.get(context).tooltips
    val tooltipSeen by tooltips.seen(TooltipStore.CREATE).collectAsState(initial = true)

    val requestCamera = rememberCameraPermission { nav.navigate(Routes.CAMERA_FABRIC) }
    val picker = rememberPhotoPicker { uri ->
        context.openFabricFlow(
            uri,
            navigate = { nav.navigate(it) },
            onError = { e -> scope.launch { snackbar.showSnackbar(context.getString(e.titleRes)) } },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(HeritageForestDark)
            .padding(padding),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Heritage Luxe Studio Scanner Viewfinder
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp),
                ) {
                    // Header row: "Capture Fabric" + Close
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Capture Fabric",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = HeritageCream,
                        )
                        IconButton(onClick = { nav.navigate(Routes.HOME) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = HeritageCream,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Editorial Studio Viewfinder Card with gold reticle & AI scanner HUD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                width = 1.dp,
                                color = HeritageGold.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(22.dp),
                            )
                            .clickable { requestCamera() },
                    ) {
                        AssetImage(
                            asset = activePreset.heroAsset,
                            contentDescription = activePreset.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Deep forest studio vignette overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            HeritageForestDark.copy(alpha = 0.65f),
                                            HeritageForestDark.copy(alpha = 0.35f),
                                            HeritageForestDark.copy(alpha = 0.82f),
                                        ),
                                    ),
                                ),
                        )

                        // Corner reticle brackets
                        ViewfinderCornerBrackets(
                            color = HeritageGold,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                        )

                        // Top AI Scanner status pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = HeritageForestDark.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = HeritageGold,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "AI Weave & Palette Scanner",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HeritageCream,
                                )
                            }
                        }

                        // Center camera launch prompt
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = HeritageForestDark.copy(alpha = 0.78f),
                                border = BorderStroke(1.5.dp, HeritageGold),
                                modifier = Modifier.size(64.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.CenterFocusStrong,
                                        contentDescription = null,
                                        tint = HeritageGold,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Tap to Open Live Camera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = activePreset.title + " · " + activePreset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = HeritageCream.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Bottom quick-analyze sample chip
                        Surface(
                            onClick = {
                                val sample = SampleFabrics.all.getOrElse(activePreset.sampleIndex) {
                                    SampleFabrics.all.first()
                                }
                                SampleFabrics.materialize(context, sample)?.let { uri ->
                                    nav.navigate(Routes.analysis(uri))
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = HeritageGold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp),
                        ) {
                            Text(
                                text = "Analyze ${activePreset.title} Sample →",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HeritageForestDark,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Mode switcher: Photo · Sample · • Scan
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeritageCream.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable { requestCamera() }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Text(
                            "Sample",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeritageCream.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable { showSamples = true }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { requestCamera() }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3FA66C)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Scan",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Shutter row: Gallery swatch thumbnail · Double-ring white shutter · Cycle sample preset
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: Gallery / current swatch thumbnail
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, HeritageCream.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { picker.launch(pickRequest()) },
                        ) {
                            AssetImage(
                                asset = activePreset.swatchAsset,
                                contentDescription = stringResource(R.string.create_option_gallery_title),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Center: Double-ring white shutter button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable { requestCamera() },
                        ) {
                            Box(
                                Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }

                        // Right: Cycle studio sample preset / open sample picker on long or repeated tap
                        Surface(
                            onClick = {
                                presetIndex = (presetIndex + 1) % STUDIO_PRESETS.size
                            },
                            shape = CircleShape,
                            color = HeritageForest,
                            border = BorderStroke(1.dp, HeritageCream.copy(alpha = 0.25f)),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Cached,
                                    contentDescription = stringResource(R.string.sample_cta),
                                    tint = HeritageCream,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(22.dp)) }

            // Quick mode cards below shutter bar
            item {
                DarkOptionCard(
                    icon = Icons.Filled.CameraAlt,
                    title = R.string.create_option_camera_title,
                    body = R.string.create_option_camera_body,
                    onClick = requestCamera,
                )
            }
            item {
                DarkOptionCard(
                    icon = Icons.Filled.PhotoLibrary,
                    title = R.string.create_option_gallery_title,
                    body = R.string.create_option_gallery_body,
                    onClick = { picker.launch(pickRequest()) },
                )
            }
            item {
                DarkOptionCard(
                    icon = Icons.Filled.Person,
                    title = R.string.create_option_person_title,
                    body = R.string.create_option_person_body,
                    onClick = { nav.navigate(Routes.tryOn(null, null)) },
                )
            }
        }

        if (!tooltipSeen) {
            FirstRunTooltip(
                text = R.string.tooltip_create,
                onDismiss = { scope.launch { tooltips.markSeen(TooltipStore.CREATE) } },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        if (showSamples) {
            SamplePickerDialog(
                onDismiss = { showSamples = false },
                onPicked = { uri ->
                    showSamples = false
                    nav.navigate(Routes.analysis(uri))
                },
            )
        }
    }
}

@Composable
private fun ViewfinderCornerBrackets(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val len = 26.dp.toPx()
        val stroke = 3.dp.toPx()
        val w = size.width
        val h = size.height
        // Top-left
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke, cap = StrokeCap.Round)
        // Top-right
        drawLine(color, Offset(w - len, 0f), Offset(w, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, len), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-left
        drawLine(color, Offset(0f, h - len), Offset(0f, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(len, h), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-right
        drawLine(color, Offset(w - len, h), Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w, h - len), Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun DarkOptionCard(icon: ImageVector, title: Int, body: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF133A2C)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFD4A84B),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = HeritageCream,
                )
                Text(
                    stringResource(body),
                    style = MaterialTheme.typography.bodySmall,
                    color = HeritageCream.copy(alpha = 0.72f),
                )
            }
        }
    }
}

private fun pickRequest() = androidx.activity.result.PickVisualMediaRequest(
    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
)
