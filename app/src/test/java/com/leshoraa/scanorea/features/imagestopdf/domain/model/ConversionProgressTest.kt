package com.leshoraa.scanorea.features.imagestopdf.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying [ConversionProgress] calculation logic.
 */
class ConversionProgressTest {

    @Test
    fun progressFraction_whenTotalPagesIsZero_returnsZero() {
        val progress = ConversionProgress(currentPageIndex = 0, totalPages = 0)
        assertEquals(0f, progress.progressFraction, 0.001f)
    }

    @Test
    fun progressFraction_whenPartiallyCompleted_calculatesCorrectFraction() {
        val progress = ConversionProgress(currentPageIndex = 2, totalPages = 4)
        assertEquals(0.5f, progress.progressFraction, 0.001f)
    }

    @Test
    fun progressFraction_whenFullyCompleted_returnsOne() {
        val progress = ConversionProgress(currentPageIndex = 5, totalPages = 5)
        assertEquals(1f, progress.progressFraction, 0.001f)
    }
}
