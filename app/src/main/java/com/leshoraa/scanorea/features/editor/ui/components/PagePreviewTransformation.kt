package com.leshoraa.scanorea.features.editor.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation
import com.leshoraa.scanorea.features.editor.domain.PerspectiveWarpCalculator
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds

/**
 * Coil transformation that applies clockwise rotation, perspective homography warping,
 * and normalized crop clipping to page image previews in the editor and grid views.
 */
class PagePreviewTransformation(
    val rotationDegrees: Int = 0,
    val cropBounds: ImageCropBounds = ImageCropBounds.DEFAULT,
    val perspectiveQuad: DocumentQuad = DocumentQuad.DEFAULT
) : Transformation {

    override val cacheKey: String =
        "page_transform_r${rotationDegrees}_c_${cropBounds.left}_${cropBounds.top}_${cropBounds.right}_${cropBounds.bottom}_p_${perspectiveQuad.topLeftX}_${perspectiveQuad.topLeftY}_${perspectiveQuad.topRightX}_${perspectiveQuad.topRightY}_${perspectiveQuad.bottomRightX}_${perspectiveQuad.bottomRightY}_${perspectiveQuad.bottomLeftX}_${perspectiveQuad.bottomLeftY}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val normalizedRotation = (rotationDegrees % 360 + 360) % 360

        // 1. Clockwise Rotation
        val rotated = if (normalizedRotation != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        } else {
            input
        }

        // 2. Perspective Keystone Correction
        val warped = if (!perspectiveQuad.isDefault && perspectiveQuad.isValidConvex()) {
            val result = PerspectiveWarpCalculator.warpBitmap(rotated, perspectiveQuad)
            if (rotated != input && result != rotated) {
                rotated.recycle()
            }
            result
        } else {
            rotated
        }

        // 3. Normalized Rectangular Crop Clipping
        val cropped = if (!cropBounds.isDefault) {
            val srcX = (cropBounds.left * warped.width).toInt().coerceIn(0, warped.width - 1)
            val srcY = (cropBounds.top * warped.height).toInt().coerceIn(0, warped.height - 1)
            val srcW = ((cropBounds.right - cropBounds.left) * warped.width).toInt().coerceIn(1, warped.width - srcX)
            val srcH = ((cropBounds.bottom - cropBounds.top) * warped.height).toInt().coerceIn(1, warped.height - srcY)
            val result = Bitmap.createBitmap(warped, srcX, srcY, srcW, srcH)
            if (warped != input && warped != rotated && result != warped) {
                warped.recycle()
            }
            result
        } else {
            warped
        }

        return cropped
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagePreviewTransformation) return false
        return rotationDegrees == other.rotationDegrees &&
                cropBounds == other.cropBounds &&
                perspectiveQuad == other.perspectiveQuad
    }

    override fun hashCode(): Int {
        var result = rotationDegrees
        result = 31 * result + cropBounds.hashCode()
        result = 31 * result + perspectiveQuad.hashCode()
        return result
    }
}
