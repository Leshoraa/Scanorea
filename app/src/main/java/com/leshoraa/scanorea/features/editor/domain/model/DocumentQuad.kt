package com.leshoraa.scanorea.features.editor.domain.model

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Identifies the four movable corner handles of a document quadrilateral.
 */
enum class CornerHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    NONE
}

/**
 * Immutable model representing four normalized corner coordinates of a document in 2D space.
 * All coordinates are normalized in the range [0.0f, 1.0f].
 *
 * Ordered clockwise:
 * - Top-Left: (topLeftX, topLeftY)
 * - Top-Right: (topRightX, topRightY)
 * - Bottom-Right: (bottomRightX, bottomRightY)
 * - Bottom-Left: (bottomLeftX, bottomLeftY)
 */
data class DocumentQuad(
    val topLeftX: Float = DEFAULT_TOP_LEFT_X,
    val topLeftY: Float = DEFAULT_TOP_LEFT_Y,
    val topRightX: Float = DEFAULT_TOP_RIGHT_X,
    val topRightY: Float = DEFAULT_TOP_RIGHT_Y,
    val bottomRightX: Float = DEFAULT_BOTTOM_RIGHT_X,
    val bottomRightY: Float = DEFAULT_BOTTOM_RIGHT_Y,
    val bottomLeftX: Float = DEFAULT_BOTTOM_LEFT_X,
    val bottomLeftY: Float = DEFAULT_BOTTOM_LEFT_Y
) {
    /**
     * Checks if the quadrilateral matches the default full-image boundary.
     */
    val isDefault: Boolean
        get() = topLeftX == DEFAULT_TOP_LEFT_X &&
                topLeftY == DEFAULT_TOP_LEFT_Y &&
                topRightX == DEFAULT_TOP_RIGHT_X &&
                topRightY == DEFAULT_TOP_RIGHT_Y &&
                bottomRightX == DEFAULT_BOTTOM_RIGHT_X &&
                bottomRightY == DEFAULT_BOTTOM_RIGHT_Y &&
                bottomLeftX == DEFAULT_BOTTOM_LEFT_X &&
                bottomLeftY == DEFAULT_BOTTOM_LEFT_Y

    /**
     * Validates whether the four vertices form a non-degenerate convex polygon
     * with positive area above the minimum threshold.
     */
    fun isValidConvex(): Boolean {
        val crossProduct0 = crossProduct2D(
            topRightX - topLeftX, topRightY - topLeftY,
            bottomRightX - topRightX, bottomRightY - topRightY
        )
        val crossProduct1 = crossProduct2D(
            bottomRightX - topRightX, bottomRightY - topRightY,
            bottomLeftX - bottomRightX, bottomLeftY - bottomRightY
        )
        val crossProduct2 = crossProduct2D(
            bottomLeftX - bottomRightX, bottomLeftY - bottomRightY,
            topLeftX - bottomLeftX, topLeftY - bottomLeftY
        )
        val crossProduct3 = crossProduct2D(
            topLeftX - bottomLeftX, topLeftY - bottomLeftY,
            topRightX - topLeftX, topRightY - topLeftY
        )

        val allPositive = crossProduct0 > EPSILON && crossProduct1 > EPSILON && crossProduct2 > EPSILON && crossProduct3 > EPSILON
        val allNegative = crossProduct0 < -EPSILON && crossProduct1 < -EPSILON && crossProduct2 < -EPSILON && crossProduct3 < -EPSILON

        if (!allPositive && !allNegative) return false

        val estimatedArea = calculatePolygonArea()
        return estimatedArea >= MIN_POLYGON_AREA
    }

    /**
     * Estimates the pixel dimensions of the rectified document based on physical edge lengths.
     */
    fun calculateTargetDimensions(imageWidth: Int, imageHeight: Int): TargetDimensions {
        val topWidth = hypot((topRightX - topLeftX) * imageWidth, (topRightY - topLeftY) * imageHeight)
        val bottomWidth = hypot((bottomRightX - bottomLeftX) * imageWidth, (bottomRightY - bottomLeftY) * imageHeight)
        val leftHeight = hypot((bottomLeftX - topLeftX) * imageWidth, (bottomLeftY - topLeftY) * imageHeight)
        val rightHeight = hypot((bottomRightX - topRightX) * imageWidth, (bottomRightY - topRightY) * imageHeight)

        val resolvedWidth = maxOf(topWidth, bottomWidth).roundToInt().coerceIn(MIN_PIXEL_DIMENSION, imageWidth * 2)
        val resolvedHeight = maxOf(leftHeight, rightHeight).roundToInt().coerceIn(MIN_PIXEL_DIMENSION, imageHeight * 2)

        return TargetDimensions(resolvedWidth, resolvedHeight)
    }

    /**
     * Returns an updated [DocumentQuad] with a specific corner moved to (newX, newY).
     */
    fun withCorner(handle: CornerHandle, newX: Float, newY: Float): DocumentQuad {
        val clampedX = newX.coerceIn(MIN_COORDINATE, MAX_COORDINATE)
        val clampedY = newY.coerceIn(MIN_COORDINATE, MAX_COORDINATE)

        return when (handle) {
            CornerHandle.TOP_LEFT -> copy(topLeftX = clampedX, topLeftY = clampedY)
            CornerHandle.TOP_RIGHT -> copy(topRightX = clampedX, topRightY = clampedY)
            CornerHandle.BOTTOM_RIGHT -> copy(bottomRightX = clampedX, bottomRightY = clampedY)
            CornerHandle.BOTTOM_LEFT -> copy(bottomLeftX = clampedX, bottomLeftY = clampedY)
            CornerHandle.NONE -> this
        }
    }

    /**
     * Returns the normalized coordinates for a specified corner handle.
     */
    fun getCorner(handle: CornerHandle): Pair<Float, Float> = when (handle) {
        CornerHandle.TOP_LEFT -> Pair(topLeftX, topLeftY)
        CornerHandle.TOP_RIGHT -> Pair(topRightX, topRightY)
        CornerHandle.BOTTOM_RIGHT -> Pair(bottomRightX, bottomRightY)
        CornerHandle.BOTTOM_LEFT -> Pair(bottomLeftX, bottomLeftY)
        CornerHandle.NONE -> Pair(0.5f, 0.5f)
    }

    /**
     * Calculates the area of the quadrilateral using the Shoelace formula.
     */
    fun calculatePolygonArea(): Float {
        val shoelace = (topLeftX * topRightY - topLeftY * topRightX) +
                (topRightX * bottomRightY - topRightY * bottomRightX) +
                (bottomRightX * bottomLeftY - bottomRightY * bottomLeftX) +
                (bottomLeftX * topLeftY - bottomLeftY * topLeftX)
        return kotlin.math.abs(shoelace) * 0.5f
    }

    data class TargetDimensions(val width: Int, val height: Int)

    companion object {
        private const val DEFAULT_TOP_LEFT_X = 0.0f
        private const val DEFAULT_TOP_LEFT_Y = 0.0f
        private const val DEFAULT_TOP_RIGHT_X = 1.0f
        private const val DEFAULT_TOP_RIGHT_Y = 0.0f
        private const val DEFAULT_BOTTOM_RIGHT_X = 1.0f
        private const val DEFAULT_BOTTOM_RIGHT_Y = 1.0f
        private const val DEFAULT_BOTTOM_LEFT_X = 0.0f
        private const val DEFAULT_BOTTOM_LEFT_Y = 1.0f

        const val MIN_COORDINATE = 0.0f
        const val MAX_COORDINATE = 1.0f
        private const val EPSILON = 1e-5f
        private const val MIN_POLYGON_AREA = 0.02f
        private const val MIN_PIXEL_DIMENSION = 32

        val DEFAULT = DocumentQuad()

        /**
         * Creates an inset quadrilateral with a uniform margin from outer edges.
         */
        fun ofInsetMargin(marginFraction: Float = 0.05f): DocumentQuad {
            val clampedMargin = marginFraction.coerceIn(0.01f, 0.30f)
            return DocumentQuad(
                topLeftX = clampedMargin,
                topLeftY = clampedMargin,
                topRightX = 1.0f - clampedMargin,
                topRightY = clampedMargin,
                bottomRightX = 1.0f - clampedMargin,
                bottomRightY = 1.0f - clampedMargin,
                bottomLeftX = clampedMargin,
                bottomLeftY = 1.0f - clampedMargin
            )
        }

        private fun crossProduct2D(ax: Float, ay: Float, bx: Float, by: Float): Float {
            return ax * by - ay * bx
        }
    }
}
