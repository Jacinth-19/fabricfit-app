package com.ntoma.studio.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntoma.studio.R
import com.ntoma.studio.di.AppContainer
import com.ntoma.studio.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Bundled, attributed editorial photographs. No image request or user photo goes online. */
data class EditorialPhoto(val asset: String, val title: Int, val description: Int, val credit: String, val source: String)
val editorialPhotos = listOf(
    EditorialPhoto("editorial/kente-festival.jpg", R.string.studio_festival_title, R.string.studio_festival_description,
        "Fquasie · CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Kente_fashion_in_Ghana.jpg"),
    EditorialPhoto("editorial/adowa-kente.jpg", R.string.studio_adowa_title, R.string.studio_adowa_description,
        "Bright Kwame Ayisi (Kwameghana) · CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Adowa_Dancer_in_Kente_Cloth.jpg"),
)

@Composable
fun StudioHeader(modifier: Modifier = Modifier, subtitle: Int = R.string.studio_header_subtitle) {
    Row(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        BrandMark(Modifier.size(38.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.heritage_brand), fontFamily = com.ntoma.studio.ui.theme.HeritageSerif, fontSize = 27.sp, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ThemeSwitch()
    }
}

@Composable
fun ThemeSwitch() {
    val context = LocalContext.current
    val settings = AppContainer.get(context).settings
    val prefs by settings.preferences.collectAsStateWithLifecycle(initialValue = com.ntoma.studio.domain.model.UserPreferences())
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = prefs.themeMode == ThemeMode.DARK || (prefs.themeMode == ThemeMode.SYSTEM && systemDark)
    val scope = rememberCoroutineScope()
    FilledTonalIconButton(modifier = Modifier.testTag("studio_theme_toggle"), onClick = {
        scope.launch { settings.update { it.copy(themeMode = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } }
    }) {
        Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            stringResource(if (dark) R.string.studio_switch_light else R.string.studio_switch_dark))
    }
}

@Composable
fun StudioHero(onCreate: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp)).testTag("studio_editorial_hero")) {
        AssetImage(editorialPhotos[0].asset, stringResource(editorialPhotos[0].description), Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2173F35)), startY = 180f)))
        Surface(Modifier.align(Alignment.TopStart).padding(18.dp), shape = RoundedCornerShape(50), color = Color(0xF2F8F3E9)) {
            Text(stringResource(R.string.studio_hero_badge), Modifier.padding(12.dp, 7.dp), color = Color(0xFF173F35), style = MaterialTheme.typography.labelSmall)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Text(stringResource(R.string.studio_hero_title), color = Color.White, fontFamily = com.ntoma.studio.ui.theme.HeritageSerif,
                fontSize = 29.sp, lineHeight = 34.sp)
            Text(stringResource(R.string.studio_hero_body), Modifier.padding(top = 8.dp, bottom = 16.dp),
                color = Color(0xFFF0EDE2), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD6A84F), contentColor = Color(0xFF252722))) {
                Text(stringResource(R.string.studio_start_designing))
                Spacer(Modifier.width(16.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@Composable
fun PhotoCredits(photo: EditorialPhoto) {
    var showing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    TextButton(onClick = { showing = true }, contentPadding = PaddingValues(0.dp)) {
        Text(stringResource(R.string.studio_photo_credit), style = MaterialTheme.typography.labelSmall)
    }
    if (showing) AlertDialog(onDismissRequest = { showing = false },
        title = { Text(stringResource(photo.title)) },
        text = { Column {
            Text(stringResource(photo.description))
            Spacer(Modifier.height(12.dp)); Text(photo.credit)
            Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.studio_credit_disclosure))
        } }, confirmButton = {
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(photo.source))); showing = false }) {
                Text(stringResource(R.string.studio_view_source))
            }
        }, dismissButton = { TextButton(onClick = { showing = false }) { Text(stringResource(R.string.action_close)) } })
}

@Composable
fun RealPhotoSample(onSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val photo = editorialPhotos[1]
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetImage(photo.asset, stringResource(photo.description), Modifier.size(90.dp, 126.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(stringResource(R.string.studio_real_sample_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.studio_real_sample_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(enabled = !loading, modifier = Modifier.testTag("studio_real_photo_sample"), contentPadding = PaddingValues(0.dp), onClick = {
                    loading = true; failed = false
                    scope.launch {
                        try {
                            val uri = withContext(Dispatchers.IO) {
                                val file = File(context.cacheDir, "ntoma_adowa_sample.jpg")
                                context.assets.open(photo.asset).use { input -> file.outputStream().use { input.copyTo(it) } }
                                Uri.fromFile(file)
                            }
                            onSelected(uri)
                        } catch (_: Exception) { failed = true }
                        finally { loading = false }
                    }
                }) { Text(stringResource(if (loading) R.string.template_loading else R.string.studio_use_real_photo)) }
                if (failed) Text(stringResource(R.string.studio_sample_error), color = MaterialTheme.colorScheme.error)
                PhotoCredits(photo)
            }
        }
    }
}
