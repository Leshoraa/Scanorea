package com.leshoraa.scanorea.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize

/**
 * Application settings screen providing persistence configuration for document export defaults,
 * storage destinations, and preset template navigation.
 *
 * @param destinationFolderDisplayName Display name of the active document export directory.
 * @param onChangeDestinationFolder Callback invoked to launch external directory selection.
 * @param defaultPageSize Currently configured default page dimensions.
 * @param onDefaultPageSizeChange Callback invoked when a new default page size is selected.
 * @param defaultOrientation Currently configured default page orientation.
 * @param onDefaultOrientationChange Callback invoked when a new default page orientation is selected.
 * @param defaultCompression Currently configured default image compression profile.
 * @param onDefaultCompressionChange Callback invoked when a new default compression profile is selected.
 * @param presetsCount Total count of stored conversion presets.
 * @param onNavigateToPresets Callback invoked to navigate to preset template management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    destinationFolderDisplayName: String,
    onChangeDestinationFolder: () -> Unit,
    defaultPageSize: PdfPageSize,
    onDefaultPageSizeChange: (PdfPageSize) -> Unit,
    defaultOrientation: PdfPageOrientation,
    onDefaultOrientationChange: (PdfPageOrientation) -> Unit,
    defaultCompression: CompressionProfile,
    onDefaultCompressionChange: (CompressionProfile) -> Unit,
    defaultFilter: ImageFilterType,
    onDefaultFilterChange: (ImageFilterType) -> Unit,
    presetsCount: Int,
    onNavigateToPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPaperSizeDialogOpen by remember { mutableStateOf(false) }
    var isOrientationDialogOpen by remember { mutableStateOf(false) }
    var isCompressionDialogOpen by remember { mutableStateOf(false) }
    var isFilterDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Storage & Presets
            Column {
                StockSectionHeader(title = "STORAGE & PRESETS")
                Spacer(modifier = Modifier.height(6.dp))
                StockPreferenceGroup {
                    StockPreferenceItem(
                        title = "Save destination",
                        summary = destinationFolderDisplayName,
                        leadingIcon = Icons.Outlined.Folder,
                        onClick = onChangeDestinationFolder
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    StockPreferenceItem(
                        title = "Preset templates",
                        summary = "$presetsCount saved template(s) • Dynamic date & paper rules",
                        leadingIcon = Icons.Outlined.BookmarkBorder,
                        onClick = onNavigateToPresets
                    )
                }
            }

            // Group 2: Document Defaults
            Column {
                StockSectionHeader(title = "DOCUMENT DEFAULTS")
                Spacer(modifier = Modifier.height(6.dp))
                StockPreferenceGroup {
                    StockPreferenceItem(
                        title = "Default paper size",
                        summary = defaultPageSize.label,
                        leadingIcon = Icons.Outlined.Description,
                        onClick = { isPaperSizeDialogOpen = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    StockPreferenceItem(
                        title = "Default page orientation",
                        summary = defaultOrientation.label,
                        leadingIcon = Icons.Outlined.ScreenRotation,
                        onClick = { isOrientationDialogOpen = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    StockPreferenceItem(
                        title = "Default compression & quality",
                        summary = defaultCompression.label,
                        leadingIcon = Icons.Outlined.Tune,
                        onClick = { isCompressionDialogOpen = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    StockPreferenceItem(
                        title = "Default filter mode",
                        summary = defaultFilter.label,
                        leadingIcon = Icons.Outlined.ColorLens,
                        onClick = { isFilterDialogOpen = true }
                    )
                }
            }

            // Group 3: About
            Column {
                StockSectionHeader(title = "ABOUT")
                Spacer(modifier = Modifier.height(6.dp))
                StockPreferenceGroup {
                    StockPreferenceItem(
                        title = "Scanorea",
                        summary = "Version 1.0.0 (Build 2026)",
                        leadingIcon = Icons.Outlined.Info,
                        onClick = {},
                        trailing = {}
                    )
                }
            }
        }
    }

    // Dialog for Paper Size selection
    if (isPaperSizeDialogOpen) {
        StockSingleChoiceDialog(
            title = "Default paper size",
            options = PdfPageSize.entries.map { it to it.label },
            selectedOption = defaultPageSize,
            onOptionSelected = onDefaultPageSizeChange,
            onDismissRequest = { isPaperSizeDialogOpen = false }
        )
    }

    // Dialog for Orientation selection
    if (isOrientationDialogOpen) {
        StockSingleChoiceDialog(
            title = "Default page orientation",
            options = PdfPageOrientation.entries.map { it to it.label },
            selectedOption = defaultOrientation,
            onOptionSelected = onDefaultOrientationChange,
            onDismissRequest = { isOrientationDialogOpen = false }
        )
    }

    // Dialog for Compression Profile selection
    if (isCompressionDialogOpen) {
        StockSingleChoiceDialog(
            title = "Default compression quality",
            options = CompressionProfile.entries.map { it to it.label },
            selectedOption = defaultCompression,
            onOptionSelected = onDefaultCompressionChange,
            onDismissRequest = { isCompressionDialogOpen = false }
        )
    }

    // Dialog for Filter selection
    if (isFilterDialogOpen) {
        StockSingleChoiceDialog(
            title = "Default filter mode",
            options = ImageFilterType.entries.map { it to it.label },
            selectedOption = defaultFilter,
            onOptionSelected = onDefaultFilterChange,
            onDismissRequest = { isFilterDialogOpen = false }
        )
    }
}

@Composable
private fun StockSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp)
    )
}

@Composable
private fun StockPreferenceGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun StockPreferenceItem(
    title: String,
    summary: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun <T> StockSingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selectedOption) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (option == selectedOption) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
