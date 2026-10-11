package com.ntoma.studio.ui.screens.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.media.FabricCrop
import com.ntoma.studio.media.FittedImage
import com.ntoma.studio.ui.navigation.Routes
import com.ntoma.studio.ui.navigation.appViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun EditImageScreen(nav: NavHostController, padding: PaddingValues, uri: String, target: String) {
    val vm: ImageEditViewModel = appViewModel { c, _ -> ImageEditViewModel(c, uri, target) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.resultPath) {
        val path = state.resultPath ?: return@LaunchedEffect
        nav.navigate(if (target == "person") Routes.tryOn(null, null, path) else Routes.analysis(path)) {
            popUpTo(Routes.CREATE)
        }
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
            }
            Text(stringResource(if (target == "person") R.string.edit_title_person else R.string.edit_title_fabric), style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(if (target == "person") R.string.edit_hint_person else R.string.edit_hint),
            modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium)
        val bitmap = state.bitmap
        val border = MaterialTheme.colorScheme.primary
        if (bitmap != null) {
            Canvas(Modifier.fillMaxWidth().weight(1f).padding(20.dp).testTag("fabric_crop_canvas")
                .pointerInput(bitmap, state.saving) {
                    if (state.saving) return@pointerInput
                    val fit = FittedImage.inView(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat())
                    var start = Offset.Zero
                    fun update(end: Offset) {
                        vm.select(FabricCrop(min(start.x, fit.imageX(end.x)), min(start.y, fit.imageY(end.y)),
                            max(start.x, fit.imageX(end.x)), max(start.y, fit.imageY(end.y))))
                    }
                    detectDragGestures(onDragStart = { start = Offset(fit.imageX(it.x), fit.imageY(it.y)) },
                        onDrag = { change, _ -> change.consume(); update(change.position) })
                }) {
                val fit = FittedImage.inView(bitmap.width, bitmap.height, size.width, size.height)
                drawImage(bitmap.asImageBitmap(), dstOffset = IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
                    dstSize = IntSize(fit.width.roundToInt(), fit.height.roundToInt()))
                val s = state.selection
                val l = fit.left + s.left * fit.width
                val t = fit.top + s.top * fit.height
                val r = fit.left + s.right * fit.width
                val b = fit.top + s.bottom * fit.height
                val dim = Color.Black.copy(alpha = .5f)
                drawRect(dim, Offset(fit.left, fit.top), Size(fit.width, t - fit.top))
                drawRect(dim, Offset(fit.left, b), Size(fit.width, fit.top + fit.height - b))
                drawRect(dim, Offset(fit.left, t), Size(l - fit.left, b - t))
                drawRect(dim, Offset(r, t), Size(fit.left + fit.width - r, b - t))
                drawRect(border, Offset(l, t), Size(r - l, b - t), style = Stroke(3.dp.toPx()))
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
            }
        }
        state.error?.let { Text(context.getString(it.titleRes), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp)) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = vm::rotate, enabled = !state.saving && bitmap != null) { Icon(Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.edit_rotate)) }
            OutlinedButton(onClick = vm::reset, enabled = !state.saving) { Text(stringResource(R.string.edit_reset)) }
            OutlinedButton(onClick = { vm.select(FabricCrop.FULL) }, enabled = !state.saving) { Text(stringResource(R.string.edit_ratio_full)) }
        }
        Button(onClick = vm::confirm, enabled = !state.saving && bitmap != null,
            modifier = Modifier.fillMaxWidth().padding(20.dp).testTag("confirm_fabric_crop")) {
            if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(if (target == "person") R.string.edit_use_photo else R.string.edit_use_fabric))
        }
    }
}
