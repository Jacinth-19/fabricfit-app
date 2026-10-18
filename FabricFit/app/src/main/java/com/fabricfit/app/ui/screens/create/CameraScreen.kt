package com.fabricfit.app.ui.screens.create

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.media.ColorPatternAnalyzer
import com.fabricfit.app.ui.components.ErrorState
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

enum class CaptureMode(val label: String) {
    PHOTO("Photo"),
    SCAN("Scan"),
}

@Composable
fun CameraScreen(nav: NavHostController, padding: PaddingValues, target: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var backLens by remember { mutableStateOf(target != "person") }
    var failed by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.SCAN) }
    var liveSignature by remember { mutableStateOf<ColorPatternAnalyzer.FabricSignature?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Real-time fabric analysis HUD loop when scanning fabrics
    LaunchedEffect(target, failed) {
        if (target != "fabric" || failed) return@LaunchedEffect
        while (true) {
            val frame = previewView?.bitmap
            if (frame != null) {
                val sig = withContext(Dispatchers.Default) {
                    try {
                        val scaled = Bitmap.createScaledBitmap(frame, 64, 64, true)
                        val px = IntArray(64 * 64)
                        scaled.getPixels(px, 0, 64, 0, 0, 64, 64)
                        ColorPatternAnalyzer.analyze(px, 64, 64)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (sig != null) liveSignature = sig
            }
            delay(700)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HeritageForestDark)
            .padding(padding),
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = HeritageCream,
                )
            }
            Text(
                stringResource(if (target == "person") R.string.tryon_photo_camera else R.string.camera_title),
                style = MaterialTheme.typography.titleLarge,
                color = HeritageCream,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    flashOn = !flashOn
                    imageCapture?.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                },
            ) {
                Icon(
                    if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(if (flashOn) R.string.camera_flash_on else R.string.camera_flash_off),
                    tint = HeritageCream,
                )
            }
            IconButton(onClick = { backLens = !backLens }) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_switch_lens),
                    tint = HeritageCream,
                )
            }
        }

        if (failed) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorState(R.string.camera_no_device, R.string.camera_no_device_body)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val assetPath = if (target == "person") {
                                    "catalog/heritage/outfit_modern_kente.jpg"
                                } else {
                                    "catalog/heritage/scan_kente_closeup.jpg"
                                }
                                val savedUri = withContext(Dispatchers.IO) {
                                    val out = File(context.cacheDir, "sample_${target}_${System.currentTimeMillis()}.jpg")
                                    context.assets.open(assetPath).use { input ->
                                        out.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    Uri.fromFile(out).toString()
                                }
                                if (target == "person") {
                                    val tryOnRoute = "tryon?fabricId={fabricId}&styleId={styleId}&person={person}"
                                    val tryOnEntry = runCatching { nav.getBackStackEntry(tryOnRoute) }.getOrNull()
                                    if (tryOnEntry != null) {
                                        tryOnEntry.savedStateHandle["person_path"] = savedUri
                                        nav.popBackStack(route = tryOnRoute, inclusive = false)
                                    } else {
                                        nav.navigate(Routes.tryOn(null, null, savedUri))
                                    }
                                } else {
                                    nav.navigate(Routes.analysis(savedUri))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeritageGold,
                            contentColor = HeritageForestDark,
                        ),
                    ) {
                        Text(stringResource(R.string.sample_cta), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().weight(1f)) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx) },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        previewView = view
                        view.setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                focusAt(view, camera, event.x, event.y)
                            }
                            true
                        }
                    },
                )

                DisposableEffect(lifecycleOwner, backLens) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        try {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build()
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                .build()
                            imageCapture = capture
                            val selector = if (backLens) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                            preview.setSurfaceProvider(previewView!!.surfaceProvider)
                        } catch (e: Exception) {
                            Log.w("FabricFitCamera", "bind failed", e)
                            failed = true
                        }
                    }, ContextCompat.getMainExecutor(context))
                    onDispose {
                        try {
                            providerFuture.get().unbindAll()
                        } catch (e: Exception) {
                        }
                    }
                }

                // Frame + guidance overlay
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                        .border(2.dp, HeritageGold.copy(alpha = 0.75f), MaterialTheme.shapes.large),
                )

                // Live scan HUD badge at top when scanning fabrics
                val sig = liveSignature
                if (target == "fabric" && sig != null) {
                    LiveScanHudBadge(
                        signature = sig,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 36.dp, vertical = 36.dp),
                    )
                } else {
                    Text(
                        stringResource(R.string.camera_guidance),
                        style = MaterialTheme.typography.labelLarge,
                        color = HeritageCream,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                            .background(HeritageForest.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                // Mode selector + Capture button at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (target == "fabric") {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    HeritageForestDark.copy(alpha = 0.82f),
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            CaptureMode.entries.forEach { mode ->
                                val selected = captureMode == mode
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { captureMode = mode }
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
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
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else HeritageCream.copy(alpha = 0.65f),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    } else {
                        Text(
                            stringResource(R.string.camera_focus_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = HeritageCream,
                            modifier = Modifier
                                .padding(bottom = 14.dp)
                                .background(HeritageForestDark.copy(alpha = 0.75f), MaterialTheme.shapes.medium)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }

                    if (capturing) {
                        CircularProgressIndicator(
                            color = HeritageGold,
                            modifier = Modifier.size(72.dp),
                        )
                    } else {
                        Surface(
                            onClick = {
                                capture(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = executor,
                                    onCapturing = { capturing = it },
                                    onSaved = { file ->
                                        val uriStr = Uri.fromFile(file).toString()
                                        ContextCompat.getMainExecutor(context).execute {
                                            if (target == "fabric" && captureMode != CaptureMode.PHOTO) {
                                                nav.navigate(Routes.analysis(uriStr))
                                            } else {
                                                nav.navigate(Routes.edit(uriStr, target))
                                            }
                                        }
                                    },
                                    onError = { capturing = false },
                                )
                            },
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, HeritageGold, CircleShape)
                                .semantics { contentDescription = context.getString(R.string.camera_capture) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = HeritageForestDark,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun focusAt(view: PreviewView, camera: Camera?, x: Float, y: Float) {
    val cam = camera ?: return
    val factory = SurfaceOrientedMeteringPointFactory(view.width.toFloat(), view.height.toFloat())
    val point = factory.createPoint(x, y)
    cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
}

private fun capture(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.ExecutorService,
    onCapturing: (Boolean) -> Unit,
    onSaved: (File) -> Unit,
    onError: () -> Unit,
) {
    val capture = imageCapture ?: return
    onCapturing(true)
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(
        options,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                onCapturing(false)
                onSaved(file)
            }

            override fun onError(exception: ImageCaptureException) {
                onCapturing(false)
                onError()
            }
        },
    )
}

@Composable
private fun LiveScanHudBadge(
    signature: ColorPatternAnalyzer.FabricSignature,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.75f),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Live Scan",
                style = MaterialTheme.typography.labelMedium,
                color = HeritageGold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = signature.category?.name ?: "Analyzing...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
    }
}
