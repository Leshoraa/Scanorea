package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.editor.domain.model.AdjustmentTool
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import java.util.Locale

/**
 * Material 3 document adjustment panel.
 * Provides non-modal, inline contrast and brightness sliders alongside one-tap auto clean and reset actions.
 */
@Composable
fun EditorAdjustPanel(
    page: ImagePage,
    onAutoAdjust: () -> Unit,
    onResetAdjustments: () -> Unit,
    onAdjustmentChange: (contrast: Float, brightness: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf(AdjustmentTool.CONTRAST) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Row 1: Adjustment Tool Selection & Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contrast Tool Chip
            FilterChip(
                selected = activeTool == AdjustmentTool.CONTRAST,
                onClick = { activeTool = AdjustmentTool.CONTRAST },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Contrast,
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
                        imageVector = Icons.Outlined.Brightness6,
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

            // Auto Clean Action Chip
            FilterChip(
                selected = false,
                onClick = onAutoAdjust,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Auto Clean",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            // Reset Action Chip: Restores initial device recommendation
            val isModified = kotlin.math.abs(page.contrast - page.initialContrast) > 0.01f ||
                    kotlin.math.abs(page.brightness - page.initialBrightness) > 0.5f
            FilterChip(
                selected = false,
                onClick = onResetAdjustments,
                enabled = isModified,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
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

        // Row 2: Live Value Badge and Inline Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrement / Min indicator icon
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Decrease",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Inline Material 3 Slider
            when (activeTool) {
                AdjustmentTool.CONTRAST -> {
                    Slider(
                        value = page.contrast,
                        onValueChange = { newContrast ->
                            onAdjustmentChange(newContrast, page.brightness)
                        },
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
                            onAdjustmentChange(page.contrast, newBrightness)
                        },
                        valueRange = -50f..50f,
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

            // Increment / Max indicator icon
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Increase",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Tappable Value Pill Badge (tap resets active tool to neutral)
            val valueText = when (activeTool) {
                AdjustmentTool.CONTRAST -> String.format(Locale.US, "%.1fx", page.contrast)
                AdjustmentTool.BRIGHTNESS -> "${if (page.brightness.toInt() > 0) "+" else ""}${page.brightness.toInt()}"
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clickable {
                        when (activeTool) {
                            AdjustmentTool.CONTRAST -> onAdjustmentChange(1.0f, page.brightness)
                            AdjustmentTool.BRIGHTNESS -> onAdjustmentChange(page.contrast, 0.0f)
                        }
                    }
                    .padding(1.dp)
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
