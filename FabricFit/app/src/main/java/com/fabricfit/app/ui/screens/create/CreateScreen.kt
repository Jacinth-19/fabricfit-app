package com.fabricfit.app.ui.screens.create

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cached
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.permissions.rememberCameraPermission
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.FirstRunTooltip
import com.fabricfit.app.ui.components.SamplePickerDialog
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.util.openFabricFlow
import com.fabricfit.app.ui.util.rememberPhotoPicker
import kotlinx.coroutines.launch

@Composable
fun CreateScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: CreateViewModel = appViewModel { c, ctx -> CreateViewModel(c) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSamples by remember { mutableStateOf(false) }
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
            // 1. Heritage Luxe Capture Fabric Viewfinder Hero (Reference Image 3)
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

                    // Live Kente weave viewfinder frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { requestCamera() },
                    ) {
                        AssetImage(
                            asset = "catalog/heritage/scan_kente_closeup.jpg",
                            contentDescription = "Capture Fabric Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
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

                    // Shutter row: Gallery swatch thumbnail · Double-ring white shutter · Sample / Flip icon
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: Gallery / Kente swatch thumbnail
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, HeritageCream.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { picker.launch(pickRequest()) },
                        ) {
                            AssetImage(
                                asset = "catalog/heritage/swatch_kente.jpg",
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

                        // Right: Sample / Flip button
                        Surface(
                            onClick = { showSamples = true },
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
