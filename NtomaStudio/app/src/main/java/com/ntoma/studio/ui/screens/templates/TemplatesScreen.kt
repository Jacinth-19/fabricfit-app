package com.ntoma.studio.ui.screens.templates

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ntoma.studio.R
import com.ntoma.studio.di.AppContainer
import com.ntoma.studio.domain.model.*
import com.ntoma.studio.ui.components.LocalImage
import com.ntoma.studio.ui.util.resolveKey
import com.ntoma.studio.ui.util.shareImagePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplatesScreen(nav: NavHostController, padding: PaddingValues, fabricId: Long, styleId: String) {
    val context = LocalContext.current
    val container = AppContainer.get(context)
    val scope = rememberCoroutineScope()
    var fabric by remember(fabricId) { mutableStateOf<Fabric?>(null) }
    var styles by remember { mutableStateOf<List<DressStyle>>(emptyList()) }
    var selected by remember { mutableStateOf<DressStyle?>(null) }
    var repeats by remember { mutableIntStateOf(3) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<AppError?>(null) }
    var exporting by remember { mutableStateOf(false) }
    LaunchedEffect(fabricId, styleId) {
        fabric = container.fabricAnalysis.get(fabricId)
        styles = container.dressStyles.all()
        selected = styles.find { it.id == styleId } ?: styles.firstOrNull()
        if (fabric == null || selected == null) error = AppError.InvalidImage
    }
    LaunchedEffect(fabric, selected, repeats) {
        preview = null
        val f = fabric ?: return@LaunchedEffect
        val s = selected ?: return@LaunchedEffect
        val result = withContext(Dispatchers.Default) { container.templateRenderer.render(f.imageUri, s, repeats) }
        when (result) {
            is Outcome.Success -> { preview = result.data; error = null }
            is Outcome.Failure -> error = result.error
        }
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) }
            Text(stringResource(R.string.template_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(R.string.template_disclosure), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
        fabric?.let { f ->
            Row(Modifier.padding(20.dp, 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                LocalImage(f.imageUri, stringResource(R.string.cd_fabric_image), Modifier.size(48.dp))
                Text(stringResource(R.string.template_selected_fabric), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            val bitmap = preview
            if (bitmap != null) Image(bitmap.asImageBitmap(), stringResource(R.string.template_title), Modifier.fillMaxSize().testTag("template_preview"), contentScale = ContentScale.Fit)
            else if (error == null) CircularProgressIndicator()
            else Text(context.getString(error!!.titleRes), color = MaterialTheme.colorScheme.error)
        }
        Text(selected?.let { context.resolveKey(it.titleKey) }.orEmpty(), Modifier.padding(20.dp, 8.dp), style = MaterialTheme.typography.titleMedium)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles, key = { it.id }) { s ->
                FilterChip(selected = s.id == selected?.id, onClick = { selected = s }, modifier = Modifier.testTag("template_style_${s.id}"),
                    label = { Text(context.resolveKey(s.titleKey) ?: s.id) })
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(stringResource(R.string.template_motif_scale), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { repeats = (repeats - 1).coerceAtLeast(1) }) { Text("−") }
            Text("$repeats")
            TextButton(onClick = { repeats = (repeats + 1).coerceAtMost(6) }, modifier = Modifier.testTag("template_more_repeats")) { Text("+") }
        }
        Button(enabled = preview != null && !exporting, modifier = Modifier.fillMaxWidth().padding(20.dp, 8.dp).testTag("share_template"), onClick = {
            val bitmap = preview ?: return@Button
            exporting = true
            scope.launch {
                try {
                    val file = withContext(Dispatchers.IO) { container.imageProcessor.save(bitmap, "template") }
                    context.shareImagePath(file.absolutePath, context.getString(R.string.template_share_text))
                } catch (e: Exception) { error = AppError.Generation }
                finally { exporting = false }
            }
        }) { Text(stringResource(R.string.template_share)) }
    }
}
