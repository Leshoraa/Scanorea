package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.editor.domain.model.AdjustmentTool
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Material 3 document adjustment panel.
 * Provides non-modal, inline contrast, brightness, and sharpness sliders alongside reset action
 * and interactive 0.1x increment/decrement steppers.
 */
@Composable
fun EditorAdjustPanel(
    page: ImagePage,
    onAutoAdjust: () -> Unit,
    onResetAdjustments: () -> Unit,
    onAdjustmentChange: (contrast: Float, brightness: Float, sharpness: Float) -> Unit,
    modifier: Modifier = Modifier,
    onAdjustmentChangeFinished: () -> Unit = {}
) {
    var activeTool by remember { mutableStateOf(AdjustmentTool.CONTRAST) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var localSharpness by remember(page.id, page.sharpness) { mutableFloatStateOf(page.sharpness) }
    var sharpnessDebounceJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(page.sharpness) {
        localSharpness = page.sharpness
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Tool Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contrast Tool Chip
            FilterChip(
                selected = activeTool == AdjustmentTool.CONTRAST,
                onClick = { activeTool = AdjustmentTool.CONTRAST },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Contrast,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Contrast",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeTool == AdjustmentTool.CONTRAST) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            // Brightness Tool Chip
            FilterChip(
                selected = activeTool == AdjustmentTool.BRIGHTNESS,
                onClick = { activeTool = AdjustmentTool.BRIGHTNESS },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Brightness6,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeTool == AdjustmentTool.BRIGHTNESS) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            // Sharpness Tool Chip
            FilterChip(
                selected = activeTool == AdjustmentTool.SHARPNESS,
                onClick = { activeTool = AdjustmentTool.SHARPNESS },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Sharpness",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeTool == AdjustmentTool.SHARPNESS) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            // Reset Action Chip
            val isModified = kotlin.math.abs(page.contrast - page.initialContrast) > 0.01f ||
                    kotlin.math.abs(page.brightness - page.initialBrightness) > 0.5f ||
                    page.sharpness > 0.01f
            FilterChip(
                selected = false,
                onClick = onResetAdjustments,
                enabled = isModified,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = if (isModified) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    iconColor = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val canDecrease = when (activeTool) {
            AdjustmentTool.CONTRAST -> page.contrast > 0.51f
            AdjustmentTool.BRIGHTNESS -> page.brightness > -49f
            AdjustmentTool.SHARPNESS -> localSharpness > 0.01f
        }

        val canIncrease = when (activeTool) {
            AdjustmentTool.CONTRAST -> page.contrast < 2.49f
            AdjustmentTool.BRIGHTNESS -> page.brightness < 49f
            AdjustmentTool.SHARPNESS -> localSharpness < 0.99f
        }

        // Live Value Badge, 0.1x Steppers, and Inline Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrement indicator button for 0.1x step down
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (activeTool) {
                        AdjustmentTool.CONTRAST -> {
                            val nextVal = (page.contrast - 0.1f).coerceIn(0.5f, 2.5f)
                            onAdjustmentChange(nextVal, page.brightness, page.sharpness)
                        }
                        AdjustmentTool.BRIGHTNESS -> {
                            val nextVal = (page.brightness - 5f).coerceIn(-50f, 50f)
                            onAdjustmentChange(page.contrast, nextVal, page.sharpness)
                        }
                        AdjustmentTool.SHARPNESS -> {
                            val nextVal = (localSharpness - 0.1f).coerceIn(0.0f, 1.0f)
                            localSharpness = nextVal
                            onAdjustmentChange(page.contrast, page.brightness, nextVal)
                        }
                    }
                    onAdjustmentChangeFinished()
                },
                enabled = canDecrease,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease 0.1x",
                        tint = if (canDecrease) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Inline Material 3 Slider
            when (activeTool) {
                AdjustmentTool.CONTRAST -> {
                    Slider(
                        value = page.contrast,
                        onValueChange = { newContrast ->
                            onAdjustmentChange(newContrast, page.brightness, page.sharpness)
                        },
                        onValueChangeFinished = onAdjustmentChangeFinished,
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                AdjustmentTool.BRIGHTNESS -> {
                    Slider(
                        value = page.brightness,
                        onValueChange = { newBrightness ->
                            onAdjustmentChange(page.contrast, newBrightness, page.sharpness)
                        },
                        onValueChangeFinished = onAdjustmentChangeFinished,
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                AdjustmentTool.SHARPNESS -> {
                    Slider(
                        value = localSharpness,
                        onValueChange = { newSharpness ->
                            localSharpness = newSharpness
                            sharpnessDebounceJob?.cancel()
                            sharpnessDebounceJob = coroutineScope.launch {
                                delay(120)
                                onAdjustmentChange(page.contrast, page.brightness, newSharpness)
                            }
                        },
                        onValueChangeFinished = {
                            sharpnessDebounceJob?.cancel()
                            onAdjustmentChange(page.contrast, page.brightness, localSharpness)
                            onAdjustmentChangeFinished()
                        },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Increment indicator button for 0.1x step up
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (activeTool) {
                        AdjustmentTool.CONTRAST -> {
                            val nextVal = (page.contrast + 0.1f).coerceIn(0.5f, 2.5f)
                            onAdjustmentChange(nextVal, page.brightness, page.sharpness)
                        }
                        AdjustmentTool.BRIGHTNESS -> {
                            val nextVal = (page.brightness + 5f).coerceIn(-50f, 50f)
                            onAdjustmentChange(page.contrast, nextVal, page.sharpness)
                        }
                        AdjustmentTool.SHARPNESS -> {
                            val nextVal = (localSharpness + 0.1f).coerceIn(0.0f, 1.0f)
                            localSharpness = nextVal
                            onAdjustmentChange(page.contrast, page.brightness, nextVal)
                        }
                    }
                    onAdjustmentChangeFinished()
                },
                enabled = canIncrease,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase 0.1x",
                        tint = if (canIncrease) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Tappable Value Pill Badge (tap resets active tool to neutral)
            val valueText = when (activeTool) {
                AdjustmentTool.CONTRAST -> String.format(Locale.US, "%.1fx", page.contrast)
                AdjustmentTool.BRIGHTNESS -> "${if (page.brightness.toInt() > 0) "+" else ""}${page.brightness.toInt()}"
                AdjustmentTool.SHARPNESS -> "${(localSharpness * 100).toInt()}%"
            }
            Surface(
                onClick = {
                    when (activeTool) {
                        AdjustmentTool.CONTRAST -> onAdjustmentChange(1.0f, page.brightness, page.sharpness)
                        AdjustmentTool.BRIGHTNESS -> onAdjustmentChange(page.contrast, 0.0f, page.sharpness)
                        AdjustmentTool.SHARPNESS -> {
                            localSharpness = 0.0f
                            onAdjustmentChange(page.contrast, page.brightness, 0.0f)
                        }
                    }
                    onAdjustmentChangeFinished()
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(1.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
