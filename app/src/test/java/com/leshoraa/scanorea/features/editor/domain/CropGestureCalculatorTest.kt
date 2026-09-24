package com.leshoraa.scanorea.features.editor.domain

import com.leshoraa.scanorea.features.editor.ui.components.DragHandle
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying CropGestureCalculator boundary calculations.
 */
class CropGestureCalculatorTest {

    @Test
    fun noneHandle_returnsUnchangedBounds() {
        val initial = ImageCropBounds(0.1f, 0.1f, 0.9f, 0.9f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.NONE,
            deltaX = 0.05f,
            deltaY = 0.05f
        )
        assertEquals(initial, result)
    }

    @Test
    fun bodyHandle_translatesBoxWithinBounds() {
        val initial = ImageCropBounds(0.1f, 0.1f, 0.6f, 0.6f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.BODY,
            deltaX = 0.1f,
            deltaY = 0.05f
        )
        assertEquals(0.2f, result.left, 0.001f)
        assertEquals(0.15f, result.top, 0.001f)
        assertEquals(0.7f, result.right, 0.001f)
        assertEquals(0.65f, result.bottom, 0.001f)
    }

    @Test
    fun bodyHandle_clampsAtEdges() {
        val initial = ImageCropBounds(0.8f, 0.8f, 1.0f, 1.0f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.BODY,
            deltaX = 0.5f,
            deltaY = 0.5f
        )
        assertEquals(0.8f, result.left, 0.001f)
        assertEquals(0.8f, result.top, 0.001f)
        assertEquals(1.0f, result.right, 0.001f)
        assertEquals(1.0f, result.bottom, 0.001f)
    }

    @Test
    fun freeAspectRatio_bottomRightDrag_expandsCorrectly() {
        val initial = ImageCropBounds(0.2f, 0.2f, 0.5f, 0.5f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.BOTTOM_RIGHT,
            deltaX = 0.1f,
            deltaY = 0.2f,
            cropAspectRatio = CropAspectRatio.FREE
        )
        assertEquals(0.2f, result.left, 0.001f)
        assertEquals(0.2f, result.top, 0.001f)
        assertEquals(0.6f, result.right, 0.001f)
        assertEquals(0.7f, result.bottom, 0.001f)
    }

    @Test
    fun freeAspectRatio_topLeftDrag_respectsMinSize() {
        val initial = ImageCropBounds(0.2f, 0.2f, 0.5f, 0.5f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.TOP_LEFT,
            deltaX = 0.9f,
            deltaY = 0.9f,
            cropAspectRatio = CropAspectRatio.FREE,
            minSize = 0.1f
        )
        assertTrue(result.right - result.left >= 0.099f)
        assertTrue(result.bottom - result.top >= 0.099f)
    }

    @Test
    fun squareAspectRatio_maintainsEqualProportions() {
        val initial = ImageCropBounds(0.1f, 0.1f, 0.5f, 0.5f)
        val result = CropGestureCalculator.updateBounds(
            currentBounds = initial,
            activeHandle = DragHandle.BOTTOM_RIGHT,
            deltaX = 0.2f,
            deltaY = 0.2f,
            cropAspectRatio = CropAspectRatio.SQUARE,
            effectiveAspectRatio = 1.0f
        )
        val width = result.right - result.left
        val height = result.bottom - result.top
        assertEquals(width, height, 0.01f)
    }
}
