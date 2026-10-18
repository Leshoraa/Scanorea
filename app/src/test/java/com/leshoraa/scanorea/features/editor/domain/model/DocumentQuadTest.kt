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

    @Test
    fun rotateClockwise_rotatesCornersCorrectly() {
        val quad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.2f,
            topRightX = 0.8f, topRightY = 0.2f,
            bottomRightX = 0.8f, bottomRightY = 0.9f,
            bottomLeftX = 0.1f, bottomLeftY = 0.9f
        )
        val rotated = quad.rotateClockwise()

        // Clockwise maps (x, y) to (1 - y, x), cycling BL -> TL, TL -> TR, TR -> BR, BR -> BL
        assertEquals(0.1f, rotated.topLeftX, 1e-4f)
        assertEquals(0.1f, rotated.topLeftY, 1e-4f)
        assertEquals(0.8f, rotated.topRightX, 1e-4f)
        assertEquals(0.1f, rotated.topRightY, 1e-4f)
        assertEquals(0.8f, rotated.bottomRightX, 1e-4f)
        assertEquals(0.8f, rotated.bottomRightY, 1e-4f)
        assertEquals(0.1f, rotated.bottomLeftX, 1e-4f)
        assertEquals(0.8f, rotated.bottomLeftY, 1e-4f)
        assertTrue(rotated.isValidConvex())
    }

    @Test
    fun rotateCounterClockwise_rotatesCornersCorrectly() {
        val quad = DocumentQuad(
            topLeftX = 0.1f, topLeftY = 0.2f,
            topRightX = 0.8f, topRightY = 0.2f,
            bottomRightX = 0.8f, bottomRightY = 0.9f,
            bottomLeftX = 0.1f, bottomLeftY = 0.9f
        )
        val rotated = quad.rotateCounterClockwise()

        // Counter-clockwise maps (x, y) to (y, 1 - x), cycling TR -> TL, BR -> TR, BL -> BR, TL -> BL
        assertEquals(0.2f, rotated.topLeftX, 1e-4f)
        assertEquals(0.2f, rotated.topLeftY, 1e-4f)
        assertEquals(0.9f, rotated.topRightX, 1e-4f)
        assertEquals(0.2f, rotated.topRightY, 1e-4f)
        assertEquals(0.9f, rotated.bottomRightX, 1e-4f)
        assertEquals(0.9f, rotated.bottomRightY, 1e-4f)
        assertEquals(0.2f, rotated.bottomLeftX, 1e-4f)
        assertEquals(0.9f, rotated.bottomLeftY, 1e-4f)
        assertTrue(rotated.isValidConvex())
    }

    @Test
    fun rotateClockwise_fourTimesReturnsToOriginal() {
        val quad = DocumentQuad(
            topLeftX = 0.15f, topLeftY = 0.18f,
            topRightX = 0.82f, topRightY = 0.22f,
            bottomRightX = 0.79f, bottomRightY = 0.88f,
            bottomLeftX = 0.12f, bottomLeftY = 0.85f
        )
        val fullTurn = quad
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()

        assertEquals(quad.topLeftX, fullTurn.topLeftX, 1e-4f)
        assertEquals(quad.topLeftY, fullTurn.topLeftY, 1e-4f)
        assertEquals(quad.topRightX, fullTurn.topRightX, 1e-4f)
        assertEquals(quad.topRightY, fullTurn.topRightY, 1e-4f)
        assertEquals(quad.bottomRightX, fullTurn.bottomRightX, 1e-4f)
        assertEquals(quad.bottomRightY, fullTurn.bottomRightY, 1e-4f)
        assertEquals(quad.bottomLeftX, fullTurn.bottomLeftX, 1e-4f)
        assertEquals(quad.bottomLeftY, fullTurn.bottomLeftY, 1e-4f)
    }

    @Test
    fun rotateClockwiseThenCounterClockwise_returnsToOriginal() {
        val quad = DocumentQuad(
            topLeftX = 0.12f, topLeftY = 0.14f,
            topRightX = 0.85f, topRightY = 0.16f,
            bottomRightX = 0.88f, bottomRightY = 0.92f,
            bottomLeftX = 0.08f, bottomLeftY = 0.89f
        )
        val reverted = quad.rotateClockwise().rotateCounterClockwise()

        assertEquals(quad.topLeftX, reverted.topLeftX, 1e-4f)
        assertEquals(quad.topLeftY, reverted.topLeftY, 1e-4f)
        assertEquals(quad.topRightX, reverted.topRightX, 1e-4f)
        assertEquals(quad.topRightY, reverted.topRightY, 1e-4f)
        assertEquals(quad.bottomRightX, reverted.bottomRightX, 1e-4f)
        assertEquals(quad.bottomRightY, reverted.bottomRightY, 1e-4f)
        assertEquals(quad.bottomLeftX, reverted.bottomLeftX, 1e-4f)
        assertEquals(quad.bottomLeftY, reverted.bottomLeftY, 1e-4f)
    }

    @Test
    fun rotate_defaultQuadStaysDefault() {
        val defaultQuad = DocumentQuad.DEFAULT
        assertTrue(defaultQuad.rotateClockwise().isDefault)
        assertTrue(defaultQuad.rotateCounterClockwise().isDefault)
    }
}
