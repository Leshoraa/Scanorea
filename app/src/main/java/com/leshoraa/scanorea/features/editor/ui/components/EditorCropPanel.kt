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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
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
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage

/**
 * Operating mode inside the crop panel: rectangular crop vs 4-point perspective warp.
 */
enum class CropPanelMode(val label: String) {
    RECTANGLE("Rectangle"),
    PERSPECTIVE("Perspective")
}

/**
 * Material 3 document crop, perspective keystone, and orientation panel.
 * Provides aspect ratio presets, 4-corner perspective adjustment, auto paper edge detection,
 * 90° clockwise rotation, and page reordering triggers.
 */
@Composable
fun EditorCropPanel(
    page: ImagePage,
    currentPageIndex: Int,
    totalPages: Int,
    isReordering: Boolean,
    cropPanelMode: CropPanelMode = CropPanelMode.RECTANGLE,
    onCropPanelModeChange: (CropPanelMode) -> Unit = {},
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    onRatioSelected: (CropAspectRatio) -> Unit = {},
    stagedCropBounds: ImageCropBounds = ImageCropBounds.DEFAULT,
    stagedPerspectiveQuad: DocumentQuad = DocumentQuad.DEFAULT,
    onCropBoundsChange: (ImageCropBounds) -> Unit,
    onRotatePage: () -> Unit,
    onRotatePageLeft: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    onResetPerspective: () -> Unit = {},
    onApplyCrop: () -> Unit = {},
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onMovePage: (sourceIndex: Int, targetIndex: Int) -> Unit,
    isAutoDetecting: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPagePickerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Row 1: Mode Selectors & Contextual Preset Chips (Unified Single Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = cropPanelMode == CropPanelMode.RECTANGLE,
                onClick = { onCropPanelModeChange(CropPanelMode.RECTANGLE) },
                label = {
                    Text(
                        text = "Rectangle",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (cropPanelMode == CropPanelMode.RECTANGLE) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            FilterChip(
                selected = cropPanelMode == CropPanelMode.PERSPECTIVE,
                onClick = { onCropPanelModeChange(CropPanelMode.PERSPECTIVE) },
                label = {
                    Text(
                        text = "Perspective",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (cropPanelMode == CropPanelMode.PERSPECTIVE) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    if (isAutoDetecting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = if (cropPanelMode == CropPanelMode.PERSPECTIVE) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = FilterChipDefaults.filterChipElevation(0.dp),
                border = null
            )

            if (cropPanelMode == CropPanelMode.RECTANGLE) {
                CropAspectRatio.entries.forEach { ratioPreset ->
                    val isSelected = selectedRatio == ratioPreset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onRatioSelected(ratioPreset)
                            when (ratioPreset) {
                                CropAspectRatio.FREE -> {}
                                CropAspectRatio.ORIGINAL -> onResetCrop()
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
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        elevation = FilterChipDefaults.filterChipElevation(0.dp),
                        border = null
                    )
                }
            } else {
                FilterChip(
                    selected = false,
                    onClick = onResetPerspective,
                    enabled = !stagedPerspectiveQuad.isDefault,
                    label = {
                        Text(
                            text = "Reset Corners",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    elevation = FilterChipDefaults.filterChipElevation(0.dp),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Action Controls & Page Navigation (Arrows pinned outside scroll, rectangular buttons scrollable in center)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left fixed arrow (Previous page)
            FilledTonalIconButton(
                onClick = onPreviousPage,
                enabled = currentPageIndex > 0 && !isReordering,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous page",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Scrollable container for rectangular action buttons
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onRotatePage,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate right",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rotate Right",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FilledTonalButton(
                        onClick = onRotatePageLeft,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "Rotate left",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rotate Left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (totalPages > 1) {
                        Box {
                            FilledTonalButton(
                                onClick = { if (!isReordering) isPagePickerVisible = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = "Move to page",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentPageIndex + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = isPagePickerVisible,
                                onDismissRequest = { isPagePickerVisible = false }
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
                                            isPagePickerVisible = false
                                            onMovePage(currentPageIndex, targetIndex)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right fixed arrow (Next page)
            FilledTonalIconButton(
                onClick = onNextPage,
                enabled = currentPageIndex < totalPages - 1 && !isReordering,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next page",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
