package com.leshoraa.scanorea.features.editor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentQuadTest {

    @Test
    fun defaultQuad_isDefaultReturnsTrue() {
        val quad = DocumentQuad.DEFAULT
        assertTrue(quad.isDefault)
        assertTrue(quad.isValidConvex())
    }

    @Test
    fun customConvexQuad_isValidConvexReturnsTrue() {
        val trapezoidQuad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.1f,
            topRightX = 0.9f, topRightY = 0.15f,
            bottomRightX = 0.85f, bottomRightY = 0.9f,
            bottomLeftX = 0.15f, bottomLeftY = 0.85f
        )
        assertFalse(trapezoidQuad.isDefault)
        assertTrue(trapezoidQuad.isValidConvex())
    }

    @Test
    fun selfIntersectingQuad_isValidConvexReturnsFalse() {
        val bowTieQuad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.1f,
            topRightX = 0.1f, topRightY = 0.9f,
            bottomRightX = 0.9f, bottomRightY = 0.1f,
            bottomLeftX = 0.9f, bottomLeftY = 0.9f
        )
        assertFalse(bowTieQuad.isValidConvex())
    }

    @Test
    fun concaveQuad_isValidConvexReturnsFalse() {
        val arrowHeadQuad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.1f,
            topRightX = 0.9f, topRightY = 0.1f,
            bottomRightX = 0.9f, bottomRightY = 0.9f,
            bottomLeftX = 0.5f, bottomLeftY = 0.5f
        )
        assertFalse(arrowHeadQuad.isValidConvex())
    }

    @Test
    fun withCorner_updatesTargetCornerAndClampsBounds() {
        val initialQuad = DocumentQuad.DEFAULT
        val updatedQuad = initialQuad.withCorner(CornerHandle.TOP_LEFT, -0.2f, 0.3f)

        assertEquals(0.0f, updatedQuad.topLeftX, 1e-4f)
        assertEquals(0.3f, updatedQuad.topLeftY, 1e-4f)
        assertEquals(1.0f, updatedQuad.topRightX, 1e-4f)
    }

    @Test
    fun ofInsetMargin_createsClampedMarginQuad() {
        val insetQuad = DocumentQuad.ofInsetMargin(0.05f)
        assertFalse(insetQuad.isDefault)
        assertTrue(insetQuad.isValidConvex())
        assertEquals(0.05f, insetQuad.topLeftX, 1e-4f)
        assertEquals(0.95f, insetQuad.topRightX, 1e-4f)
        assertEquals(0.95f, insetQuad.bottomRightY, 1e-4f)
        assertEquals(0.05f, insetQuad.bottomLeftX, 1e-4f)
    }

    @Test
    fun calculateTargetDimensions_computesReasonableResolution() {
        val quad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.1f,
            topRightX = 0.9f, topRightY = 0.1f,
            bottomRightX = 0.9f, bottomRightY = 0.9f,
            bottomLeftX = 0.1f, bottomLeftY = 0.9f
        )
        val dimensions = quad.calculateTargetDimensions(1000, 1000)
        assertEquals(800, dimensions.width)
        assertEquals(800, dimensions.height)
    }
}
