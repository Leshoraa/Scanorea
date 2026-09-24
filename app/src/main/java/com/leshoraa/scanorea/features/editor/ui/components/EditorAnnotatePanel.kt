package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationTool
import com.leshoraa.scanorea.features.editor.domain.model.StrokeSize
import kotlin.math.roundToInt

/**
 * Bottom tool panel for document markup.
 * Features tool selection chips, color swatches with custom color picker, stroke sizes, and opacity control.
 */
@Composable
fun EditorAnnotatePanel(
    selectedTool: AnnotationTool,
    onToolSelected: (AnnotationTool) -> Unit,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    selectedAlpha: Float,
    onAlphaSelected: (Float) -> Unit,
    selectedStrokeSize: StrokeSize,
    onStrokeSizeSelected: (StrokeSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var isColorPickerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Row 1: Tool Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pen Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.PEN,
                onClick = { onToolSelected(AnnotationTool.PEN) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.PEN.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.PEN) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )

            // Highlighter Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.HIGHLIGHTER,
                onClick = { onToolSelected(AnnotationTool.HIGHLIGHTER) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Highlight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.HIGHLIGHTER.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.HIGHLIGHTER) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )

            // Redact Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.REDACT,
                onClick = { onToolSelected(AnnotationTool.REDACT) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.REDACT.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.REDACT) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )

            // Rectangle Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.RECT_SHAPE,
                onClick = { onToolSelected(AnnotationTool.RECT_SHAPE) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CropSquare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.RECT_SHAPE.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.RECT_SHAPE) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )

            // Circle Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.OVAL_SHAPE,
                onClick = { onToolSelected(AnnotationTool.OVAL_SHAPE) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.OVAL_SHAPE.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.OVAL_SHAPE) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )

            // Arrow Tool
            ElevatedFilterChip(
                selected = selectedTool == AnnotationTool.ARROW_SHAPE,
                onClick = { onToolSelected(AnnotationTool.ARROW_SHAPE) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = AnnotationTool.ARROW_SHAPE.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTool == AnnotationTool.ARROW_SHAPE) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Color Swatches + Custom Picker + Stroke Size Chips + Opacity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset Color Circles
            AnnotationColors.PRESETS.forEach { colorLong ->
                val isSelected = selectedColor == colorLong
                val color = Color(colorLong)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorLong) }
                )
            }

            // Custom Color & Opacity Picker Button
            Surface(
                onClick = { isColorPickerVisible = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Custom Color & Opacity",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            // Stroke Size Selector Chips
            StrokeSize.entries.forEach { size ->
                val isSelected = selectedStrokeSize == size
                Surface(
                    onClick = { onStrokeSizeSelected(size) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            // Opacity Indicator & Button
            Surface(
                onClick = { isColorPickerVisible = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Opacity,
                        contentDescription = "Opacity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(selectedAlpha * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (isColorPickerVisible) {
        EditorColorPickerDialog(
            initialColor = selectedColor,
            initialAlpha = selectedAlpha,
            onColorConfirmed = { newColor, newAlpha ->
                onColorSelected(newColor)
                onAlphaSelected(newAlpha)
            },
            onDismissRequest = { isColorPickerVisible = false }
        )
    }
}
