package com.leshoraa.scanorea.features.editor.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation
import com.leshoraa.scanorea.core.filter.DocumentSharpnessFilter
import com.leshoraa.scanorea.features.editor.domain.PerspectiveWarpCalculator
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds

/**
 * Coil transformation that applies clockwise rotation, perspective homography warping,
 * normalized crop clipping, and adaptive sharpness enhancement to page previews.
 */
class PagePreviewTransformation(
    val rotationDegrees: Int = 0,
    val cropBounds: ImageCropBounds = ImageCropBounds.DEFAULT,
    val perspectiveQuad: DocumentQuad = DocumentQuad.DEFAULT,
    val sharpness: Float = 0.0f
) : Transformation {

    override val cacheKey: String =
        "page_transform_r${rotationDegrees}_c_${cropBounds.left}_${cropBounds.top}_${cropBounds.right}_${cropBounds.bottom}_p_${perspectiveQuad.topLeftX}_${perspectiveQuad.topLeftY}_${perspectiveQuad.topRightX}_${perspectiveQuad.topRightY}_${perspectiveQuad.bottomRightX}_${perspectiveQuad.bottomRightY}_${perspectiveQuad.bottomLeftX}_${perspectiveQuad.bottomLeftY}_s_$sharpness"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val normalizedRotation = (rotationDegrees % 360 + 360) % 360
        val intermediateBitmaps = mutableListOf<Bitmap>()

        // Clockwise Rotation
        val rotated = if (normalizedRotation != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            val rot = Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
            if (rot != input) {
                intermediateBitmaps.add(rot)
            }
            rot
        } else {
            input
        }

        // Perspective Keystone Correction
        val warped = if (!perspectiveQuad.isDefault && perspectiveQuad.isValidConvex()) {
            val warp = PerspectiveWarpCalculator.warpBitmap(rotated, perspectiveQuad)
            if (warp != input && warp != rotated) {
                intermediateBitmaps.add(warp)
            }
            warp
        } else {
            rotated
        }

        // Normalized Rectangular Crop Clipping
        val cropped = if (!cropBounds.isDefault) {
            val srcX = (cropBounds.left * warped.width).toInt().coerceIn(0, warped.width - 1)
            val srcY = (cropBounds.top * warped.height).toInt().coerceIn(0, warped.height - 1)
            val srcW = ((cropBounds.right - cropBounds.left) * warped.width).toInt().coerceIn(1, warped.width - srcX)
            val srcH = ((cropBounds.bottom - cropBounds.top) * warped.height).toInt().coerceIn(1, warped.height - srcY)
            val cropBmp = Bitmap.createBitmap(warped, srcX, srcY, srcW, srcH)
            if (cropBmp != input && cropBmp != warped) {
                intermediateBitmaps.add(cropBmp)
            }
            cropBmp
        } else {
            warped
        }

        // Adaptive Document Sharpness Enhancement
        val finalResult = if (sharpness > 0.01f) {
            DocumentSharpnessFilter.apply(cropped, sharpness)
        } else {
            cropped
        }

        // Clean up intermediate bitmaps that are not the input and not the final result
        intermediateBitmaps.forEach { bmp ->
            if (bmp != input && bmp != finalResult && !bmp.isRecycled) {
                bmp.recycle()
            }
        }

        return finalResult
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagePreviewTransformation) return false
        return rotationDegrees == other.rotationDegrees &&
                cropBounds == other.cropBounds &&
                perspectiveQuad == other.perspectiveQuad &&
                sharpness == other.sharpness
    }

    override fun hashCode(): Int {
        var result = rotationDegrees
        result = 31 * result + cropBounds.hashCode()
        result = 31 * result + perspectiveQuad.hashCode()
        result = 31 * result + sharpness.hashCode()
        return result
    }
}
