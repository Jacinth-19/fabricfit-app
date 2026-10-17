package com.fabricfit.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R

/**
 * Video trimmer UI with timeline and start/end handles.
 */
@Composable
fun VideoTrimmer(
    videoUri: Uri,
    durationMs: Long,
    onTrimComplete: (startTimeMs: Long, endTimeMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var startTimeMs by remember { mutableStateOf(0L) }
    var endTimeMs by remember { mutableStateOf(durationMs) }
    var isPlaying by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.video_trim_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timeline with trim handles
        VideoTimeline(
            durationMs = durationMs,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            onStartChange = { startTimeMs = it },
            onEndChange = { endTimeMs = it },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(startTimeMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatTime(endTimeMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Duration display
        Text(
            text = stringResource(R.string.video_trim_duration, formatTime(endTimeMs - startTimeMs)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    startTimeMs = 0L
                    endTimeMs = durationMs
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.video_trim_reset))
            }

            Button(
                onClick = {
                    onTrimComplete(startTimeMs, endTimeMs)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.video_trim_apply))
            }
        }
    }
}

@Composable
private fun VideoTimeline(
    durationMs: Long,
    startTimeMs: Long,
    endTimeMs: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
) {
    val timelineWidth = 300f // dp
    val handleWidth = 20f // dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        // Selected range
        val startFraction = startTimeMs.toFloat() / durationMs
        val endFraction = endTimeMs.toFloat() / durationMs

        Box(
            modifier = Modifier
                .fillMaxWidth(endFraction - startFraction)
                .height(60.dp)
                .padding(start = (startFraction * timelineWidth).dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                ),
        )

        // Start handle
        TrimHandle(
            modifier = Modifier
                .padding(start = (startFraction * timelineWidth).dp)
                .align(Alignment.CenterStart),
            onDrag = { deltaX ->
                val deltaMs = (deltaX / timelineWidth * durationMs).toLong()
                val newStart = (startTimeMs + deltaMs).coerceIn(0, endTimeMs - 1000)
                onStartChange(newStart)
            },
        )

        // End handle
        TrimHandle(
            modifier = Modifier
                .padding(end = ((1f - endFraction) * timelineWidth).dp)
                .align(Alignment.CenterEnd),
            onDrag = { deltaX ->
                val deltaMs = (deltaX / timelineWidth * durationMs).toLong()
                val newEnd = (endTimeMs + deltaMs).coerceIn(startTimeMs + 1000, durationMs)
                onEndChange(newEnd)
            },
        )
    }
}

@Composable
private fun TrimHandle(
    modifier: Modifier = Modifier,
    onDrag: (Float) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .width(20.dp)
            .height(60.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    onDrag(dragAmount)
                }
            },
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 20.dp),
        ) {
            // Handle grip lines
            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
        }
    }
}

/**
 * Format milliseconds to mm:ss format.
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
