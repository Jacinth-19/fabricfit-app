package com.fabricfit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R
import com.fabricfit.app.ui.util.ImageEditor

/**
 * Aspect ratio selector for image cropping.
 */
@Composable
fun AspectRatioSelector(
    selectedRatio: ImageEditor.AspectRatio,
    onRatioSelected: (ImageEditor.AspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.image_aspect_ratio),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            items(ImageEditor.AspectRatio.values().toList()) { ratio ->
                FilterChip(
                    selected = selectedRatio == ratio,
                    onClick = { onRatioSelected(ratio) },
                    label = { Text(ratio.label) },
                )
            }
        }
    }
}
