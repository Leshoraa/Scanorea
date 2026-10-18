package com.leshoraa.scanorea.features.imagestopdf.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating [ImageCropBounds] boundary constraints, clamping logic,
 * and aspect-ratio scaling mathematics.
 */
class ImageCropBoundsTest {

    @Test
    fun defaultBounds_coversFullNormalizedCanvas() {
        val bounds = ImageCropBounds.DEFAULT
        assertEquals(0.0f, bounds.left, 0.0001f)
        assertEquals(0.0f, bounds.top, 0.0001f)
        assertEquals(1.0f, bounds.right, 0.0001f)
        assertEquals(1.0f, bounds.bottom, 0.0001f)
        assertTrue(bounds.isDefault)
        assertEquals(1.0f, bounds.widthFraction, 0.0001f)
        assertEquals(1.0f, bounds.heightFraction, 0.0001f)
    }

    @Test
    fun customBounds_reportsCorrectFractionsAndNonDefault() {
        val bounds = ImageCropBounds(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        assertFalse(bounds.isDefault)
        assertEquals(0.7f, bounds.widthFraction, 0.0001f)
        assertEquals(0.7f, bounds.heightFraction, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_throwsWhenLeftGreaterThanRight() {
        ImageCropBounds(left = 0.8f, top = 0.1f, right = 0.2f, bottom = 0.9f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_throwsWhenTopGreaterThanBottom() {
        ImageCropBounds(left = 0.1f, top = 0.9f, right = 0.8f, bottom = 0.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_throwsWhenOutOfBounds() {
        ImageCropBounds(left = -0.1f, top = 0f, right = 1f, bottom = 1f)
    }

    @Test
    fun ofClamped_restrainsExtremeValuesWithinNormalizedRange() {
        val clamped = ImageCropBounds.ofClamped(
            left = -0.5f,
            top = -0.2f,
            right = 1.5f,
            bottom = 2.0f,
            minDimension = 0.1f
        )
        assertEquals(0.0f, clamped.left, 0.0001f)
        assertEquals(0.0f, clamped.top, 0.0001f)
        assertEquals(1.0f, clamped.right, 0.0001f)
        assertEquals(1.0f, clamped.bottom, 0.0001f)
    }

    @Test
    fun ofClamped_enforcesMinimumDimension() {
        val clamped = ImageCropBounds.ofClamped(
            left = 0.5f,
            top = 0.5f,
            right = 0.51f,
            bottom = 0.51f,
            minDimension = 0.1f
        )
        assertTrue(clamped.widthFraction >= 0.1f)
        assertTrue(clamped.heightFraction >= 0.1f)
        assertTrue(clamped.right <= 1.0f)
        assertTrue(clamped.bottom <= 1.0f)
    }

    @Test
    fun fromAspectRatio_squareTargetOnSquareImage_fillsCanvas() {
        val bounds = ImageCropBounds.fromAspectRatio(targetRatio = 1.0f, imageAspectRatio = 1.0f)
        assertEquals(0.0f, bounds.left, 0.0001f)
        assertEquals(0.0f, bounds.top, 0.0001f)
        assertEquals(1.0f, bounds.right, 0.0001f)
        assertEquals(1.0f, bounds.bottom, 0.0001f)
    }

    @Test
    fun fromAspectRatio_squareTargetOnPortraitImage_centersVertically() {
        // Image is 1000 x 2000, so imageAspectRatio = 0.5
        // A square crop requires physical width == physical height.
        // In normalized space: height must be 0.5 of image height.
        val bounds = ImageCropBounds.fromAspectRatio(targetRatio = 1.0f, imageAspectRatio = 0.5f)
        assertEquals(0.0f, bounds.left, 0.0001f)
        assertEquals(1.0f, bounds.right, 0.0001f)
        assertEquals(0.25f, bounds.top, 0.0001f)
        assertEquals(0.75f, bounds.bottom, 0.0001f)

        // Effective physical ratio: (1.0 * 1000) / (0.5 * 2000) = 1000 / 1000 = 1.0
        val physicalWidth = bounds.widthFraction * 1000f
        val physicalHeight = bounds.heightFraction * 2000f
        assertEquals(1.0f, physicalWidth / physicalHeight, 0.0001f)
    }

    @Test
    fun fromAspectRatio_squareTargetOnLandscapeImage_centersHorizontally() {
        // Image is 2000 x 1000, so imageAspectRatio = 2.0
        // A square crop requires physical width == physical height.
        // In normalized space: width must be 0.5 of image width.
        val bounds = ImageCropBounds.fromAspectRatio(targetRatio = 1.0f, imageAspectRatio = 2.0f)
        assertEquals(0.25f, bounds.left, 0.0001f)
        assertEquals(0.75f, bounds.right, 0.0001f)
        assertEquals(0.0f, bounds.top, 0.0001f)
        assertEquals(1.0f, bounds.bottom, 0.0001f)

        val physicalWidth = bounds.widthFraction * 2000f
        val physicalHeight = bounds.heightFraction * 1000f
        assertEquals(1.0f, physicalWidth / physicalHeight, 0.0001f)
    }

    @Test
    fun fromAspectRatio_a4TargetOnLandscapeImage_cropsCorrectly() {
        val a4Ratio = 1f / 1.4142f // ~0.7071
        val bounds = ImageCropBounds.fromAspectRatio(targetRatio = a4Ratio, imageAspectRatio = 2.0f)

        val physicalWidth = bounds.widthFraction * 2000f
        val physicalHeight = bounds.heightFraction * 1000f
        val actualRatio = physicalWidth / physicalHeight
        assertEquals(a4Ratio, actualRatio, 0.001f)
    }

    @Test
    fun rotateClockwise_rotatesBoundsCorrectly() {
        // Original bounds: top half of canvas
        val original = ImageCropBounds(left = 0.0f, top = 0.0f, right = 1.0f, bottom = 0.5f)
        val rotated = original.rotateClockwise()

        // 90 degrees clockwise should map top half to right half
        assertEquals(0.5f, rotated.left, 0.0001f)
        assertEquals(0.0f, rotated.top, 0.0001f)
        assertEquals(1.0f, rotated.right, 0.0001f)
        assertEquals(1.0f, rotated.bottom, 0.0001f)
    }

    @Test
    fun rotateCounterClockwise_rotatesBoundsCorrectly() {
        // Original bounds: top half of canvas
        val original = ImageCropBounds(left = 0.0f, top = 0.0f, right = 1.0f, bottom = 0.5f)
        val rotated = original.rotateCounterClockwise()

        // 90 degrees counter-clockwise should map top half to left half
        assertEquals(0.0f, rotated.left, 0.0001f)
        assertEquals(0.0f, rotated.top, 0.0001f)
        assertEquals(0.5f, rotated.right, 0.0001f)
        assertEquals(1.0f, rotated.bottom, 0.0001f)
    }

    @Test
    fun rotateClockwise_fourTimesReturnsToOriginal() {
        val original = ImageCropBounds(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        val fullTurn = original
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()

        assertEquals(original.left, fullTurn.left, 0.001f)
        assertEquals(original.top, fullTurn.top, 0.001f)
        assertEquals(original.right, fullTurn.right, 0.001f)
        assertEquals(original.bottom, fullTurn.bottom, 0.001f)
    }

    @Test
    fun rotateClockwiseThenCounterClockwise_returnsToOriginal() {
        val original = ImageCropBounds(left = 0.15f, top = 0.25f, right = 0.75f, bottom = 0.85f)
        val reverted = original.rotateClockwise().rotateCounterClockwise()

        assertEquals(original.left, reverted.left, 0.001f)
        assertEquals(original.top, reverted.top, 0.001f)
        assertEquals(original.right, reverted.right, 0.001f)
        assertEquals(original.bottom, reverted.bottom, 0.001f)
    }

    @Test
    fun rotate_defaultBoundsStaysDefault() {
        val defaultBounds = ImageCropBounds.DEFAULT
        assertTrue(defaultBounds.rotateClockwise().isDefault)
        assertTrue(defaultBounds.rotateCounterClockwise().isDefault)
    }
}
