package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FolderGridDragCalculator] validating slot calculations,
 * bounds checking, prospective slot shifting, and delta vectors.
 */
class FolderGridDragCalculatorTest {

    private val cardWidth = 100f
    private val cardHeight = 60f
    private val spacing = 10f

    @Test
    fun calculateSlotCenter_returnsExpectedCoordinates() {
        val center0 = FolderGridDragCalculator.calculateSlotCenter(0, cardWidth, cardHeight, spacing)
        assertEquals(50f, center0.x, 0.01f)
        assertEquals(30f, center0.y, 0.01f)

        val center1 = FolderGridDragCalculator.calculateSlotCenter(1, cardWidth, cardHeight, spacing)
        assertEquals(160f, center1.x, 0.01f)
        assertEquals(30f, center1.y, 0.01f)

        val center2 = FolderGridDragCalculator.calculateSlotCenter(2, cardWidth, cardHeight, spacing)
        assertEquals(50f, center2.x, 0.01f)
        assertEquals(100f, center2.y, 0.01f)

        val center3 = FolderGridDragCalculator.calculateSlotCenter(3, cardWidth, cardHeight, spacing)
        assertEquals(160f, center3.x, 0.01f)
        assertEquals(100f, center3.y, 0.01f)

        val center4 = FolderGridDragCalculator.calculateSlotCenter(4, cardWidth, cardHeight, spacing)
        assertEquals(50f, center4.x, 0.01f)
        assertEquals(170f, center4.y, 0.01f)

        val center5 = FolderGridDragCalculator.calculateSlotCenter(5, cardWidth, cardHeight, spacing)
        assertEquals(160f, center5.x, 0.01f)
        assertEquals(170f, center5.y, 0.01f)
    }

    @Test
    fun isOutsideDropZone_whenInside_returnsFalse() {
        val center = Offset(100f, 50f)
        val isOutside = FolderGridDragCalculator.isOutsideDropZone(center, cardWidth, cardHeight, spacing, itemCount = 3)
        assertFalse(isOutside)

        val centerRowThree = Offset(50f, 170f)
        assertFalse(FolderGridDragCalculator.isOutsideDropZone(centerRowThree, cardWidth, cardHeight, spacing, itemCount = 5))
    }

    @Test
    fun isOutsideDropZone_whenFarOutside_returnsTrue() {
        val farLeft = Offset(-100f, 50f)
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(farLeft, cardWidth, cardHeight, spacing, itemCount = 3))

        val farBottom = Offset(100f, 350f)
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(farBottom, cardWidth, cardHeight, spacing, itemCount = 5))

        val farRight = Offset(300f, 50f)
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(farRight, cardWidth, cardHeight, spacing, itemCount = 3))

        val farTop = Offset(100f, -100f)
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(farTop, cardWidth, cardHeight, spacing, itemCount = 3))
    }

    @Test
    fun isOutsideDropZone_whenDimensionsZero_returnsTrue() {
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(Offset(50f, 50f), 0f, 60f, 10f, 3))
        assertTrue(FolderGridDragCalculator.isOutsideDropZone(Offset(50f, 50f), 100f, 0f, 10f, 3))
    }

    @Test
    fun determineTargetDropIndex_whenDraggedNearTargetSlot_returnsTargetSlot() {
        // Drag item 0 (at (50, 30)) towards slot 1 (at (160, 30))
        val target1 = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(110f, 0f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 3
        )
        assertEquals(1, target1)

        // Drag item 0 (at (50, 30)) towards slot 2 (at (50, 100))
        val target2 = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(0f, 70f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 3
        )
        assertEquals(2, target2)

        // Drag item 0 (at (50, 30)) towards slot 4 (at (50, 170)) in a 5-item grid
        val target4 = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(0f, 140f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 5
        )
        assertEquals(4, target4)
    }

    @Test
    fun determineTargetDropIndex_whenDraggedFarOutside_revertsToDraggedIndex() {
        // Drag item 0 far away downwards into list area
        val target = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(100f, 400f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 3
        )
        assertEquals(0, target)
    }

    @Test
    fun determineTargetDropIndex_whenHoveringOverOtherCard_revertsToDraggedIndex() {
        // For 3 items: Slot 3 center is (160, 100). Drag item 0 from (50, 30) with delta (110, 70) to land squarely in slot 3
        val target = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(110f, 70f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 3
        )
        assertEquals(0, target)

        // For 5 items: Slot 5 ("Other") center is (160, 170). Drag item 0 from (50, 30) with delta (110, 140) to land in slot 5
        val target5 = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(110f, 140f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 5
        )
        assertEquals(0, target5)
    }

    @Test
    fun determineTargetDropIndex_withTwoItems_whenDraggedToRowTwo_revertsToDraggedIndex() {
        // Only 2 items exist (row 0). Dragging down into row 1 should not swap
        val target = FolderGridDragCalculator.determineTargetDropIndex(
            draggedIndex = 0,
            dragOffset = Offset(0f, 70f),
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            spacingPx = spacing,
            itemCount = 2
        )
        assertEquals(0, target)
    }

    @Test
    fun determineTargetDropIndex_withInvalidArguments_returnsDraggedIndex() {
        // itemCount <= 1
        assertEquals(0, FolderGridDragCalculator.determineTargetDropIndex(0, Offset(100f, 0f), cardWidth, cardHeight, spacing, 1))

        // draggedIndex out of range
        assertEquals(-1, FolderGridDragCalculator.determineTargetDropIndex(-1, Offset(100f, 0f), cardWidth, cardHeight, spacing, 3))
        assertEquals(6, FolderGridDragCalculator.determineTargetDropIndex(6, Offset(100f, 0f), cardWidth, cardHeight, spacing, 5))

        // zero card sizes
        assertEquals(0, FolderGridDragCalculator.determineTargetDropIndex(0, Offset(100f, 0f), 0f, 60f, spacing, 3))
    }

    @Test
    fun reorderList_movesItemToExpectedIndex() {
        val original = listOf("Favorites", "Work", "Study", "Personal", "Projects")
        val reorderedForward = FolderGridDragCalculator.reorderList(original, fromIndex = 0, toIndex = 4)
        assertEquals(listOf("Work", "Study", "Personal", "Projects", "Favorites"), reorderedForward)

        val reorderedBackward = FolderGridDragCalculator.reorderList(original, fromIndex = 4, toIndex = 1)
        assertEquals(listOf("Favorites", "Projects", "Work", "Study", "Personal"), reorderedBackward)

        // Identity / Out of bounds returns original unchanged
        assertEquals(original, FolderGridDragCalculator.reorderList(original, 2, 2))
        assertEquals(original, FolderGridDragCalculator.reorderList(original, -1, 2))
        assertEquals(original, FolderGridDragCalculator.reorderList(original, 0, 10))
    }

    @Test
    fun calculateTargetSlot_allSixPermutationsForThreeItems_displacesCorrectly() {
        // Permutation 1: Drag 0 -> 1
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 0, targetDropIndex = 1))
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 0, targetDropIndex = 1))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 0, targetDropIndex = 1))

        // Permutation 2: Drag 0 -> 2
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 0, targetDropIndex = 2))
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 0, targetDropIndex = 2))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 0, targetDropIndex = 2))

        // Permutation 3: Drag 1 -> 0
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 1, targetDropIndex = 0))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 1, targetDropIndex = 0))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 1, targetDropIndex = 0))

        // Permutation 4: Drag 1 -> 2
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 1, targetDropIndex = 2))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 1, targetDropIndex = 2))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 1, targetDropIndex = 2))

        // Permutation 5: Drag 2 -> 0
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 2, targetDropIndex = 0))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 2, targetDropIndex = 0))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 2, targetDropIndex = 0))

        // Permutation 6: Drag 2 -> 1
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 2, targetDropIndex = 1))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 2, targetDropIndex = 1))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 2, targetDropIndex = 1))
    }

    @Test
    fun calculateTargetSlot_whenCancelledOrNeutral_retainsOriginalSlots() {
        assertEquals(0, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 0, draggedIndex = 0, targetDropIndex = 0))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = 0, targetDropIndex = 0))
        assertEquals(1, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 1, draggedIndex = -1, targetDropIndex = -1))
        assertEquals(2, FolderGridDragCalculator.calculateTargetSlot(currentIndex = 2, draggedIndex = 1, targetDropIndex = -1))
    }

    @Test
    fun calculateSlotShift_computesAccurateTranslationsForAllDirections() {
        // Horizontal: Slot 0 to Slot 1 (dx = +110, dy = 0)
        val shift0to1 = FolderGridDragCalculator.calculateSlotShift(0, 1, cardWidth, cardHeight, spacing)
        assertEquals(110f, shift0to1.x, 0.01f)
        assertEquals(0f, shift0to1.y, 0.01f)

        // Horizontal: Slot 1 to Slot 0 (dx = -110, dy = 0)
        val shift1to0 = FolderGridDragCalculator.calculateSlotShift(1, 0, cardWidth, cardHeight, spacing)
        assertEquals(-110f, shift1to0.x, 0.01f)
        assertEquals(0f, shift1to0.y, 0.01f)

        // Vertical: Slot 0 (row 0, col 0) to Slot 2 (row 1, col 0) -> dx = 0, dy = +70
        val shift0to2 = FolderGridDragCalculator.calculateSlotShift(0, 2, cardWidth, cardHeight, spacing)
        assertEquals(0f, shift0to2.x, 0.01f)
        assertEquals(70f, shift0to2.y, 0.01f)

        // Vertical: Slot 2 (row 1, col 0) to Slot 0 (row 0, col 0) -> dx = 0, dy = -70
        val shift2to0 = FolderGridDragCalculator.calculateSlotShift(2, 0, cardWidth, cardHeight, spacing)
        assertEquals(0f, shift2to0.x, 0.01f)
        assertEquals(-70f, shift2to0.y, 0.01f)

        // Diagonal: Slot 2 (row 1, col 0) to Slot 1 (row 0, col 1) -> dx = +110, dy = -70
        val shift2to1 = FolderGridDragCalculator.calculateSlotShift(2, 1, cardWidth, cardHeight, spacing)
        assertEquals(110f, shift2to1.x, 0.01f)
        assertEquals(-70f, shift2to1.y, 0.01f)

        // Diagonal: Slot 1 (row 0, col 1) to Slot 2 (row 1, col 0) -> dx = -110, dy = +70
        val shift1to2 = FolderGridDragCalculator.calculateSlotShift(1, 2, cardWidth, cardHeight, spacing)
        assertEquals(-110f, shift1to2.x, 0.01f)
        assertEquals(70f, shift1to2.y, 0.01f)

        // Identity: Slot 1 to Slot 1 -> dx = 0, dy = 0
        val shift1to1 = FolderGridDragCalculator.calculateSlotShift(1, 1, cardWidth, cardHeight, spacing)
        assertEquals(0f, shift1to1.x, 0.01f)
        assertEquals(0f, shift1to1.y, 0.01f)
    }
}
