package com.leshoraa.scanorea.features.pdfviewer.domain

/**
 * Pure calculation engine for PDF viewer fast scrolling and active page detection.
 */
object PdfScrollCalculator {

    data class ItemBounds(
        val index: Int,
        val top: Int,
        val bottom: Int
    )

    /**
     * Determines which page is currently intersecting the scrollbar thumb's vertical center.
     * When a paper passes the scrollbar, this switches to the page that the scrollbar is now hovering over.
     */
    fun findPageAtThumbCenter(
        thumbCenterY: Float,
        visibleItems: List<ItemBounds>,
        totalPages: Int,
        gapPx: Int = 0
    ): Int {
        if (visibleItems.isEmpty() || totalPages <= 1) return 1

        val matched = visibleItems.firstOrNull { item ->
            val itemBottomWithGap = item.bottom + gapPx
            thumbCenterY >= item.top && thumbCenterY < itemBottomWithGap
        }

        return when {
            matched != null -> matched.index + 1
            thumbCenterY < visibleItems.first().top -> visibleItems.first().index + 1
            else -> visibleItems.last().index + 1
        }.coerceIn(1, totalPages)
    }

    /**
     * Calculates the normalized scroll progress (0.0f..1.0f) based on LazyListState parameters.
     */
    fun calculateScrollProgress(
        firstVisibleIndex: Int,
        firstVisibleOffset: Int,
        totalPages: Int,
        visibleItems: List<ItemBounds>,
        viewportStart: Int,
        viewportEnd: Int,
        spacingPx: Float = 0f
    ): Float {
        if (totalPages <= 1 || visibleItems.isEmpty()) return 0f

        // At absolute top
        if (firstVisibleIndex == 0 && firstVisibleOffset <= 0) {
            return 0f
        }

        // At absolute bottom
        val lastItem = visibleItems.last()
        if (lastItem.index == totalPages - 1 && lastItem.bottom <= viewportEnd) {
            return 1f
        }

        val avgItemHeight = visibleItems.sumOf { it.bottom - it.top }.toFloat() / visibleItems.size
        val itemSpan = avgItemHeight + spacingPx
        val totalEstimatedHeight = totalPages * itemSpan
        val viewportHeight = (viewportEnd - viewportStart).toFloat()
        val maxScrollDistance = (totalEstimatedHeight - viewportHeight).coerceAtLeast(1f)

        val currentScrolled = firstVisibleIndex * itemSpan + firstVisibleOffset
        return (currentScrolled / maxScrollDistance).coerceIn(0f, 1f)
    }

    /**
     * Maps a fast scroll drag fraction (0.0f..1.0f) to the target item index and pixel offset.
     */
    fun calculateScrollTarget(
        fraction: Float,
        totalPages: Int,
        avgItemHeight: Float,
        spacingPx: Float,
        viewportHeight: Float
    ): Pair<Int, Int> {
        if (totalPages <= 1) return Pair(0, 0)

        val clampedFraction = fraction.coerceIn(0f, 1f)
        if (clampedFraction <= 0f) return Pair(0, 0)

        val itemSpan = avgItemHeight + spacingPx
        val totalEstimatedHeight = totalPages * itemSpan
        val maxScrollDistance = (totalEstimatedHeight - viewportHeight).coerceAtLeast(0f)

        if (maxScrollDistance <= 0f) return Pair(0, 0)

        val targetScrollPx = clampedFraction * maxScrollDistance
        val targetIndex = (targetScrollPx / itemSpan).toInt().coerceIn(0, totalPages - 1)
        val targetOffset = (targetScrollPx - targetIndex * itemSpan).toInt().coerceAtLeast(0)

        return Pair(targetIndex, targetOffset)
    }
}
