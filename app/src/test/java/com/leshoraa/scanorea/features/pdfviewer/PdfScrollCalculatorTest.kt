package com.leshoraa.scanorea.features.pdfviewer

import com.leshoraa.scanorea.features.pdfviewer.domain.PdfScrollCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfScrollCalculatorTest {

    @Test
    fun findPageAtThumbCenter_returnsPage1_whenSinglePageOrEmpty() {
        val emptyResult = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 300f,
            visibleItems = emptyList(),
            totalPages = 1
        )
        assertEquals(1, emptyResult)

        val singlePageResult = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 300f,
            visibleItems = listOf(PdfScrollCalculator.ItemBounds(index = 0, top = 0, bottom = 1200)),
            totalPages = 1
        )
        assertEquals(1, singlePageResult)
    }

    @Test
    fun findPageAtThumbCenter_detectsPageUnderThumb_whenPagesScrollPast() {
        val totalPages = 4
        val gap = 24 // 8.dp * 3px/dp

        // State 1: Page 1 covers the scrollbar thumb at Y = 500
        val itemsState1 = listOf(
            PdfScrollCalculator.ItemBounds(index = 0, top = 0, bottom = 1000),
            PdfScrollCalculator.ItemBounds(index = 1, top = 1024, bottom = 2024)
        )
        val pageState1 = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 500f,
            visibleItems = itemsState1,
            totalPages = totalPages,
            gapPx = gap
        )
        assertEquals(1, pageState1)

        // State 2: Page 1 has scrolled past Y = 500. Now Page 2 intersects Y = 500
        val itemsState2 = listOf(
            PdfScrollCalculator.ItemBounds(index = 0, top = -800, bottom = 200),
            PdfScrollCalculator.ItemBounds(index = 1, top = 224, bottom = 1224)
        )
        val pageState2 = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 500f,
            visibleItems = itemsState2,
            totalPages = totalPages,
            gapPx = gap
        )
        assertEquals(2, pageState2)

        // State 3: Page 2 has scrolled past Y = 500. Now Page 3 intersects Y = 500
        val itemsState3 = listOf(
            PdfScrollCalculator.ItemBounds(index = 1, top = -800, bottom = 200),
            PdfScrollCalculator.ItemBounds(index = 2, top = 224, bottom = 1224)
        )
        val pageState3 = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 500f,
            visibleItems = itemsState3,
            totalPages = totalPages,
            gapPx = gap
        )
        assertEquals(3, pageState3)
    }

    @Test
    fun findPageAtThumbCenter_handlesBoundaryGapsGracefully() {
        val totalPages = 3
        val gap = 24

        // Thumb is directly in the 24px gap between Page 1 and Page 2
        val items = listOf(
            PdfScrollCalculator.ItemBounds(index = 0, top = 0, bottom = 1000),
            PdfScrollCalculator.ItemBounds(index = 1, top = 1024, bottom = 2024)
        )
        // 1010 is within [1000, 1024) gap
        val pageInGap = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 1010f,
            visibleItems = items,
            totalPages = totalPages,
            gapPx = gap
        )
        // Page 1 owns up to the start of Page 2
        assertEquals(1, pageInGap)

        // As soon as thumb reaches 1024, Page 2 begins
        val pageAtStartOf2 = PdfScrollCalculator.findPageAtThumbCenter(
            thumbCenterY = 1024f,
            visibleItems = items,
            totalPages = totalPages,
            gapPx = gap
        )
        assertEquals(2, pageAtStartOf2)
    }

    @Test
    fun calculateScrollProgress_returns0_atAbsoluteTop() {
        val items = listOf(
            PdfScrollCalculator.ItemBounds(index = 0, top = 0, bottom = 1200)
        )
        val progress = PdfScrollCalculator.calculateScrollProgress(
            firstVisibleIndex = 0,
            firstVisibleOffset = 0,
            totalPages = 4,
            visibleItems = items,
            viewportStart = 0,
            viewportEnd = 2400
        )
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun calculateScrollProgress_returns1_atAbsoluteBottom() {
        val totalPages = 3
        val items = listOf(
            PdfScrollCalculator.ItemBounds(index = 1, top = -600, bottom = 600),
            PdfScrollCalculator.ItemBounds(index = 2, top = 624, bottom = 1824)
        )
        val progress = PdfScrollCalculator.calculateScrollProgress(
            firstVisibleIndex = 1,
            firstVisibleOffset = 600,
            totalPages = totalPages,
            visibleItems = items,
            viewportStart = 0,
            viewportEnd = 2000 // bottom 1824 <= 2000
        )
        assertEquals(1f, progress, 0.001f)
    }

    @Test
    fun calculateScrollTarget_mapsFractionToItemAndOffset() {
        val totalPages = 4
        val avgHeight = 1500f
        val spacing = 24f
        val viewportHeight = 2400f

        // Top fraction
        val (topIndex, topOffset) = PdfScrollCalculator.calculateScrollTarget(
            fraction = 0f,
            totalPages = totalPages,
            avgItemHeight = avgHeight,
            spacingPx = spacing,
            viewportHeight = viewportHeight
        )
        assertEquals(0, topIndex)
        assertEquals(0, topOffset)

        // Middle fraction (0.5f)
        val (midIndex, midOffset) = PdfScrollCalculator.calculateScrollTarget(
            fraction = 0.5f,
            totalPages = totalPages,
            avgItemHeight = avgHeight,
            spacingPx = spacing,
            viewportHeight = viewportHeight
        )
        assertTrue("Mid index should be between 0 and 3", midIndex in 0..3)
        assertTrue("Mid offset should be non-negative", midOffset >= 0)

        // Bottom fraction (1.0f)
        val (bottomIndex, bottomOffset) = PdfScrollCalculator.calculateScrollTarget(
            fraction = 1.0f,
            totalPages = totalPages,
            avgItemHeight = avgHeight,
            spacingPx = spacing,
            viewportHeight = viewportHeight
        )
        assertTrue("Bottom index should be near end", bottomIndex >= 1)
        assertTrue("Bottom offset should be positive", bottomOffset >= 0)
    }
}
