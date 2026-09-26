package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.ui.geometry.Offset

/**
 * Pure calculation engine for folder grid drag-and-drop reordering.
 *
 * Encapsulates slot coordinate resolution, out-of-bounds tolerance testing,
 * prospective reorder slot displacement logic, and translation shift vectors.
 * Supports up to 3x2 grid layout (5 pinned folders + 1 "Other" action card).
 */
object FolderGridDragCalculator {

    const val GRID_COLUMNS = 2
    const val MAX_PINNED_CAPACITY = 5
    const val MAX_GRID_ROWS = 3

    private const val HORIZONTAL_TOLERANCE_FRACTION = 1.2f
    private const val VERTICAL_TOLERANCE_FRACTION = 1.8f

    /**
     * Calculates the center coordinates [Offset] of a slot index in the 2-column grid.
     * Slot 0: (Col 0, Row 0)
     * Slot 1: (Col 1, Row 0)
     * Slot 2: (Col 0, Row 1)
     * Slot 3: (Col 1, Row 1)
     * Slot 4: (Col 0, Row 2)
     * Slot 5: (Col 1, Row 2) - Fixed "Other" action card in 3x2 grid
     */
    fun calculateSlotCenter(
        slotIndex: Int,
        cardWidthPx: Float,
        cardHeightPx: Float,
        spacingPx: Float
    ): Offset {
        val col = slotIndex % GRID_COLUMNS
        val row = slotIndex / GRID_COLUMNS
        val cx = col * (cardWidthPx + spacingPx) + (cardWidthPx / 2f)
        val cy = row * (cardHeightPx + spacingPx) + (cardHeightPx / 2f)
        return Offset(cx, cy)
    }

    /**
     * Determines whether the dragged card center has moved outside the acceptable drop zone.
     * If the finger moves too far away, drag reorder is cancelled/aborted to prevent accidental reordering.
     */
    fun isOutsideDropZone(
        draggedCenter: Offset,
        cardWidthPx: Float,
        cardHeightPx: Float,
        spacingPx: Float,
        itemCount: Int
    ): Boolean {
        if (cardWidthPx <= 0f || cardHeightPx <= 0f) return true

        val toleranceX = cardWidthPx * HORIZONTAL_TOLERANCE_FRACTION
        val toleranceY = cardHeightPx * VERTICAL_TOLERANCE_FRACTION

        val gridWidth = GRID_COLUMNS * cardWidthPx + spacingPx
        val totalRows = when {
            itemCount >= 4 -> MAX_GRID_ROWS
            itemCount >= 2 -> 2
            else -> 1
        }
        val gridHeight = totalRows * cardHeightPx + ((totalRows - 1).coerceAtLeast(0) * spacingPx)

        return draggedCenter.x < -toleranceX ||
                draggedCenter.x > (gridWidth + toleranceX) ||
                draggedCenter.y < -toleranceY ||
                draggedCenter.y > (gridHeight + toleranceY)
    }

    /**
     * Computes which slot the dragged card is hovering over.
     *
     * Returns [draggedIndex] (no change) if:
     * - The drag is outside the drop zone bounds.
     * - The drag is moved into an unallocated row.
     */
    fun determineTargetDropIndex(
        draggedIndex: Int,
        dragOffset: Offset,
        cardWidthPx: Float,
        cardHeightPx: Float,
        spacingPx: Float,
        itemCount: Int
    ): Int {
        if (draggedIndex !in 0 until itemCount || cardWidthPx <= 0f || cardHeightPx <= 0f || itemCount <= 1) {
            return draggedIndex
        }

        val draggedCenter = calculateSlotCenter(draggedIndex, cardWidthPx, cardHeightPx, spacingPx) + dragOffset

        // If outside overall bounding box, abort reorder preview
        if (isOutsideDropZone(draggedCenter, cardWidthPx, cardHeightPx, spacingPx, itemCount)) {
            return draggedIndex
        }

        // Special handling for rows with a single pinned item alongside "Other":
        // In 5-item grid: Row 2 has Slot 4 (pinned) and Slot 5 (Other).
        // Any drag hovering in Row 2 targets Slot 4 (the last pinned folder).
        if (itemCount == MAX_PINNED_CAPACITY) {
            val row2Top = 2 * (cardHeightPx + spacingPx) - (spacingPx / 2f)
            if (draggedCenter.y >= row2Top) {
                return 4
            }
        } else if (itemCount == 4) {
            // In 4-item grid: Row 0 has Slots 0, 1; Row 1 has Slots 2, 3; Row 2 has only "Other".
            // Dragging down into Row 2 (where only Other sits) is invalid.
            val row2Top = 2 * (cardHeightPx + spacingPx) - (spacingPx / 2f)
            if (draggedCenter.y >= row2Top) {
                return draggedIndex
            }
        } else if (itemCount == 3) {
            // In 3-item grid: Row 1 has Slot 2 (pinned) and Slot 3 (Other).
            // Any drag hovering in Row 1 targets Slot 2.
            val row1Top = cardHeightPx + (spacingPx / 2f)
            val row2Top = 2 * (cardHeightPx + spacingPx) - (spacingPx / 2f)
            if (draggedCenter.y >= row1Top && draggedCenter.y < row2Top) {
                return 2
            }
            if (draggedCenter.y >= row2Top) {
                return draggedIndex
            }
        } else if (itemCount == 2) {
            // If only 2 pinned items exist (both in Row 0), dragging down into Row 1 is invalid
            val row1Top = cardHeightPx + (spacingPx / 2f)
            if (draggedCenter.y >= row1Top) {
                return draggedIndex
            }
        }

        // Find the nearest valid pinned folder slot
        val validSlots = 0 until itemCount.coerceAtMost(MAX_PINNED_CAPACITY)
        return validSlots.minByOrNull { slot ->
            val slotCenter = calculateSlotCenter(slot, cardWidthPx, cardHeightPx, spacingPx)
            val dx = draggedCenter.x - slotCenter.x
            val dy = draggedCenter.y - slotCenter.y
            dx * dx + dy * dy
        } ?: draggedIndex
    }

    /**
     * Calculates the prospective visual slot index for a given card during an active drag.
     *
     * Non-dragged cards between [draggedIndex] and [targetDropIndex] slide into the adjacent slot
     * to make room for the lifted card.
     */
    fun calculateTargetSlot(
        currentIndex: Int,
        draggedIndex: Int,
        targetDropIndex: Int
    ): Int {
        if (draggedIndex == -1 || targetDropIndex == -1 || targetDropIndex == draggedIndex) {
            return currentIndex
        }

        return when {
            targetDropIndex < draggedIndex -> {
                // Dragging backward (e.g. from slot 4 to slot 0): items in [target..dragged-1] shift forward (+1)
                if (currentIndex in targetDropIndex until draggedIndex) currentIndex + 1 else currentIndex
            }
            targetDropIndex > draggedIndex -> {
                // Dragging forward (e.g. from slot 0 to slot 4): items in [dragged+1..target] shift backward (-1)
                if (currentIndex in (draggedIndex + 1)..targetDropIndex) currentIndex - 1 else currentIndex
            }
            else -> currentIndex
        }
    }

    /**
     * Computes the visual (X, Y) pixel translation vector needed to shift a card
     * from [fromSlot] to [toSlot].
     */
    fun calculateSlotShift(
        fromSlot: Int,
        toSlot: Int,
        cardWidthPx: Float,
        cardHeightPx: Float,
        spacingPx: Float
    ): Offset {
        val deltaCol = (toSlot % GRID_COLUMNS) - (fromSlot % GRID_COLUMNS)
        val deltaRow = (toSlot / GRID_COLUMNS) - (fromSlot / GRID_COLUMNS)
        val shiftX = deltaCol * (cardWidthPx + spacingPx)
        val shiftY = deltaRow * (cardHeightPx + spacingPx)
        return Offset(shiftX, shiftY)
    }

    /**
     * Produces a new list where the element at [fromIndex] is moved to [toIndex].
     */
    fun <T> reorderList(list: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex == toIndex || fromIndex !in list.indices || toIndex !in list.indices) {
            return list
        }
        val mutable = list.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }
}
