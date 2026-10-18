package com.leshoraa.scanorea.features.recentpdfs.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.DocumentFilter
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_PINNED_FOLDERS = 5
private const val CARD_LIFT_SCALE = 1.04f
private const val CARD_RESTING_SCALE = 1.0f
private const val COLOR_TRANSITION_DURATION_MS = 200
private const val Z_INDEX_DRAGGING = 100f
private const val Z_INDEX_SHIFTED = 5f
private const val Z_INDEX_RESTING = 1f

private val CARD_CORNER_RADIUS = 16.dp
private val CARD_CONTAINER_HEIGHT = 64.dp
private val CARD_GRID_SPACING = 10.dp
private val CARD_ACTIVE_BORDER_WIDTH = 2.dp
private val CARD_DRAG_SHADOW_ELEVATION = 8.dp
private val CARD_RESTING_SHADOW_ELEVATION = 0.dp
private val FOLDER_ICON_SIZE = 24.dp
private val MORE_OPTIONS_ICON_SIZE = 20.dp
private val MORE_OPTIONS_BUTTON_SIZE = 44.dp

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
    onUnpinFolder: (String) -> Unit = {},
    onReorderPinnedFolders: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var localPinnedFolders by remember(pinnedFolders) { mutableStateOf(pinnedFolders) }
    val activePinned = localPinnedFolders.take(MAX_VISIBLE_PINNED_FOLDERS)
    val favoriteCount = remember(recentPdfs) { recentPdfs.count { it.isFavorite } }
    val folderCountsMap = remember(recentPdfs) {
        val map = mutableMapOf<String, Int>()
        recentPdfs.forEach { pdf ->
            pdf.folders.forEach { f ->
                val key = f.lowercase()
                map[key] = (map[key] ?: 0) + 1
            }
        }
        map
    }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var targetDropIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isSettling by remember { mutableStateOf(false) }
    val dropAnimOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val currentActivePinned by rememberUpdatedState(activePinned)
    val currentLocalPinnedFolders by rememberUpdatedState(localPinnedFolders)
    val currentOnReorderPinnedFolders by rememberUpdatedState(onReorderPinnedFolders)

    val performDropHaptic: () -> Unit = remember(haptic, view) {
        {
            val performed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else false
            if (!performed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val spacingPx = with(density) { CARD_GRID_SPACING.toPx() }
        val cardHeightPx = with(density) { CARD_CONTAINER_HEIGHT.toPx() }
        val totalWidthPx = constraints.maxWidth.toFloat()
        val cardWidthPx = ((totalWidthPx - spacingPx) / 2f).coerceAtLeast(1f)

        val handleStartDrag: (Int) -> Unit = remember {
            { index ->
                if (!isSettling) {
                    draggedIndex = index
                    targetDropIndex = index
                    dragOffset = Offset.Zero
                }
            }
        }

        val handleDragDelta: (Offset) -> Unit = remember(cardWidthPx, cardHeightPx, spacingPx) {
            { delta ->
                if (!isSettling && draggedIndex != -1) {
                    dragOffset += delta
                    val newTarget = FolderGridDragCalculator.determineTargetDropIndex(
                        draggedIndex = draggedIndex,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        itemCount = currentActivePinned.size
                    )
                    if (newTarget != targetDropIndex && newTarget in currentActivePinned.indices) {
                        targetDropIndex = newTarget
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
        }

        val handleEndDrag: () -> Unit = remember(cardWidthPx, cardHeightPx, spacingPx, coroutineScope) {
            {
                val currentDragged = draggedIndex
                val finalTarget = if (targetDropIndex in currentActivePinned.indices && targetDropIndex != currentDragged) {
                    targetDropIndex
                } else {
                    FolderGridDragCalculator.determineTargetDropIndex(
                        draggedIndex = currentDragged,
                        dragOffset = dragOffset,
                        cardWidthPx = cardWidthPx,
                        cardHeightPx = cardHeightPx,
                        spacingPx = spacingPx,
                        itemCount = currentActivePinned.size
                    )
                }

                if (currentDragged in currentActivePinned.indices) {
                    val targetOffset = if (finalTarget in currentActivePinned.indices && finalTarget != currentDragged) {
                        FolderGridDragCalculator.calculateSlotShift(
                            fromSlot = currentDragged,
                            toSlot = finalTarget,
                            cardWidthPx = cardWidthPx,
                            cardHeightPx = cardHeightPx,
                            spacingPx = spacingPx
                        )
                    } else {
                        Offset.Zero
                    }

                    isSettling = true
                    coroutineScope.launch {
                        try {
                            dropAnimOffset.snapTo(dragOffset)
                            dropAnimOffset.animateTo(
                                targetValue = targetOffset,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            performDropHaptic()

                            val reorderedList = if (finalTarget in currentActivePinned.indices && finalTarget != currentDragged) {
                                val reordered = FolderGridDragCalculator.reorderList(currentActivePinned, currentDragged, finalTarget)
                                reordered + currentLocalPinnedFolders.drop(currentActivePinned.size)
                            } else null

                            draggedIndex = -1
                            targetDropIndex = -1
                            dragOffset = Offset.Zero
                            isSettling = false

                            if (reorderedList != null) {
                                localPinnedFolders = reorderedList
                                currentOnReorderPinnedFolders(reorderedList)
                            }
                        } finally {
                            draggedIndex = -1
                            targetDropIndex = -1
                            dragOffset = Offset.Zero
                            isSettling = false
                        }
                    }
                } else {
                    draggedIndex = -1
                    targetDropIndex = -1
                    dragOffset = Offset.Zero
                    isSettling = false
                }
            }
        }

        val handleCancelDrag: () -> Unit = remember(coroutineScope) {
            {
                if (!isSettling) {
                    val currentDragged = draggedIndex
                    if (currentDragged in currentActivePinned.indices) {
                        isSettling = true
                        coroutineScope.launch {
                            try {
                                dropAnimOffset.snapTo(dragOffset)
                                dropAnimOffset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            } finally {
                                draggedIndex = -1
                                targetDropIndex = -1
                                dragOffset = Offset.Zero
                                isSettling = false
                            }
                        }
                    } else {
                        draggedIndex = -1
                        targetDropIndex = -1
                        dragOffset = Offset.Zero
                        isSettling = false
                    }
                }
            }
        }

        val isAnyDragging = draggedIndex != -1 || isSettling
        val dragOffsetProvider = remember {
            {
                if (isSettling) dropAnimOffset.value else dragOffset
            }
        }

        val activeDragRow = if (draggedIndex != -1) draggedIndex / FolderGridDragCalculator.GRID_COLUMNS else -1

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Slots 0 & 1 (or Slot 0 + Other if only 1 item, or Other + Spacer if 0 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (activeDragRow == 0) 100f else 1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val firstFolder = activePinned.getOrNull(0)
                if (firstFolder != null) {
                    key(firstFolder) {
                        PinnedFolderGridCard(
                            slotIndex = 0,
                            folderName = firstFolder,
                            favoriteCount = favoriteCount,
                            folderCountsMap = folderCountsMap,
                            currentFilter = currentFilter,
                            onSelectFilter = onSelectFilter,
                            onRenameFolder = onRenameFolder,
                            onDeleteFolder = onDeleteFolder,
                            onUnpinFolder = onUnpinFolder,
                            activePinnedSize = activePinned.size,
                            draggedIndex = draggedIndex,
                            targetDropIndex = targetDropIndex,
                            dragOffsetProvider = dragOffsetProvider,
                            cardWidthPx = cardWidthPx,
                            cardHeightPx = cardHeightPx,
                            spacingPx = spacingPx,
                            isAnyItemDragging = isAnyDragging,
                            isSettling = isSettling,
                            onStartDrag = handleStartDrag,
                            onDragDelta = handleDragDelta,
                            onEndDrag = handleEndDrag,
                            onCancelDrag = handleCancelDrag,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    key("action_other_card") {
                        OtherFolderCard(
                            totalFoldersCount = totalFoldersCount,
                            isAnyItemDragging = isAnyDragging,
                            onClick = onOpenAllFolders,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val secondFolder = activePinned.getOrNull(1)
                if (secondFolder != null) {
                    key(secondFolder) {
                        PinnedFolderGridCard(
                            slotIndex = 1,
                            folderName = secondFolder,
                            favoriteCount = favoriteCount,
                            folderCountsMap = folderCountsMap,
                            currentFilter = currentFilter,
                            onSelectFilter = onSelectFilter,
                            onRenameFolder = onRenameFolder,
                            onDeleteFolder = onDeleteFolder,
                            onUnpinFolder = onUnpinFolder,
                            activePinnedSize = activePinned.size,
                            draggedIndex = draggedIndex,
                            targetDropIndex = targetDropIndex,
                            dragOffsetProvider = dragOffsetProvider,
                            cardWidthPx = cardWidthPx,
                            cardHeightPx = cardHeightPx,
                            spacingPx = spacingPx,
                            isAnyItemDragging = isAnyDragging,
                            isSettling = isSettling,
                            onStartDrag = handleStartDrag,
                            onDragDelta = handleDragDelta,
                            onEndDrag = handleEndDrag,
                            onCancelDrag = handleCancelDrag,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (firstFolder != null) {
                    key("action_other_card") {
                        OtherFolderCard(
                            totalFoldersCount = totalFoldersCount,
                            isAnyItemDragging = isAnyDragging,
                            onClick = onOpenAllFolders,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // When 2 or 3 items: 2 rows layout
            if (activePinned.size in 2..3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (activeDragRow == 1) 100f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val thirdFolder = activePinned.getOrNull(2)
                    if (thirdFolder != null) {
                        key(thirdFolder) {
                            PinnedFolderGridCard(
                                slotIndex = 2,
                                folderName = thirdFolder,
                                favoriteCount = favoriteCount,
                                folderCountsMap = folderCountsMap,
                                currentFilter = currentFilter,
                                onSelectFilter = onSelectFilter,
                                onRenameFolder = onRenameFolder,
                                onDeleteFolder = onDeleteFolder,
                                onUnpinFolder = onUnpinFolder,
                                activePinnedSize = activePinned.size,
                                draggedIndex = draggedIndex,
                                targetDropIndex = targetDropIndex,
                                dragOffsetProvider = dragOffsetProvider,
                                cardWidthPx = cardWidthPx,
                                cardHeightPx = cardHeightPx,
                                spacingPx = spacingPx,
                                isAnyItemDragging = isAnyDragging,
                                isSettling = isSettling,
                                onStartDrag = handleStartDrag,
                                onDragDelta = handleDragDelta,
                                onEndDrag = handleEndDrag,
                                onCancelDrag = handleCancelDrag,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        key("action_other_card") {
                            OtherFolderCard(
                                totalFoldersCount = totalFoldersCount,
                                isAnyItemDragging = isAnyDragging,
                                onClick = onOpenAllFolders,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        key("action_other_card") {
                            OtherFolderCard(
                                totalFoldersCount = totalFoldersCount,
                                isAnyItemDragging = isAnyDragging,
                                onClick = onOpenAllFolders,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else if (activePinned.size >= 4) {
                // Row 2: Slots 2 & 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (activeDragRow == 1) 100f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val thirdFolder = activePinned.getOrNull(2)
                    if (thirdFolder != null) {
                        key(thirdFolder) {
                            PinnedFolderGridCard(
                                slotIndex = 2,
                                folderName = thirdFolder,
                                favoriteCount = favoriteCount,
                                folderCountsMap = folderCountsMap,
                                currentFilter = currentFilter,
                                onSelectFilter = onSelectFilter,
                                onRenameFolder = onRenameFolder,
                                onDeleteFolder = onDeleteFolder,
                                onUnpinFolder = onUnpinFolder,
                                activePinnedSize = activePinned.size,
                                draggedIndex = draggedIndex,
                                targetDropIndex = targetDropIndex,
                                dragOffsetProvider = dragOffsetProvider,
                                cardWidthPx = cardWidthPx,
                                cardHeightPx = cardHeightPx,
                                spacingPx = spacingPx,
                                isAnyItemDragging = isAnyDragging,
                                isSettling = isSettling,
                                onStartDrag = handleStartDrag,
                                onDragDelta = handleDragDelta,
                                onEndDrag = handleEndDrag,
                                onCancelDrag = handleCancelDrag,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val fourthFolder = activePinned.getOrNull(3)
                    if (fourthFolder != null) {
                        key(fourthFolder) {
                            PinnedFolderGridCard(
                                slotIndex = 3,
                                folderName = fourthFolder,
                                favoriteCount = favoriteCount,
                                folderCountsMap = folderCountsMap,
                                currentFilter = currentFilter,
                                onSelectFilter = onSelectFilter,
                                onRenameFolder = onRenameFolder,
                                onDeleteFolder = onDeleteFolder,
                                onUnpinFolder = onUnpinFolder,
                                activePinnedSize = activePinned.size,
                                draggedIndex = draggedIndex,
                                targetDropIndex = targetDropIndex,
                                dragOffsetProvider = dragOffsetProvider,
                                cardWidthPx = cardWidthPx,
                                cardHeightPx = cardHeightPx,
                                spacingPx = spacingPx,
                                isAnyItemDragging = isAnyDragging,
                                isSettling = isSettling,
                                onStartDrag = handleStartDrag,
                                onDragDelta = handleDragDelta,
                                onEndDrag = handleEndDrag,
                                onCancelDrag = handleCancelDrag,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Row 3: Slot 4 & Other (Slot 5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (activeDragRow == 2) 100f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val fifthFolder = activePinned.getOrNull(4)
                    if (fifthFolder != null) {
                        key(fifthFolder) {
                            PinnedFolderGridCard(
                                slotIndex = 4,
                                folderName = fifthFolder,
                                favoriteCount = favoriteCount,
                                folderCountsMap = folderCountsMap,
                                currentFilter = currentFilter,
                                onSelectFilter = onSelectFilter,
                                onRenameFolder = onRenameFolder,
                                onDeleteFolder = onDeleteFolder,
                                onUnpinFolder = onUnpinFolder,
                                activePinnedSize = activePinned.size,
                                draggedIndex = draggedIndex,
                                targetDropIndex = targetDropIndex,
                                dragOffsetProvider = dragOffsetProvider,
                                cardWidthPx = cardWidthPx,
                                cardHeightPx = cardHeightPx,
                                spacingPx = spacingPx,
                                isAnyItemDragging = isAnyDragging,
                                isSettling = isSettling,
                                onStartDrag = handleStartDrag,
                                onDragDelta = handleDragDelta,
                                onEndDrag = handleEndDrag,
                                onCancelDrag = handleCancelDrag,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        key("action_other_card") {
                            OtherFolderCard(
                                totalFoldersCount = totalFoldersCount,
                                isAnyItemDragging = isAnyDragging,
                                onClick = onOpenAllFolders,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        key("action_other_card") {
                            OtherFolderCard(
                                totalFoldersCount = totalFoldersCount,
                                isAnyItemDragging = isAnyDragging,
                                onClick = onOpenAllFolders,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
    folderCountsMap: Map<String, Int>,
    currentFilter: DocumentFilter,
    onSelectFilter: (DocumentFilter) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onUnpinFolder: (String) -> Unit,
    activePinnedSize: Int,
    draggedIndex: Int,
    targetDropIndex: Int,
    dragOffsetProvider: () -> Offset,
    cardWidthPx: Float,
    cardHeightPx: Float,
    spacingPx: Float,
    isAnyItemDragging: Boolean,
    isSettling: Boolean = false,
    onStartDrag: (Int) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetSlot = FolderGridDragCalculator.calculateTargetSlot(slotIndex, draggedIndex, targetDropIndex)
    val shift = FolderGridDragCalculator.calculateSlotShift(slotIndex, targetSlot, cardWidthPx, cardHeightPx, spacingPx)
    val isCurrentDragging = draggedIndex == slotIndex
    val isSlotShifted = isAnyItemDragging && !isCurrentDragging && (targetSlot != slotIndex)

    val animatedShiftX = animateFloatAsState(
        targetValue = if (isCurrentDragging || !isAnyItemDragging) 0f else shift.x,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folderShiftX_$folderName"
    )
    val animatedShiftY = animateFloatAsState(
        targetValue = if (isCurrentDragging || !isAnyItemDragging) 0f else shift.y,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folderShiftY_$folderName"
    )

    val shiftOffsetProvider = {
        Offset(animatedShiftX.value, animatedShiftY.value)
    }

    FolderGridCard(
        title = folderName,
        countLabel = calculateFolderItemCount(folderName, favoriteCount, folderCountsMap),
        icon = resolveFolderIcon(folderName),
        isSelected = isFolderSelected(folderName, currentFilter),
        onClick = { toggleFolderSelection(folderName, currentFilter, onSelectFilter) },
        isCustomFolder = !folderName.equals("Favorites", ignoreCase = true),
        onRename = { onRenameFolder(folderName) },
        onDelete = { onDeleteFolder(folderName) },
        onUnpin = { onUnpinFolder(folderName) },
        isDraggable = activePinnedSize > 1,
        isDragging = isCurrentDragging,
        isSettling = isSettling,
        dragOffsetProvider = dragOffsetProvider,
        shiftOffsetProvider = shiftOffsetProvider,
        isSlotShifted = isSlotShifted,
        isAnyItemDragging = isAnyItemDragging,
        onStartDrag = { onStartDrag(slotIndex) },
        onDragDelta = onDragDelta,
        onEndDrag = onEndDrag,
        onCancelDrag = onCancelDrag,
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
        icon = Icons.Filled.FolderOpen,
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
    onUnpin: (() -> Unit)? = null,
    isDraggable: Boolean = false,
    isDragging: Boolean = false,
    isSettling: Boolean = false,
    dragOffsetProvider: () -> Offset = { Offset.Zero },
    shiftOffsetProvider: () -> Offset = { Offset.Zero },
    isSlotShifted: Boolean = false,
    isAnyItemDragging: Boolean = false,
    onStartDrag: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onEndDrag: () -> Unit = {},
    onCancelDrag: () -> Unit = {}
) {
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnCancelDrag by rememberUpdatedState(onCancelDrag)

    val isLifted = isDragging && !isSettling

    val animatedScale = animateFloatAsState(
        targetValue = if (isLifted) CARD_LIFT_SCALE else CARD_RESTING_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale_$title"
    )

    val animatedElevation = animateDpAsState(
        targetValue = if (isLifted) CARD_DRAG_SHADOW_ELEVATION else CARD_RESTING_SHADOW_ELEVATION,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation_$title"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isLifted || isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        animationSpec = tween(durationMillis = COLOR_TRANSITION_DURATION_MS),
        label = "cardContainerColor_$title"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isLifted || isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = COLOR_TRANSITION_DURATION_MS),
        label = "cardContentColor_$title"
    )

    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = onClick,
        enabled = !isAnyItemDragging && !isSettling,
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        color = containerColor,
        contentColor = contentColor,
        border = null,
        shadowElevation = CARD_RESTING_SHADOW_ELEVATION,
        tonalElevation = CARD_RESTING_SHADOW_ELEVATION,
        modifier = modifier
            .height(CARD_CONTAINER_HEIGHT)
            .zIndex(
                when {
                    isDragging -> Z_INDEX_DRAGGING
                    isSlotShifted -> Z_INDEX_SHIFTED
                    else -> Z_INDEX_RESTING
                }
            )
            .semantics {
                role = Role.Button
                selected = isSelected
                contentDescription = "$title folder, $countLabel"
            }
            .graphicsLayer {
                if (isDragging) {
                    val offset = dragOffsetProvider()
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = animatedScale.value
                    scaleY = animatedScale.value
                    shadowElevation = animatedElevation.value.toPx()
                } else if (isAnyItemDragging) {
                    val shift = shiftOffsetProvider()
                    translationX = shift.x
                    translationY = shift.y
                    scaleX = 1f
                    scaleY = 1f
                } else {
                    translationX = 0f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
            .then(
                if (isDraggable) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnStartDrag()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDragDelta(dragAmount)
                            },
                            onDragEnd = {
                                currentOnEndDrag()
                            },
                            onDragCancel = {
                                currentOnCancelDrag()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val hasOptionsMenu = onUnpin != null || (isCustomFolder && (onRename != null || onDelete != null))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = if (hasOptionsMenu) 4.dp else 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    isSelected || isLifted -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                },
                modifier = Modifier.size(FOLDER_ICON_SIZE)
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
                        isSelected || isLifted -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasOptionsMenu) {
                Box {
                    var isMenuExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { isMenuExpanded = true },
                        enabled = !isAnyItemDragging && !isSettling,
                        modifier = Modifier.size(MORE_OPTIONS_BUTTON_SIZE)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options for $title folder",
                            tint = if (isSelected || isLifted) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(MORE_OPTIONS_ICON_SIZE)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (onUnpin != null) {
                            DropdownMenuItem(
                                text = { Text("Unpin") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onUnpin()
                                }
                            )
                        }
                        if (isCustomFolder && onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
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
                        if (isCustomFolder && onDelete != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
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
        Icons.Filled.Folder
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
    folderCountsMap: Map<String, Int>
): String {
    val count = if (folderName.equals("Favorites", ignoreCase = true)) {
        favoriteCount
    } else {
        folderCountsMap[folderName.lowercase()] ?: 0
    }
    return if (count == 1) "1 document" else "$count documents"
}
