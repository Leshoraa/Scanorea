package com.leshoraa.scanorea.features.editor.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad

/**
 * Calculates and executes 4-corner perspective transformations (homography warp).
 * Maps an unconstrained quadrilateral document region into a flattened, rectangular bitmap.
 */
object PerspectiveWarpCalculator {

    /**
     * Warps the quadrilateral region defined by [quad] on [sourceBitmap] into a new rectangular [Bitmap].
     *
     * If the quadrilateral is default or degenerate (non-convex), returns [sourceBitmap] unmodified.
     * Uses direct [Canvas.drawBitmap] with a projective homography matrix to guarantee hardware and software rendering accuracy.
     */
    fun warpBitmap(
        sourceBitmap: Bitmap,
        quad: DocumentQuad
    ): Bitmap {
        if (sourceBitmap.isRecycled || quad.isDefault || !quad.isValidConvex()) {
            return sourceBitmap
        }

        val sourceWidth = sourceBitmap.width
        val sourceHeight = sourceBitmap.height
        val targetDimensions = quad.calculateTargetDimensions(sourceWidth, sourceHeight)

        val destinationWidth = targetDimensions.width.toFloat()
        val destinationHeight = targetDimensions.height.toFloat()

        val sourcePoints = floatArrayOf(
            quad.topLeftX * sourceWidth, quad.topLeftY * sourceHeight,
            quad.topRightX * sourceWidth, quad.topRightY * sourceHeight,
            quad.bottomRightX * sourceWidth, quad.bottomRightY * sourceHeight,
            quad.bottomLeftX * sourceWidth, quad.bottomLeftY * sourceHeight
        )

        val destinationPoints = floatArrayOf(
            0.0f, 0.0f,
            destinationWidth, 0.0f,
            destinationWidth, destinationHeight,
            0.0f, destinationHeight
        )

        val mappingMatrix = Matrix()
        val isMatrixComputed = mappingMatrix.setPolyToPoly(
            sourcePoints, 0,
            destinationPoints, 0,
            HOMOGRAPHY_POINT_COUNT
        )

        if (!isMatrixComputed) {
            return sourceBitmap
        }

        val outputBitmap = try {
            Bitmap.createBitmap(
                targetDimensions.width,
                targetDimensions.height,
                Bitmap.Config.ARGB_8888
            )
        } catch (_: OutOfMemoryError) {
            return sourceBitmap
        }

        val canvas = Canvas(outputBitmap)
        val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(sourceBitmap, mappingMatrix, renderPaint)
        return outputBitmap
    }

    private const val HOMOGRAPHY_POINT_COUNT = 4
}
