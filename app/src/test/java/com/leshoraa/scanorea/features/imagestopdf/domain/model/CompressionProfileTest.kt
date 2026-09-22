package com.leshoraa.scanorea.features.imagestopdf.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating [CompressionProfile] compression ratios and size estimations.
 */
class CompressionProfileTest {

    @Test
    fun estimateSizeBytes_increasesMonotonicallyWithPageCount() {
        val zeroPageEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(0)
        val singlePageEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(1)
        val fivePageEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(5)

        assertEquals(0L, zeroPageEstimate)
        assertTrue(singlePageEstimate > 0L)
        assertTrue(fivePageEstimate > singlePageEstimate)
    }

    @Test
    fun estimateSizeBytes_smallFileProducesSmallerEstimateThanHighQuality() {
        val pageCount = 3
        val smallSize = CompressionProfile.SMALL_FILE.estimateSizeBytes(pageCount)
        val balancedSize = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(pageCount)
        val highQualitySize = CompressionProfile.HIGH_QUALITY.estimateSizeBytes(pageCount)
        val maximumSize = CompressionProfile.MAXIMUM.estimateSizeBytes(pageCount)

        assertTrue(smallSize < balancedSize)
        assertTrue(balancedSize < highQualitySize)
        assertTrue(highQualitySize < maximumSize)
    }

    @Test
    fun compressionProfiles_haveValidDimensionsAndQuality() {
        CompressionProfile.entries.forEach { profile ->
            assertTrue(profile.maxDimensionPixels > 0)
            assertTrue(profile.jpegQuality in 1..100)
            assertTrue(profile.label.isNotBlank())
            assertTrue(profile.description.isNotBlank())
        }
    }
}
