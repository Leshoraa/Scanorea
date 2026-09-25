package com.leshoraa.scanorea.features.imagestopdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.editor.domain.PerspectiveWarpCalculator
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

/**
 * Handles decoding of image URIs into memory-optimized bitmaps.
 * Prevents OutOfMemory errors by inspecting image dimensions and applying subsampling.
 */
class ImageDecoderDataSource(private val context: Context) {

    /**
     * Decodes a bitmap from the given [uri], constrained by [maxDimensionPixels] to protect system memory,
     * and optionally applies rotation, crop boundaries, and an [ImageFilterType].
     */
    fun decodeBitmap(
        uri: Uri,
        maxDimensionPixels: Int,
        filter: ImageFilterType = ImageFilterType.BLACK_AND_WHITE,
        contrast: Float = 1.0f,
        brightness: Float = 0.0f,
        rotationDegrees: Int = 0,
        cropBounds: ImageCropBounds = ImageCropBounds.DEFAULT,
        perspectiveQuad: DocumentQuad = DocumentQuad.DEFAULT
    ): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        // First pass: Read dimensions without loading pixel data into RAM
        openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw IOException("Unable to read image dimensions for URI: $uri")
        }

        // Calculate sample size to downscale high-resolution images
        options.inSampleSize = calculateInSampleSize(
            options.outWidth,
            options.outHeight,
            maxDimensionPixels
        )
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        // Second pass: Decode downsampled bitmap
        val decodedBitmap = openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("Failed to decode bitmap stream for URI: $uri")

        // Read EXIF orientation to keep photos right side up
        val orientation = readExifOrientation(uri)
        val orientedBitmap = applyExifOrientation(decodedBitmap, orientation)

        // Apply custom clockwise rotation if specified
        val rotatedBitmap = if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(((rotationDegrees % 360 + 360) % 360).toFloat()) }
            val rotated = Bitmap.createBitmap(orientedBitmap, 0, 0, orientedBitmap.width, orientedBitmap.height, matrix, true)
            if (rotated != orientedBitmap) {
                orientedBitmap.recycle()
            }
            rotated
        } else {
            orientedBitmap
        }

        // Apply perspective keystone rectification if specified
        val warpedBitmap = if (!perspectiveQuad.isDefault && perspectiveQuad.isValidConvex()) {
            val warped = PerspectiveWarpCalculator.warpBitmap(rotatedBitmap, perspectiveQuad)
            if (warped != rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            warped
        } else {
            rotatedBitmap
        }

        // Apply custom normalized crop boundaries if specified
        val croppedBitmap = if (!cropBounds.isDefault) {
            val srcX = (cropBounds.left * warpedBitmap.width).toInt().coerceIn(0, warpedBitmap.width - 1)
            val srcY = (cropBounds.top * warpedBitmap.height).toInt().coerceIn(0, warpedBitmap.height - 1)
            val srcW = ((cropBounds.right - cropBounds.left) * warpedBitmap.width).toInt().coerceIn(1, warpedBitmap.width - srcX)
            val srcH = ((cropBounds.bottom - cropBounds.top) * warpedBitmap.height).toInt().coerceIn(1, warpedBitmap.height - srcY)
            val cropped = Bitmap.createBitmap(warpedBitmap, srcX, srcY, srcW, srcH)
            if (cropped != warpedBitmap) {
                warpedBitmap.recycle()
            }
            cropped
        } else {
            warpedBitmap
        }

        return applyFilter(croppedBitmap, filter, contrast, brightness)
    }

    private fun applyFilter(
        bitmap: Bitmap,
        filter: ImageFilterType,
        contrast: Float = 1.0f,
        brightness: Float = 0.0f
    ): Bitmap {
        val colorMatrix = filter.createColorMatrix(contrast, brightness) ?: return bitmap
        val filteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filteredBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        return filteredBitmap
    }

    private fun openInputStream(uri: Uri): InputStream {
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for URI: $uri")
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val longestEdge = max(width, height)

        while (longestEdge / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }

        return inSampleSize
    }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            openInputStream(uri).use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            // Default to normal orientation when EXIF data is unreadable or absent
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }
}
