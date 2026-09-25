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

    private const val HORIZONTAL_TOLERANCE_FRACTION = 0.40f
    private const val VERTICAL_TOLERANCE_FRACTION = 0.45f

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
        val col = slotIndex % 2
        val row = slotIndex / 2
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

        val gridWidth = 2 * cardWidthPx + spacingPx
        val totalRows = when {
            itemCount >= 4 -> 3
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
     * - The drag is hovering over the fixed "Other" card (Slot 5 when itemCount >= 4, Slot 3 when itemCount in 2..3).
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

        // Fixed "Other" card detection: cannot swap with the Other action card
        if (itemCount >= 4) {
            // "Other" is at Row 2, Col 1 (Slot 5)
            val otherLeft = cardWidthPx + (spacingPx / 2f)
            val otherTop = 2 * (cardHeightPx + spacingPx) - (spacingPx / 2f)
            if (draggedCenter.x >= otherLeft && draggedCenter.y >= otherTop) {
                return draggedIndex
            }
        } else if (itemCount in 2..3) {
            // "Other" is at Row 1, Col 1 (Slot 3)
            val otherLeft = cardWidthPx + (spacingPx / 2f)
            val otherTop = cardHeightPx + (spacingPx / 2f)
            if (draggedCenter.x >= otherLeft && draggedCenter.y >= otherTop) {
                return draggedIndex
            }
            // Dragging down into non-existent Row 2 is invalid
            val row2Top = 2 * (cardHeightPx + spacingPx) - (spacingPx / 2f)
            if (draggedCenter.y >= row2Top) {
                return draggedIndex
            }
        }

        // If only 2 pinned items exist (both in Row 0), dragging down into Row 1 is invalid
        if (itemCount == 2) {
            val row1Top = cardHeightPx + (spacingPx / 2f)
            if (draggedCenter.y >= row1Top) {
                return draggedIndex
            }
        }

        // Find the nearest valid pinned folder slot
        val validSlots = 0 until itemCount.coerceAtMost(5)
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
        val deltaCol = (toSlot % 2) - (fromSlot % 2)
        val deltaRow = (toSlot / 2) - (fromSlot / 2)
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
