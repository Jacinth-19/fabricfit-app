package com.fabricfit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R

/**
 * Image filters UI for adjusting brightness, contrast, and saturation.
 */
@Composable
fun ImageFiltersPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.image_filters_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Brightness slider
        FilterSlider(
            label = stringResource(R.string.filter_brightness),
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = -100f..100f,
            defaultValue = 0f,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Contrast slider
        FilterSlider(
            label = stringResource(R.string.filter_contrast),
            value = contrast,
            onValueChange = onContrastChange,
            valueRange = -100f..100f,
            defaultValue = 0f,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Saturation slider
        FilterSlider(
            label = stringResource(R.string.filter_saturation),
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = -100f..100f,
            defaultValue = 0f,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reset button
        androidx.compose.material3.TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.filter_reset))
        }
    }
}

@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${value.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Apply image filters to a bitmap.
 */
object ImageFilterProcessor {
    /**
     * Apply brightness, contrast, and saturation filters to a bitmap.
     */
    fun applyFilters(
        bitmap: Bitmap,
        brightness: Float,
        contrast: Float,
        saturation: Float,
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        // Normalize values
        val brightnessFactor = brightness / 100f
        val contrastFactor = (contrast + 100f) / 100f
        val saturationFactor = (saturation + 100f) / 100f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            var r = (pixel shr 16 and 0xFF)
            var g = (pixel shr 8 and 0xFF)
            var b = (pixel and 0xFF)

            // Apply brightness
            r = (r + brightnessFactor * 255).toInt().coerceIn(0, 255)
            g = (g + brightnessFactor * 255).toInt().coerceIn(0, 255)
            b = (b + brightnessFactor * 255).toInt().coerceIn(0, 255)

            // Apply contrast
            r = (((r / 255f - 0.5f) * contrastFactor + 0.5f) * 255).toInt().coerceIn(0, 255)
            g = (((g / 255f - 0.5f) * contrastFactor + 0.5f) * 255).toInt().coerceIn(0, 255)
            b = (((b / 255f - 0.5f) * contrastFactor + 0.5f) * 255).toInt().coerceIn(0, 255)

            // Apply saturation
            val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            r = (gray + saturationFactor * (r - gray)).toInt().coerceIn(0, 255)
            g = (gray + saturationFactor * (g - gray)).toInt().coerceIn(0, 255)
            b = (gray + saturationFactor * (b - gray)).toInt().coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
