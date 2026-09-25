package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.DocumentFilter

/**
 * 2x2 grid representing quick-access pinned folders and an "Other" action card,
 * inspired by Google Photos collections layout.
 *
 * Displays up to 3 pinned folders (e.g., Favorites, Work, Study) plus 1 dedicated card
 * that triggers the full folder management bottom sheet.
 */
@Composable
fun FolderGridSection(
    pinnedFolders: List<String>,
    recentPdfs: List<RecentPdf>,
    totalFoldersCount: Int,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit,
    onOpenAllFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePinned = pinnedFolders.take(3)
    val favoriteCount = recentPdfs.count { it.isFavorite }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: First two pinned items (or item + Other if fewer than 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val firstFolder = activePinned.getOrNull(0)
            if (firstFolder != null) {
                FolderGridCard(
                    title = firstFolder,
                    countLabel = calculateFolderItemCount(firstFolder, favoriteCount, recentPdfs),
                    icon = resolveFolderIcon(firstFolder),
                    isSelected = isFolderSelected(firstFolder, currentFilter),
                    onClick = { toggleFolderSelection(firstFolder, currentFilter, onSelectFilter) },
                    modifier = Modifier.weight(1f)
                )
            }

            val secondFolder = activePinned.getOrNull(1)
            if (secondFolder != null) {
                FolderGridCard(
                    title = secondFolder,
                    countLabel = calculateFolderItemCount(secondFolder, favoriteCount, recentPdfs),
                    icon = resolveFolderIcon(secondFolder),
                    isSelected = isFolderSelected(secondFolder, currentFilter),
                    onClick = { toggleFolderSelection(secondFolder, currentFilter, onSelectFilter) },
                    modifier = Modifier.weight(1f)
                )
            } else if (firstFolder != null) {
                // If only 1 pinned item exists, put Other next to it
                FolderGridCard(
                    title = "Other",
                    countLabel = "$totalFoldersCount folders",
                    icon = Icons.Outlined.FolderOpen,
                    isSelected = false,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Third pinned item + "Other" card (or only Other if exactly 2 pinned items)
        if (activePinned.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val thirdFolder = activePinned.getOrNull(2)
                if (thirdFolder != null) {
                    FolderGridCard(
                        title = thirdFolder,
                        countLabel = calculateFolderItemCount(thirdFolder, favoriteCount, recentPdfs),
                        icon = resolveFolderIcon(thirdFolder),
                        isSelected = isFolderSelected(thirdFolder, currentFilter),
                        onClick = { toggleFolderSelection(thirdFolder, currentFilter, onSelectFilter) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                FolderGridCard(
                    title = "Other",
                    countLabel = "$totalFoldersCount folders",
                    icon = Icons.Outlined.FolderOpen,
                    isSelected = false,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual flat tonal card representing a folder or special collection.
 */
@Composable
private fun FolderGridCard(
    title: String,
    countLabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun resolveFolderIcon(folderName: String): ImageVector {
    return if (folderName.equals("Favorites", ignoreCase = true)) {
        Icons.Filled.Star
    } else {
        Icons.Outlined.Folder
    }
}

private fun isFolderSelected(folderName: String, currentFilter: DocumentFilter): Boolean {
    return if (folderName.equals("Favorites", ignoreCase = true)) {
        currentFilter is DocumentFilter.Favorites
    } else {
        currentFilter is DocumentFilter.Category && currentFilter.name.equals(folderName, ignoreCase = true)
    }
}

private fun toggleFolderSelection(
    folderName: String,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit
) {
    val isSelected = isFolderSelected(folderName, currentFilter)
    if (isSelected) {
        // Deselect back to all documents
        onSelectFilter(DocumentFilter.All)
    } else {
        if (folderName.equals("Favorites", ignoreCase = true)) {
            onSelectFilter(DocumentFilter.Favorites)
        } else {
            onSelectFilter(DocumentFilter.Category(folderName))
        }
    }
}

private fun calculateFolderItemCount(
    folderName: String,
    favoriteCount: Int,
    recentPdfs: List<RecentPdf>
): String {
    val count = if (folderName.equals("Favorites", ignoreCase = true)) {
        favoriteCount
    } else {
        recentPdfs.count { pdf -> pdf.folders.any { it.equals(folderName, ignoreCase = true) } }
    }
    return if (count == 1) "1 document" else "$count documents"
}
