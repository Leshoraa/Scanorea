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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage

/**
 * Material 3 document crop and orientation panel.
 * Provides aspect ratio presets, 90° clockwise rotation, crop reset,
 * explicit Done confirmation, and page navigation / reordering triggers.
 */
@Composable
fun EditorCropPanel(
    page: ImagePage,
    currentPageIndex: Int,
    totalPages: Int,
    isReordering: Boolean,
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    onRatioSelected: (CropAspectRatio) -> Unit = {},
    onCropBoundsChange: (ImageCropBounds) -> Unit,
    onRotatePage: () -> Unit,
    onResetCrop: () -> Unit,
    onApplyCrop: () -> Unit = {},
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onMovePage: (sourceIndex: Int, targetIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPagePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Row 1: Aspect Ratio Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CropAspectRatio.entries.forEach { ratioPreset ->
                val isSelected = selectedRatio == ratioPreset
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        onRatioSelected(ratioPreset)
                        when (ratioPreset) {
                            CropAspectRatio.FREE -> {
                                // Keep current crop boundaries, allowing free manual dragging
                            }
                            CropAspectRatio.ORIGINAL -> {
                                onResetCrop()
                            }
                            CropAspectRatio.A4 -> {
                                val ratio = ratioPreset.ratio ?: (1f / 1.4142f)
                                onCropBoundsChange(ImageCropBounds.fromAspectRatio(ratio, page.effectiveAspectRatio))
                            }
                            CropAspectRatio.SQUARE -> {
                                onCropBoundsChange(ImageCropBounds.fromAspectRatio(1.0f, page.effectiveAspectRatio))
                            }
                        }
                    },
                    label = {
                        Text(
                            text = ratioPreset.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Action Controls & Page Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Page Button
            FilledTonalIconButton(
                onClick = onPreviousPage,
                enabled = currentPageIndex > 0 && !isReordering,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Previous page",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Rotate 90° Clockwise
            FilledTonalButton(
                onClick = onRotatePage,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                    contentDescription = "Rotate",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rotate (${page.rotationDegrees}°)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Reset Crop
            FilledTonalButton(
                onClick = {
                    onRatioSelected(CropAspectRatio.FREE)
                    onResetCrop()
                },
                enabled = !page.cropBounds.isDefault,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Reset Crop",
                    tint = if (!page.cropBounds.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!page.cropBounds.isDefault) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Apply Crop Done Button
            Button(
                onClick = onApplyCrop,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Done",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Move to... button with destination dropdown
            if (totalPages > 1) {
                Box {
                    FilledTonalButton(
                        onClick = { if (!isReordering) showPagePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Move to page",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Move...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showPagePicker,
                        onDismissRequest = { showPagePicker = false }
                    ) {
                        Text(
                            text = "Move this page to:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        for (targetIndex in 0 until totalPages) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (targetIndex == currentPageIndex) {
                                            "Page ${targetIndex + 1} (Current)"
                                        } else {
                                            "Page ${targetIndex + 1}"
                                        },
                                        fontWeight = if (targetIndex == currentPageIndex) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                enabled = targetIndex != currentPageIndex,
                                onClick = {
                                    showPagePicker = false
                                    onMovePage(currentPageIndex, targetIndex)
                                }
                            )
                        }
                    }
                }
            }

            // Next Page Button
            FilledTonalIconButton(
                onClick = onNextPage,
                enabled = currentPageIndex < totalPages - 1 && !isReordering,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Next page",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
