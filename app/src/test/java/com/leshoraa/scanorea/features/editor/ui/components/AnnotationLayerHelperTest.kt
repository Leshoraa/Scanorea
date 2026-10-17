package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying AnnotationLayerHelper geometric calculations, resizing constraints,
 * and hit testing for markup layers.
 */
class AnnotationLayerHelperTest {

    @Test
    fun getNormalizedBounds_rectBoxStandard_returnsCorrectRect() {
        val rectBox = PageAnnotation.RectBox(
            color = 0xFF000000,
            strokeWidth = 2f,
            left = 0.2f,
            top = 0.3f,
            right = 0.7f,
            bottom = 0.8f,
            isFilled = false
        )
        val bounds = AnnotationLayerHelper.getNormalizedBounds(rectBox)
        assertNotNull(bounds)
        assertEquals(0.2f, bounds!!.left, 0.0001f)
        assertEquals(0.3f, bounds.top, 0.0001f)
        assertEquals(0.7f, bounds.right, 0.0001f)
        assertEquals(0.8f, bounds.bottom, 0.0001f)
    }

    @Test
    fun getNormalizedBounds_rectBoxInverted_normalizesWithMinMax() {
        val invertedBox = PageAnnotation.RectBox(
            color = 0xFF000000,
            strokeWidth = 2f,
            left = 0.8f,
            top = 0.9f,
            right = 0.2f,
            bottom = 0.3f,
            isFilled = false
        )
        val bounds = AnnotationLayerHelper.getNormalizedBounds(invertedBox)
        assertNotNull(bounds)
        assertEquals(0.2f, bounds!!.left, 0.0001f)
        assertEquals(0.3f, bounds.top, 0.0001f)
        assertEquals(0.8f, bounds.right, 0.0001f)
        assertEquals(0.9f, bounds.bottom, 0.0001f)
    }

    @Test
    fun getNormalizedBounds_freehandPath_returnsNull() {
        val freehand = PageAnnotation.FreehandPath(
            color = 0xFF000000,
            strokeWidth = 2f,
            points = emptyList()
        )
        assertNull(AnnotationLayerHelper.getNormalizedBounds(freehand))
    }

    @Test
    fun updateBounds_rectBox_updatesCoordinatesCorrectly() {
        val initial = PageAnnotation.RectBox(
            color = 0xFF123456,
            strokeWidth = 2f,
            left = 0.1f,
            top = 0.1f,
            right = 0.5f,
            bottom = 0.5f,
            isFilled = false
        )
        val newBounds = Rect(0.2f, 0.25f, 0.8f, 0.9f)
        val updated = AnnotationLayerHelper.updateBounds(initial, newBounds) as PageAnnotation.RectBox
        assertEquals(0.2f, updated.left, 0.0001f)
        assertEquals(0.25f, updated.top, 0.0001f)
        assertEquals(0.8f, updated.right, 0.0001f)
        assertEquals(0.9f, updated.bottom, 0.0001f)
    }

    @Test
    fun calculateResizedBounds_bodyDrag_translatesPreservingDimensions() {
        val bounds = Rect(0.2f, 0.2f, 0.5f, 0.6f) // w=0.3, h=0.4
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.BODY,
            deltaX = 0.1f,
            deltaY = 0.05f
        )
        assertEquals(0.3f, result.left, 0.0001f)
        assertEquals(0.25f, result.top, 0.0001f)
        assertEquals(0.6f, result.right, 0.0001f)
        assertEquals(0.65f, result.bottom, 0.0001f)
        assertEquals(0.3f, result.width, 0.0001f)
        assertEquals(0.4f, result.height, 0.0001f)
    }

    @Test
    fun calculateResizedBounds_bodyDrag_clampsToZeroOne() {
        val bounds = Rect(0.8f, 0.8f, 1.0f, 1.0f) // w=0.2, h=0.2
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.BODY,
            deltaX = 0.5f,
            deltaY = 0.5f
        )
        assertEquals(0.8f, result.left, 0.0001f)
        assertEquals(0.8f, result.top, 0.0001f)
        assertEquals(1.0f, result.right, 0.0001f)
        assertEquals(1.0f, result.bottom, 0.0001f)
    }

    @Test
    fun calculateResizedBounds_bottomRightCorner_resizesWidthAndHeight() {
        val bounds = Rect(0.2f, 0.2f, 0.5f, 0.5f)
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.BOTTOM_RIGHT,
            deltaX = 0.1f,
            deltaY = 0.15f
        )
        assertEquals(0.2f, result.left, 0.0001f)
        assertEquals(0.2f, result.top, 0.0001f)
        assertEquals(0.6f, result.right, 0.0001f)
        assertEquals(0.65f, result.bottom, 0.0001f)
    }

    @Test
    fun calculateResizedBounds_corner_respectsMinSize() {
        val bounds = Rect(0.2f, 0.2f, 0.5f, 0.5f)
        // Drag bottom right far up-left past the top-left
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.BOTTOM_RIGHT,
            deltaX = -0.5f,
            deltaY = -0.5f,
            minSize = 0.05f
        )
        assertTrue(result.right >= result.left + 0.05f)
        assertTrue(result.bottom >= result.top + 0.05f)
    }

    @Test
    fun calculateResizedBounds_topEdge_changesOnlyTop() {
        val bounds = Rect(0.2f, 0.3f, 0.6f, 0.7f)
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.TOP_EDGE,
            deltaX = 0.1f, // deltaX should be ignored for top edge
            deltaY = -0.1f
        )
        assertEquals(0.2f, result.left, 0.0001f)
        assertEquals(0.2f, result.top, 0.0001f)
        assertEquals(0.6f, result.right, 0.0001f)
        assertEquals(0.7f, result.bottom, 0.0001f)
    }

    @Test
    fun calculateResizedBounds_rightEdge_changesOnlyRight() {
        val bounds = Rect(0.2f, 0.3f, 0.6f, 0.7f)
        val result = AnnotationLayerHelper.calculateResizedBounds(
            bounds = bounds,
            handle = AnnotationResizeHandle.RIGHT_EDGE,
            deltaX = 0.15f,
            deltaY = 0.2f // deltaY should be ignored for right edge
        )
        assertEquals(0.2f, result.left, 0.0001f)
        assertEquals(0.3f, result.top, 0.0001f)
        assertEquals(0.75f, result.right, 0.0001f)
        assertEquals(0.7f, result.bottom, 0.0001f)
    }

    @Test
    fun hitTestHandle_detectsCornersAndEdgesAndBody() {
        val bounds = Rect(0.2f, 0.2f, 0.8f, 0.8f)
        val imgLeft = 0f
        val imgTop = 0f
        val imgW = 1000f
        val imgH = 1000f
        val cornerRadius = 30f
        val edgeRadius = 30f

        // Screen box: [200, 200, 800, 800]
        // Top-Left corner is at (200, 200)
        assertEquals(
            AnnotationResizeHandle.TOP_LEFT,
            AnnotationLayerHelper.hitTestHandle(Offset(205f, 205f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )

        // Bottom-Right corner is at (800, 800)
        assertEquals(
            AnnotationResizeHandle.BOTTOM_RIGHT,
            AnnotationLayerHelper.hitTestHandle(Offset(795f, 795f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )

        // Top edge midpoint is at (500, 200)
        assertEquals(
            AnnotationResizeHandle.TOP_EDGE,
            AnnotationLayerHelper.hitTestHandle(Offset(500f, 205f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )

        // Left edge midpoint is at (200, 500)
        assertEquals(
            AnnotationResizeHandle.LEFT_EDGE,
            AnnotationLayerHelper.hitTestHandle(Offset(205f, 500f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )

        // Center of box (500, 500) should be BODY
        assertEquals(
            AnnotationResizeHandle.BODY,
            AnnotationLayerHelper.hitTestHandle(Offset(500f, 500f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )

        // Far outside (50, 50) should be NONE
        assertEquals(
            AnnotationResizeHandle.NONE,
            AnnotationLayerHelper.hitTestHandle(Offset(50f, 50f), bounds, imgLeft, imgTop, imgW, imgH, cornerRadius, edgeRadius)
        )
    }

    @Test
    fun isTouchOnToolbar_detectsToolbarHitCorrectly() {
        val bounds = Rect(0.2f, 0.3f, 0.8f, 0.7f)
        val imgLeft = 0f
        val imgTop = 0f
        val imgW = 1000f
        val imgH = 1000f
        val density = 2f

        // screenTop = 300f. Since 300f > toolbarH + margin, toolbar is placed above screenTop (around y = 300 - 80 - 16 = 204)
        // MidX is 500f
        val insideToolbar = Offset(500f, 215f)
        assertTrue(AnnotationLayerHelper.isTouchOnToolbar(insideToolbar, bounds, imgLeft, imgTop, imgW, imgH, density))

        val farAway = Offset(100f, 100f)
        assertFalse(AnnotationLayerHelper.isTouchOnToolbar(farAway, bounds, imgLeft, imgTop, imgW, imgH, density))
    }
}
