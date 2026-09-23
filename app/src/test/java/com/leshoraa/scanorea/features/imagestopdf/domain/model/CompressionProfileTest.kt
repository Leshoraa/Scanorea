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

    @Test
    fun estimateSizeBytes_withPages_reflectsCropAndFilters() {
        val dummyUri = android.net.TestUri("content://media/test/1")
        val basePage = ImagePage(
            id = "p1",
            uri = dummyUri,
            width = 3000,
            height = 4000,
            filter = com.leshoraa.scanorea.core.filter.ImageFilterType.ORIGINAL
        )

        // Empty pages list returns 0
        assertEquals(0L, CompressionProfile.AUTO_BALANCED.estimateSizeBytes(emptyList()))

        // Uncropped color vs cropped color
        val uncroppedEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(listOf(basePage))
        val croppedPage = basePage.copy(cropBounds = ImageCropBounds(0.25f, 0.25f, 0.75f, 0.75f)) // 25% area
        val croppedEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(listOf(croppedPage))
        assertTrue("Cropped image should result in smaller estimated bytes", croppedEstimate < uncroppedEstimate)

        // B&W filter vs Color filter
        val bwPage = basePage.copy(filter = com.leshoraa.scanorea.core.filter.ImageFilterType.BLACK_AND_WHITE)
        val bwEstimate = CompressionProfile.AUTO_BALANCED.estimateSizeBytes(listOf(bwPage))
        assertTrue("B&W document should compress much smaller than color", bwEstimate < uncroppedEstimate)

        // Profile scaling
        val smallEstimate = CompressionProfile.SMALL_FILE.estimateSizeBytes(listOf(basePage))
        val highQualityEstimate = CompressionProfile.HIGH_QUALITY.estimateSizeBytes(listOf(basePage))
        assertTrue(smallEstimate < uncroppedEstimate)
        assertTrue(uncroppedEstimate < highQualityEstimate)
    }
}
