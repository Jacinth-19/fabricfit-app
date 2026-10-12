package com.ntoma.studio.ui.screens.create

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.ui.components.ErrorState
import com.ntoma.studio.ui.navigation.Routes
import com.ntoma.studio.ui.util.openFabricFlow
import com.ntoma.studio.ui.util.rememberPhotoPicker
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(nav: NavHostController, padding: PaddingValues, target: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var backLens by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    var mode by remember { mutableStateOf(0) } // 0 photo · 1 video (soon) · 2 scan
    val picker = com.ntoma.studio.ui.util.rememberPhotoPicker { uri ->
        context.openFabricFlow(
            uri,
            navigate = { nav.navigate(it) },
            onError = { e -> scope.launch { snackbar.showSnackbar(context.getString(e.titleRes)) } },
        )
    }
    val ivory = androidx.compose.ui.graphics.Color(0xFFF2F3F7)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color(0xFF101210),
    ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
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
                    tint = ivory,
                )
            }
            Text(
                stringResource(if (target == "person") R.string.tryon_photo_camera else R.string.camera_title),
                style = MaterialTheme.typography.titleLarge,
                color = ivory,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { flashOn = !flashOn; imageCapture?.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }) {
                Icon(
                    if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(if (flashOn) R.string.camera_flash_on else R.string.camera_flash_off),
                    tint = ivory,
                )
            }
            IconButton(onClick = { backLens = !backLens }) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_switch_lens),
                    tint = ivory,
                )
            }
        }

        if (failed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column {
                    ErrorState(R.string.camera_no_device, R.string.camera_no_device_body)
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
                            Log.w("NtomaCamera", "bind failed", e)
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
                    Modifier.fillMaxSize().padding(28.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), MaterialTheme.shapes.large),
                )
                Text(
                    stringResource(R.string.camera_guidance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.camera_focus_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 88.dp)
                        .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )

                // Heritage capture controls: Photo · Video · Scan + white shutter.
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        CameraModeTab(R.string.camera_mode_photo, active = mode == 0) { mode = 0 }
                        CameraModeTab(R.string.camera_mode_video, active = false) {
                            scope.launch {
                                snackbar.showSnackbar(context.getString(R.string.camera_video_soon))
                            }
                        }
                        CameraModeTab(R.string.camera_mode_scan, active = mode == 2) {
                            mode = 2
                            picker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(contentAlignment = Alignment.Center) {
                        if (capturing) {
                            CircularProgressIndicator(modifier = Modifier.size(76.dp))
                        } else {
                            Surface(
                                onClick = {
                                    capture(
                                        context = context,
                                        imageCapture = imageCapture,
                                        executor = executor,
                                        onCapturing = { capturing = it },
                                        onSaved = { file ->
                                            nav.navigate(Routes.edit(Uri.fromFile(file).toString(), target))
                                        },
                                        onError = { capturing = false },
                                    )
                                },
                                shape = CircleShape,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier
                                    .size(76.dp)
                                    .border(
                                        4.dp,
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                                        CircleShape,
                                    )
                                    .semantics { contentDescription = context.getString(R.string.camera_capture) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFF252722),
                                    )
                                }
                            }
                        }
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    snackbar,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
    }
}

@Composable
private fun CameraModeTab(label: Int, active: Boolean, onClick: () -> Unit) {
    val gold = com.ntoma.studio.ui.theme.KenteGold
    val dim = androidx.compose.ui.graphics.Color(0xFFB9C1D0)
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = if (active) gold else dim,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
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
