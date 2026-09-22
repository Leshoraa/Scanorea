package com.leshoraa.scanorea.features.editor.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds

/**
 * Coil transformation that applies clockwise rotation and normalized crop clipping
 * to page image previews in the editor and grid views.
 */
class PagePreviewTransformation(
    val rotationDegrees: Int = 0,
    val cropBounds: ImageCropBounds = ImageCropBounds.DEFAULT
) : Transformation {

    override val cacheKey: String =
        "page_transform_r${rotationDegrees}_c_${cropBounds.left}_${cropBounds.top}_${cropBounds.right}_${cropBounds.bottom}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val normalizedRotation = (rotationDegrees % 360 + 360) % 360

        // 1. Clockwise Rotation
        val rotated = if (normalizedRotation != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        } else {
            input
        }

        // 2. Normalized Crop Clipping
        val cropped = if (!cropBounds.isDefault) {
            val srcX = (cropBounds.left * rotated.width).toInt().coerceIn(0, rotated.width - 1)
            val srcY = (cropBounds.top * rotated.height).toInt().coerceIn(0, rotated.height - 1)
            val srcW = ((cropBounds.right - cropBounds.left) * rotated.width).toInt().coerceIn(1, rotated.width - srcX)
            val srcH = ((cropBounds.bottom - cropBounds.top) * rotated.height).toInt().coerceIn(1, rotated.height - srcY)
            val result = Bitmap.createBitmap(rotated, srcX, srcY, srcW, srcH)
            if (rotated != input && result != rotated) {
                rotated.recycle()
            }
            result
        } else {
            rotated
        }

        return cropped
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagePreviewTransformation) return false
        return rotationDegrees == other.rotationDegrees && cropBounds == other.cropBounds
    }

    override fun hashCode(): Int {
        var result = rotationDegrees
        result = 31 * result + cropBounds.hashCode()
        return result
    }
}
