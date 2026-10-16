package com.fabricfit.app.ui.screens.create

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.media.ColorPatternAnalyzer
import com.fabricfit.app.permissions.rememberCameraPermission
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.FirstRunTooltip
import com.fabricfit.app.ui.components.SamplePickerDialog
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.openFabricFlow
import com.fabricfit.app.ui.util.rememberPhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

enum class CaptureMode(val label: String) {
    PHOTO("Photo"),
    VIDEO("Video"),
    SCAN("Scan"),
}

private val VIEWFINDER_SAMPLE_ASSETS = listOf(
    "catalog/heritage/scan_kente_closeup.jpg",
    "catalog/heritage/swatch_ankara.jpg",
    "catalog/heritage/swatch_lace.jpg",
    "catalog/heritage/swatch_batik.jpg",
)

@Composable
fun CreateScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: CreateViewModel = appViewModel { c, ctx -> CreateViewModel(c) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSamples by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.SCAN) }
    var sampleIndex by remember { mutableIntStateOf(0) }
    var hudVisible by remember { mutableStateOf(false) }
    var liveSignature by remember { mutableStateOf<ColorPatternAnalyzer.FabricSignature?>(null) }

    // Embedded CameraX state (activates automatically when CAMERA permission is granted & hardware exists)
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraBound by remember { mutableStateOf(false) }
    var backLens by remember { mutableStateOf(true) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val tooltips = AppContainer.get(context).tooltips
    val tooltipSeen by tooltips.seen(TooltipStore.CREATE).collectAsState(initial = true)

    val requestCamera = rememberCameraPermission {
        hasCameraPerm = true
        nav.navigate(Routes.CAMERA_FABRIC)
    }
    val picker = rememberPhotoPicker { uri ->
        context.openFabricFlow(
            uri,
        )
    }

    val currentSampleAsset = VIEWFINDER_SAMPLE_ASSETS[sampleIndex % VIEWFINDER_SAMPLE_ASSETS.size]

    // Continuously compute real-time ColorPatternAnalyzer metrics from live camera frame or active sample swatch
    LaunchedEffect(cameraBound, currentSampleAsset, captureMode, hudVisible) {
        while (true) {
            val frameBmp: Bitmap? = if (cameraBound) {
                previewView?.bitmap
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        context.assets.open(currentSampleAsset).use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            if (frameBmp != null) {
                val sig = withContext(Dispatchers.Default) {
                    try {
                        val scaled = Bitmap.createScaledBitmap(frameBmp, 64, 64, true)
                        val px = IntArray(64 * 64)
                        scaled.getPixels(px, 0, 64, 0, 0, 64, 64)
                        ColorPatternAnalyzer.analyze(px, 64, 64)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (sig != null) liveSignature = sig
            }
            if (!cameraBound) break
            delay(750)
        }
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

                    // Embedded Live CameraX / Kente Weave Viewfinder Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (cameraBound) {
                                    hudVisible = !hudVisible
                                } else {
                                    requestCamera()
                                }
                            },
                    ) {
                        if (hasCameraPerm) {
                            AndroidView(
                                factory = { ctx -> PreviewView(ctx) },
                                modifier = Modifier.fillMaxSize(),
                                update = { view -> previewView = view },
                            )
                            DisposableEffect(lifecycleOwner, backLens) {
                                val providerFuture = ProcessCameraProvider.getInstance(context)
                                providerFuture.addListener({
                                    try {
                                        val provider = providerFuture.get()
                                        val selector = if (backLens) {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        }
                                        if (provider.hasCamera(selector)) {
                                            val preview = Preview.Builder().build()
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .build()
                                            imageCapture = capture
                                            provider.unbindAll()
                                            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                                            previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }
                                            cameraBound = true
                                        } else {
                                            cameraBound = false
                                        }
                                    } catch (e: Exception) {
                                        Log.w("CreateScreenCamera", "Embedded CameraX fallback to asset", e)
                                        cameraBound = false
                                    }
                                }, ContextCompat.getMainExecutor(context))
                                onDispose {
                                    try {
                                        providerFuture.get().unbindAll()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }

                        // Fallback to high-resolution textile preview when camera is not bound
                        if (!cameraBound) {
                            AssetImage(
                                asset = currentSampleAsset,
                                contentDescription = "Capture Fabric Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Real-time Scan HUD overlay (shown in Video mode or when toggled)
                        val sig = liveSignature
                        AnimatedVisibility(
                            visible = (captureMode == CaptureMode.VIDEO || hudVisible) && sig != null,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                        ) {
                            if (sig != null) {
                                LiveScanHudBadge(signature = sig)
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Interactive Mode switcher: Photo · Video · ● Scan
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CaptureMode.entries.forEach { mode ->
                            val selected = captureMode == mode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        captureMode = mode
                                        if (mode == CaptureMode.VIDEO) {
                                            hudVisible = true
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                if (selected) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3FA66C)),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color.White else HeritageCream.copy(alpha = 0.6f),
                                )
                            }
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
                                asset = currentSampleAsset,
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
                                .clickable {
                                    if (cameraBound && imageCapture != null && !capturing) {
                                        capturing = true
                                        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                                        imageCapture?.takePicture(
                                            opts,
                                            cameraExecutor,
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    capturing = false
                                                    val uriStr = Uri.fromFile(file).toString()
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        if (captureMode == CaptureMode.PHOTO) {
                                                            nav.navigate(Routes.edit(uriStr, "fabric"))
                                                        } else {
                                                            nav.navigate(Routes.analysis(uriStr))
                                                        }
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    capturing = false
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        requestCamera()
                                                    }
                                                }
                                            },
                                        )
                                    } else {
                                        requestCamera()
                                    }
                                },
                        ) {
                            if (capturing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(48.dp),
                                )
                            } else {
                                Box(
                                    Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                )
                            }
                        }

                        // Right: Flip camera (when live camera bound) or Sample / Swatch Cycler
                        Surface(
                            onClick = {
                                if (cameraBound) {
                                    backLens = !backLens
                                } else {
                                    sampleIndex = (sampleIndex + 1) % VIEWFINDER_SAMPLE_ASSETS.size
                                    hudVisible = true
                                }
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
                )
            }
            item {
                DarkOptionCard(
                    icon = Icons.Filled.PhotoLibrary,
                    title = R.string.create_option_gallery_title,
                    body = R.string.create_option_gallery_body,
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
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        if (showSamples) {
            SamplePickerDialog(
                onDismiss = { showSamples = false },
                onPicked = { uri ->
                    showSamples = false
}

@Composable
internal fun LiveScanHudBadge(
    signature: ColorPatternAnalyzer.FabricSignature,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HeritageForestDark.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3FA66C)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${context.getString(signature.category.labelRes)} · ${(signature.categoryConfidence * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = HeritageCream,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${context.getString(signature.pattern.labelRes)} · ${context.getString(signature.texture.labelRes)} · Repeat ${(signature.motifScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = HeritageCream.copy(alpha = 0.78f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                signature.colors.take(4).forEach { c ->
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(c.argb))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    )
                }
            }
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