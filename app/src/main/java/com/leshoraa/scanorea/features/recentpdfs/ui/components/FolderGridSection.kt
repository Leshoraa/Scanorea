package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.DocumentFilter

/**
 * 3x2 grid representing quick-access pinned folders and an "Other" action card,
 * inspired by Google Photos collections layout.
 *
 * Displays up to 5 pinned folders (e.g., Favorites, Work, Study, Personal, Projects) plus 1 dedicated card
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
    var localPinnedFolders by remember(pinnedFolders) { mutableStateOf(pinnedFolders) }
    val activePinned = localPinnedFolders.take(5)
    val favoriteCount = recentPdfs.count { it.isFavorite }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    val spacingPx = with(density) { 10.dp.toPx() }

    val targetDropIndex = remember(draggedIndex, dragOffset, cardWidthPx, cardHeightPx, activePinned.size) {
        FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = draggedIndex,
            dragOffset = dragOffset,
            cardWidthPx = cardWidthPx,
            cardHeightPx = cardHeightPx,
            spacingPx = spacingPx,
            itemCount = activePinned.size
        )
    }

    var previousTargetIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(targetDropIndex) {
        if (targetDropIndex != -1 && targetDropIndex != previousTargetIndex && draggedIndex != -1 && targetDropIndex != draggedIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        previousTargetIndex = targetDropIndex
    }

    val handleEndDrag: () -> Unit = {
        if (draggedIndex in activePinned.indices && targetDropIndex in activePinned.indices && targetDropIndex != draggedIndex) {
            val reordered = FolderGridDragCalculator.reorderList(activePinned, draggedIndex, targetDropIndex)
            val fullList = reordered + localPinnedFolders.drop(activePinned.size)
            localPinnedFolders = fullList
            onReorderPinnedFolders(fullList)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        draggedIndex = -1
        dragOffset = Offset.Zero
        previousTargetIndex = -1
    }

    val handleCancelDrag: () -> Unit = {
        draggedIndex = -1
        dragOffset = Offset.Zero
        previousTargetIndex = -1
    }

    val isAnyDragging = draggedIndex != -1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Slots 0 & 1 (or Slot 0 + Other if only 1 item, or Other + Spacer if 0 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val firstFolder = activePinned.getOrNull(0)
            if (firstFolder != null) {
                PinnedFolderGridCard(
                    slotIndex = 0,
                    folderName = firstFolder,
                    favoriteCount = favoriteCount,
                    recentPdfs = recentPdfs,
                    currentFilter = currentFilter,
                    onSelectFilter = onSelectFilter,
                    onRenameFolder = onRenameFolder,
                    onDeleteFolder = onDeleteFolder,
                    activePinnedSize = activePinned.size,
                    draggedIndex = draggedIndex,
                    targetDropIndex = targetDropIndex,
                    dragOffset = dragOffset,
                    cardWidthPx = cardWidthPx,
                    cardHeightPx = cardHeightPx,
                    spacingPx = spacingPx,
                    isAnyItemDragging = isAnyDragging,
                    onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                    onDragDelta = { delta -> dragOffset += delta },
                    onEndDrag = handleEndDrag,
                    onCancelDrag = handleCancelDrag,
                    onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                    modifier = Modifier.weight(1f)
                )
            } else {
                OtherFolderCard(
                    totalFoldersCount = totalFoldersCount,
                    isAnyItemDragging = isAnyDragging,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            }

            val secondFolder = activePinned.getOrNull(1)
            if (secondFolder != null) {
                PinnedFolderGridCard(
                    slotIndex = 1,
                    folderName = secondFolder,
                    favoriteCount = favoriteCount,
                    recentPdfs = recentPdfs,
                    currentFilter = currentFilter,
                    onSelectFilter = onSelectFilter,
                    onRenameFolder = onRenameFolder,
                    onDeleteFolder = onDeleteFolder,
                    activePinnedSize = activePinned.size,
                    draggedIndex = draggedIndex,
                    targetDropIndex = targetDropIndex,
                    dragOffset = dragOffset,
                    cardWidthPx = cardWidthPx,
                    cardHeightPx = cardHeightPx,
                    spacingPx = spacingPx,
                    isAnyItemDragging = isAnyDragging,
                    onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                    onDragDelta = { delta -> dragOffset += delta },
                    onEndDrag = handleEndDrag,
                    onCancelDrag = handleCancelDrag,
                    onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                    modifier = Modifier.weight(1f)
                )
            } else if (firstFolder != null) {
                OtherFolderCard(
                    totalFoldersCount = totalFoldersCount,
                    isAnyItemDragging = isAnyDragging,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // When 2 or 3 items: 2 rows layout
        if (activePinned.size in 2..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val thirdFolder = activePinned.getOrNull(2)
                if (thirdFolder != null) {
                    PinnedFolderGridCard(
                        slotIndex = 2,
                        folderName = thirdFolder,
                        favoriteCount = favoriteCount,
                        recentPdfs = recentPdfs,
                        currentFilter = currentFilter,
                        onSelectFilter = onSelectFilter,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        activePinnedSize = activePinned.size,
                        draggedIndex = draggedIndex,
                        targetDropIndex = targetDropIndex,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        isAnyItemDragging = isAnyDragging,
                        onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                        onDragDelta = { delta -> dragOffset += delta },
                        onEndDrag = handleEndDrag,
                        onCancelDrag = handleCancelDrag,
                        onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                OtherFolderCard(
                    totalFoldersCount = totalFoldersCount,
                    isAnyItemDragging = isAnyDragging,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (activePinned.size >= 4) {
            // Row 2: Slots 2 & 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val thirdFolder = activePinned.getOrNull(2)
                if (thirdFolder != null) {
                    PinnedFolderGridCard(
                        slotIndex = 2,
                        folderName = thirdFolder,
                        favoriteCount = favoriteCount,
                        recentPdfs = recentPdfs,
                        currentFilter = currentFilter,
                        onSelectFilter = onSelectFilter,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        activePinnedSize = activePinned.size,
                        draggedIndex = draggedIndex,
                        targetDropIndex = targetDropIndex,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        isAnyItemDragging = isAnyDragging,
                        onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                        onDragDelta = { delta -> dragOffset += delta },
                        onEndDrag = handleEndDrag,
                        onCancelDrag = handleCancelDrag,
                        onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                        modifier = Modifier.weight(1f)
                    )
                }

                val fourthFolder = activePinned.getOrNull(3)
                if (fourthFolder != null) {
                    PinnedFolderGridCard(
                        slotIndex = 3,
                        folderName = fourthFolder,
                        favoriteCount = favoriteCount,
                        recentPdfs = recentPdfs,
                        currentFilter = currentFilter,
                        onSelectFilter = onSelectFilter,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        activePinnedSize = activePinned.size,
                        draggedIndex = draggedIndex,
                        targetDropIndex = targetDropIndex,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        isAnyItemDragging = isAnyDragging,
                        onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                        onDragDelta = { delta -> dragOffset += delta },
                        onEndDrag = handleEndDrag,
                        onCancelDrag = handleCancelDrag,
                        onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 3: Slot 4 & Other (Slot 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val fifthFolder = activePinned.getOrNull(4)
                if (fifthFolder != null) {
                    PinnedFolderGridCard(
                        slotIndex = 4,
                        folderName = fifthFolder,
                        favoriteCount = favoriteCount,
                        recentPdfs = recentPdfs,
                        currentFilter = currentFilter,
                        onSelectFilter = onSelectFilter,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        activePinnedSize = activePinned.size,
                        draggedIndex = draggedIndex,
                        targetDropIndex = targetDropIndex,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        isAnyItemDragging = isAnyDragging,
                        onStartDrag = { idx -> draggedIndex = idx; dragOffset = Offset.Zero },
                        onDragDelta = { delta -> dragOffset += delta },
                        onEndDrag = handleEndDrag,
                        onCancelDrag = handleCancelDrag,
                        onCardPositioned = { w, h -> cardWidthPx = w; cardHeightPx = h },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                OtherFolderCard(
                    totalFoldersCount = totalFoldersCount,
                    isAnyItemDragging = isAnyDragging,
                    onClick = onOpenAllFolders,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Pinned folder grid card item that encapsulates dynamic spring-animated
 * slot shift displacement during drag-and-drop reordering.
 */
@Composable
private fun PinnedFolderGridCard(
    slotIndex: Int,
    folderName: String,
    favoriteCount: Int,
    recentPdfs: List<RecentPdf>,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    activePinnedSize: Int,
    draggedIndex: Int,
    targetDropIndex: Int,
    dragOffset: Offset,
    cardWidthPx: Float,
    cardHeightPx: Float,
    spacingPx: Float,
    isAnyItemDragging: Boolean,
    onStartDrag: (Int) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onCardPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetSlot = FolderGridDragCalculator.calculateTargetSlot(slotIndex, draggedIndex, targetDropIndex)
    val shift = FolderGridDragCalculator.calculateSlotShift(slotIndex, targetSlot, cardWidthPx, cardHeightPx, spacingPx)
    val isCurrentDragging = draggedIndex == slotIndex

    val animatedShiftX by animateFloatAsState(
        targetValue = if (isCurrentDragging) 0f else shift.x,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "folderShiftX_$slotIndex"
    )
    val animatedShiftY by animateFloatAsState(
        targetValue = if (isCurrentDragging) 0f else shift.y,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "folderShiftY_$slotIndex"
    )

    FolderGridCard(
        title = folderName,
        countLabel = calculateFolderItemCount(folderName, favoriteCount, recentPdfs),
        icon = resolveFolderIcon(folderName),
        isSelected = isFolderSelected(folderName, currentFilter),
        onClick = { toggleFolderSelection(folderName, currentFilter, onSelectFilter) },
        isCustomFolder = !folderName.equals("Favorites", ignoreCase = true),
        onRename = { onRenameFolder(folderName) },
        onDelete = { onDeleteFolder(folderName) },
        isDraggable = activePinnedSize > 1,
        isDragging = isCurrentDragging,
        dragOffset = if (isCurrentDragging) dragOffset else Offset.Zero,
        animatedShift = Offset(animatedShiftX, animatedShiftY),
        isAnyItemDragging = isAnyItemDragging,
        onStartDrag = { onStartDrag(slotIndex) },
        onDragDelta = onDragDelta,
        onEndDrag = onEndDrag,
        onCancelDrag = onCancelDrag,
        onCardPositioned = onCardPositioned,
        modifier = modifier
    )
}

/**
 * Dedicated "Other" action card triggering the full folder management bottom sheet.
 */
@Composable
private fun OtherFolderCard(
    totalFoldersCount: Int,
    isAnyItemDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FolderGridCard(
        title = "Other",
        countLabel = "$totalFoldersCount folders",
        icon = Icons.Outlined.FolderOpen,
        isSelected = false,
        onClick = onClick,
        isDraggable = false,
        isAnyItemDragging = isAnyItemDragging,
        modifier = modifier
    )
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
    dragOffset: Offset = Offset.Zero,
    animatedShift: Offset = Offset.Zero,
    isAnyItemDragging: Boolean = false,
    onStartDrag: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onEndDrag: () -> Unit = {},
    onCancelDrag: () -> Unit = {},
    onCardPositioned: (width: Float, height: Float) -> Unit = { _, _ -> }
) {
    val containerColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val isShifted = animatedShift.x != 0f || animatedShift.y != 0f
    val borderStroke = when {
        isDragging -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
            .zIndex(
                when {
                    isDragging -> 30f
                    isShifted -> 5f
                    else -> 1f
                }
            )
            .graphicsLayer {
                if (isDragging) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    scaleX = 1.04f
                    scaleY = 1.04f
                    shadowElevation = 8.dp.toPx()
                } else {
                    translationX = animatedShift.x
                    translationY = animatedShift.y
                    scaleX = 1f
                    scaleY = 1f
                }
            }
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
            .clickable(enabled = !isAnyItemDragging, onClick = onClick)
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
                tint = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isDragging -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
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
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                        isDragging -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                        enabled = !isAnyItemDragging,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Folder options",
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
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
