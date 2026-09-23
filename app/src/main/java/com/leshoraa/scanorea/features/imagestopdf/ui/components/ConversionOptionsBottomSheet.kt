package com.leshoraa.scanorea.features.imagestopdf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.util.FileSizeFormatter
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset

/**
 * Modal bottom sheet presenting PDF conversion options, destination folder selection,
 * preset management with dynamic date tokens, and the primary Convert to PDF action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversionOptionsBottomSheet(
    options: PdfConversionOptions,
    pageCount: Int,
    presets: List<ConversionPreset>,
    destinationFolderDisplayName: String,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onPageSizeChange: (PdfPageSize) -> Unit,
    pages: List<ImagePage> = emptyList(),
    onOrientationChange: (PdfPageOrientation) -> Unit,
    onCompressionProfileChange: (CompressionProfile) -> Unit,
    onPresetSelected: (ConversionPreset) -> Unit,
    onSaveNewPreset: (String, String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onChangeFolderClick: () -> Unit,
    onConvertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var presetsDropdownExpanded by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Text(
                text = "PDF Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Document Name Input with Presets Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = options.fileName,
                    onValueChange = onFileNameChange,
                    label = { Text("Document Name") },
                    placeholder = { Text("e.g., Scanned_Document or template") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { presetsDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select or manage presets"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                DropdownMenu(
                    expanded = presetsDropdownExpanded,
                    onDismissRequest = { presetsDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Saved Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = preset.fileNameTemplate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = {
                                if (preset.id != "preset_aljabar") {
                                    IconButton(
                                        onClick = { onDeletePreset(preset.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete preset",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onPresetSelected(preset)
                                presetsDropdownExpanded = false
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Save Current as New Preset...",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            presetsDropdownExpanded = false
                            showSavePresetDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Destination Save Folder Section
            Text(
                text = "Save Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeFolderClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = destinationFolderDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap to change save destination",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onChangeFolderClick,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Change")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Page Size Selection
            Text(
                text = "Paper Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PdfPageSize.entries.forEach { size ->
                    FilterChip(
                        selected = options.pageSize == size,
                        onClick = { onPageSizeChange(size) },
                        label = { Text(size.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Orientation Selection
            Text(
                text = "Page Orientation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PdfPageOrientation.entries.forEach { orientation ->
                    FilterChip(
                        selected = options.orientation == orientation,
                        onClick = { onOrientationChange(orientation) },
                        label = { Text(orientation.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Compression Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compression & Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val estimatedSize = if (pages.isNotEmpty()) {
                    options.compressionProfile.estimateSizeBytes(pages)
                } else {
                    options.compressionProfile.estimateSizeBytes(pageCount)
                }
                Text(
                    text = "Est. ~${FileSizeFormatter.format(estimatedSize)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = options.compressionProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompressionProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = options.compressionProfile == profile,
                        onClick = { onCompressionProfileChange(profile) },
                        label = { Text(profile.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Convert Button
            Button(
                onClick = onConvertClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Convert to PDF")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save as Preset") },
            text = {
                Column {
                    Text(
                        text = "Save current settings (filename template, paper size, orientation, and compression) as a quick preset.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Preset Name") },
                        placeholder = { Text("e.g., Aljabar, Homework") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            val template = options.fileName.ifBlank { "Document_{DD:MM:YYYY}" }
                            onSaveNewPreset(newPresetName.trim(), template)
                            newPresetName = ""
                            showSavePresetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
