package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.DocumentFilter
import kotlin.math.roundToInt

/**
 * 2x2 grid representing quick-access pinned folders and an "Other" action card,
 * inspired by Google Photos collections layout.
 *
 * Displays up to 3 pinned folders (e.g., Favorites, Work, Study) plus 1 dedicated card
 * that triggers the full folder management bottom sheet.
 *
 * Supports long-press drag-and-drop to reorder pinned cards, and a contextual three-dot
 * options menu to rename or delete custom folders.
 */
@Composable
fun FolderGridSection(
    pinnedFolders: List<String>,
    recentPdfs: List<RecentPdf>,
    totalFoldersCount: Int,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit,
    onOpenAllFolders: () -> Unit,
    onRenameFolder: (String) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onReorderPinnedFolders: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activePinned = pinnedFolders.take(3)
    val favoriteCount = recentPdfs.count { it.isFavorite }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    val spacingPx = with(density) { 10.dp.toPx() }

    fun calculateSlotCenter(slotIndex: Int): Offset {
        val col = slotIndex % 2
        val row = slotIndex / 2
        val cx = col * (cardWidthPx + spacingPx) + (cardWidthPx / 2f)
        val cy = row * (cardHeightPx + spacingPx) + (cardHeightPx / 2f)
        return Offset(cx, cy)
    }

    val targetDropIndex = remember(draggedIndex, dragOffset, cardWidthPx, cardHeightPx, activePinned.size) {
        if (draggedIndex in activePinned.indices && cardWidthPx > 0f && cardHeightPx > 0f) {
            val draggedCenter = calculateSlotCenter(draggedIndex) + dragOffset
            activePinned.indices.minByOrNull { slot ->
                val slotCenter = calculateSlotCenter(slot)
                val dx = draggedCenter.x - slotCenter.x
                val dy = draggedCenter.y - slotCenter.y
                dx * dx + dy * dy
            } ?: draggedIndex
        } else {
            -1
        }
    }

    val handleEndDrag: () -> Unit = {
        if (draggedIndex in activePinned.indices && targetDropIndex in activePinned.indices && targetDropIndex != draggedIndex) {
            val reordered = activePinned.toMutableList().apply {
                val item = removeAt(draggedIndex)
                add(targetDropIndex, item)
            }
            val fullList = reordered + pinnedFolders.drop(activePinned.size)
            onReorderPinnedFolders(fullList)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        draggedIndex = -1
        dragOffset = Offset.Zero
    }

    val handleCancelDrag: () -> Unit = {
        draggedIndex = -1
        dragOffset = Offset.Zero
    }

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
                    isCustomFolder = !firstFolder.equals("Favorites", ignoreCase = true),
                    onRename = { onRenameFolder(firstFolder) },
                    onDelete = { onDeleteFolder(firstFolder) },
                    isDraggable = activePinned.size > 1,
                    isDragging = draggedIndex == 0,
                    isTargetDrop = targetDropIndex == 0 && draggedIndex != 0,
                    dragOffset = if (draggedIndex == 0) dragOffset else Offset.Zero,
                    onStartDrag = { draggedIndex = 0; dragOffset = Offset.Zero },
                    onDragDelta = { delta -> dragOffset += delta },
                    onEndDrag = handleEndDrag,
                    onCancelDrag = handleCancelDrag,
                    onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
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
                    isCustomFolder = !secondFolder.equals("Favorites", ignoreCase = true),
                    onRename = { onRenameFolder(secondFolder) },
                    onDelete = { onDeleteFolder(secondFolder) },
                    isDraggable = activePinned.size > 1,
                    isDragging = draggedIndex == 1,
                    isTargetDrop = targetDropIndex == 1 && draggedIndex != 1,
                    dragOffset = if (draggedIndex == 1) dragOffset else Offset.Zero,
                    onStartDrag = { draggedIndex = 1; dragOffset = Offset.Zero },
                    onDragDelta = { delta -> dragOffset += delta },
                    onEndDrag = handleEndDrag,
                    onCancelDrag = handleCancelDrag,
                    onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
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
                    isDraggable = false,
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
                        isCustomFolder = !thirdFolder.equals("Favorites", ignoreCase = true),
                        onRename = { onRenameFolder(thirdFolder) },
                        onDelete = { onDeleteFolder(thirdFolder) },
                        isDraggable = activePinned.size > 1,
                        isDragging = draggedIndex == 2,
                        isTargetDrop = targetDropIndex == 2 && draggedIndex != 2,
                        dragOffset = if (draggedIndex == 2) dragOffset else Offset.Zero,
                        onStartDrag = { draggedIndex = 2; dragOffset = Offset.Zero },
                        onDragDelta = { delta -> dragOffset += delta },
                        onEndDrag = handleEndDrag,
                        onCancelDrag = handleCancelDrag,
                        onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
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
                    isDraggable = false,
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
    modifier: Modifier = Modifier,
    isCustomFolder: Boolean = false,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isDraggable: Boolean = false,
    isDragging: Boolean = false,
    isTargetDrop: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    onStartDrag: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onEndDrag: () -> Unit = {},
    onCancelDrag: () -> Unit = {},
    onCardPositioned: (width: Float, height: Float) -> Unit = { _, _ -> }
) {
    val containerColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = when {
        isDragging -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isTargetDrop -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else -> null
    }

    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .height(64.dp)
            .onGloballyPositioned { coordinates ->
                onCardPositioned(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .then(
                if (isDragging) {
                    Modifier
                        .zIndex(30f)
                        .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                        .scale(1.04f)
                } else if (isTargetDrop) {
                    Modifier
                        .zIndex(5f)
                        .scale(0.98f)
                } else {
                    Modifier.zIndex(1f)
                }
            )
            .then(
                if (isDraggable) {
                    Modifier.pointerInput(title) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartDrag()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount)
                            },
                            onDragEnd = { onEndDrag() },
                            onDragCancel = { onCancelDrag() }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !isDragging, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = if (isCustomFolder) 4.dp else 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected || isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

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
                    color = if (isSelected || isDragging) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCustomFolder && (onRename != null || onDelete != null)) {
                Box {
                    var isMenuExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Folder options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (onRename != null) {
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
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
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
