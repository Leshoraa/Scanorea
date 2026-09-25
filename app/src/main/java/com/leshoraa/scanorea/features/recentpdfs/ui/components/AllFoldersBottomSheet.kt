package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.DocumentFilter

/**
 * Modal bottom sheet allowing users to view all folders, filter documents,
 * pin or unpin folders to the 2x2 home grid (up to 3 items), and manage folder life cycle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFoldersBottomSheet(
    categories: List<String>,
    pinnedFolders: List<String>,
    recentPdfs: List<RecentPdf>,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit,
    onTogglePinFolder: (String) -> Unit,
    onCreateNewFolder: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val favoriteCount = recentPdfs.count { it.isFavorite }
    val isFavoritesPinned = pinnedFolders.any { it.equals("Favorites", ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row: Title & Create New Folder Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "All Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Max 3 pinned to quick-access grid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onCreateNewFolder,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Folder",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All Documents Option
                item {
                    AllFoldersItemRow(
                        title = "All Documents",
                        countLabel = "${recentPdfs.size} documents",
                        icon = Icons.Outlined.Description,
                        isSelected = currentFilter is DocumentFilter.All,
                        showPin = false,
                        isPinned = false,
                        onTogglePin = {},
                        onClick = {
                            onSelectFilter(DocumentFilter.All)
                            onDismiss()
                        }
                    )
                }

                // Favorites Option
                item {
                    AllFoldersItemRow(
                        title = "Favorites",
                        countLabel = "$favoriteCount documents",
                        icon = Icons.Filled.Star,
                        isSelected = currentFilter is DocumentFilter.Favorites,
                        showPin = true,
                        isPinned = isFavoritesPinned,
                        onTogglePin = { onTogglePinFolder("Favorites") },
                        onClick = {
                            onSelectFilter(DocumentFilter.Favorites)
                            onDismiss()
                        }
                    )
                }

                if (categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Custom Folders",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(categories, key = { it }) { category ->
                        val folderCount = recentPdfs.count { pdf ->
                            pdf.folders.any { it.equals(category, ignoreCase = true) }
                        }
                        val isPinned = pinnedFolders.any { it.equals(category, ignoreCase = true) }
                        val isSelected = currentFilter is DocumentFilter.Category &&
                                currentFilter.name.equals(category, ignoreCase = true)

                        AllFoldersItemRow(
                            title = category,
                            countLabel = "$folderCount documents",
                            icon = Icons.Outlined.Folder,
                            isSelected = isSelected,
                            showPin = true,
                            isPinned = isPinned,
                            onTogglePin = { onTogglePinFolder(category) },
                            onClick = {
                                onSelectFilter(DocumentFilter.Category(category))
                                onDismiss()
                            },
                            onRename = { onRenameFolder(category) },
                            onDelete = { onDeleteFolder(category) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single item row inside the All Folders sheet.
 */
@Composable
private fun AllFoldersItemRow(
    title: String,
    countLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    showPin: Boolean,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pin to 2x2 grid toggle button
            if (showPin) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin from home grid" else "Pin to home grid",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Contextual menu for Rename / Delete
            if (onRename != null && onDelete != null) {
                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Folder options",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
