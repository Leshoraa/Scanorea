package com.leshoraa.scanorea.features.editor.domain

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.leshoraa.scanorea.core.filter.ImageAnalyzer
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fast, memory-safe algorithm to detect quadrilateral document boundaries from image data.
 * Identifies paper borders through contrast analysis and diagonal extremum projections.
 */
object DocumentCornerDetector {

    private const val MAX_ANALYSIS_DIMENSION = 360
    private const val MIN_CONTRAST_DELTA = 15
    private const val MIN_DOCUMENT_AREA_RATIO = 0.12f
    private const val MAX_DOCUMENT_AREA_RATIO = 0.98f
    private const val DEFAULT_INSET_MARGIN = 0.04f

    /**
     * Asynchronously loads a sampled bitmap from [uri] and detects the document quadrilateral.
     */
    suspend fun detectFromUri(
        uri: Uri,
        contentResolver: ContentResolver
    ): DocumentQuad = withContext(Dispatchers.IO) {
        val sampleBitmap = ImageAnalyzer.decodeSampledBitmap(
            uri = uri,
            contentResolver = contentResolver,
            maxDimension = MAX_ANALYSIS_DIMENSION
        ) ?: return@withContext DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)

        try {
            detectDocumentQuad(sampleBitmap)
        } catch (_: Throwable) {
            DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        } finally {
            sampleBitmap.recycle()
        }
    }

    /**
     * Analyzes [sourceBitmap] to detect the four corners of a document.
     * Returns a valid [DocumentQuad]. Falls back gracefully to an inset margin quad if detection is ambiguous.
     */
    fun detectDocumentQuad(sourceBitmap: Bitmap): DocumentQuad {
        val originalWidth = sourceBitmap.width
        val originalHeight = sourceBitmap.height

        if (originalWidth <= 0 || originalHeight <= 0) {
            return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }

        val scaleFactor = (MAX_ANALYSIS_DIMENSION.toFloat() / maxOf(originalWidth, originalHeight)).coerceAtMost(1.0f)
        val scaledWidth = (originalWidth * scaleFactor).roundToInt().coerceAtLeast(32)
        val scaledHeight = (originalHeight * scaleFactor).roundToInt().coerceAtLeast(32)

        val workingBitmap = if (scaledWidth != originalWidth || scaledHeight != originalHeight) {
            Bitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true)
        } else {
            sourceBitmap
        }

        val pixelArray = IntArray(scaledWidth * scaledHeight)
        workingBitmap.getPixels(pixelArray, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

        if (workingBitmap != sourceBitmap) {
            workingBitmap.recycle()
        }

        return detectFromPixelArray(pixelArray, scaledWidth, scaledHeight)
    }

    /**
     * Pure analytical detection operating on a 1D row-major ARGB pixel buffer.
     * Accessible for direct JVM unit testing without graphics hardware dependencies.
     */
    fun detectFromPixelArray(
        pixelArray: IntArray,
        width: Int,
        height: Int
    ): DocumentQuad {
        if (width < 8 || height < 8 || pixelArray.size < width * height) {
            return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }

        val luminanceGrid = IntArray(width * height)
        for (index in pixelArray.indices) {
            val color = pixelArray[index]
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            luminanceGrid[index] = (299 * red + 587 * green + 114 * blue) / 1000
        }

        var borderLuminanceSum = 0L
        var borderPixelCount = 0

        for (x in 0 until width) {
            borderLuminanceSum += luminanceGrid[x] // top edge
            borderLuminanceSum += luminanceGrid[(height - 1) * width + x] // bottom edge
            borderPixelCount += 2
        }
        for (y in 1 until height - 1) {
            borderLuminanceSum += luminanceGrid[y * width] // left edge
            borderLuminanceSum += luminanceGrid[y * width + (width - 1)] // right edge
            borderPixelCount += 2
        }

        val borderAverageLuminance = (borderLuminanceSum / borderPixelCount).toInt()

        val centerStartX = width / 4
        val centerEndX = (width * 3) / 4
        val centerStartY = height / 4
        val centerEndY = (height * 3) / 4

        var centerLuminanceSum = 0L
        var centerPixelCount = 0

        for (y in centerStartY until centerEndY) {
            val rowOffset = y * width
            for (x in centerStartX until centerEndX) {
                centerLuminanceSum += luminanceGrid[rowOffset + x]
                centerPixelCount++
            }
        }

        val centerAverageLuminance = if (centerPixelCount > 0) {
            (centerLuminanceSum / centerPixelCount).toInt()
        } else {
            borderAverageLuminance
        }

        val contrastDelta = abs(centerAverageLuminance - borderAverageLuminance)
        if (contrastDelta < MIN_CONTRAST_DELTA) {
            return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }

        val isPaperBrighterThanBackground = centerAverageLuminance > borderAverageLuminance
        val segmentationThreshold = (borderAverageLuminance + centerAverageLuminance) / 2

        var minSum = Int.MAX_VALUE
        var maxSum = Int.MIN_VALUE
        var minDiff = Int.MAX_VALUE
        var maxDiff = Int.MIN_VALUE

        var bestTopLeftX = 0
        var bestTopLeftY = 0
        var bestBottomRightX = width - 1
        var bestBottomRightY = height - 1
        var bestTopRightX = width - 1
        var bestTopRightY = 0
        var bestBottomLeftX = 0
        var bestBottomLeftY = height - 1

        var documentPixelCount = 0

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val luminance = luminanceGrid[rowOffset + x]
                val isDocumentPixel = if (isPaperBrighterThanBackground) {
                    luminance >= segmentationThreshold
                } else {
                    luminance <= segmentationThreshold
                }

                if (isDocumentPixel) {
                    documentPixelCount++
                    val sum = x + y
                    val diff = x - y

                    if (sum < minSum) {
                        minSum = sum
                        bestTopLeftX = x
                        bestTopLeftY = y
                    }
                    if (sum > maxSum) {
                        maxSum = sum
                        bestBottomRightX = x
                        bestBottomRightY = y
                    }
                    if (diff > maxDiff) {
                        maxDiff = diff
                        bestTopRightX = x
                        bestTopRightY = y
                    }
                    if (diff < minDiff) {
                        minDiff = diff
                        bestBottomLeftX = x
                        bestBottomLeftY = y
                    }
                }
            }
        }

        val totalPixels = width * height
        val documentRatio = documentPixelCount.toFloat() / totalPixels.toFloat()
        if (documentRatio < MIN_DOCUMENT_AREA_RATIO || documentRatio > MAX_DOCUMENT_AREA_RATIO) {
            return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }

        val candidateQuad = DocumentQuad(
            topLeftX = (bestTopLeftX.toFloat() / width.toFloat()).coerceIn(0f, 1f),
            topLeftY = (bestTopLeftY.toFloat() / height.toFloat()).coerceIn(0f, 1f),
            topRightX = (bestTopRightX.toFloat() / width.toFloat()).coerceIn(0f, 1f),
            topRightY = (bestTopRightY.toFloat() / height.toFloat()).coerceIn(0f, 1f),
            bottomRightX = (bestBottomRightX.toFloat() / width.toFloat()).coerceIn(0f, 1f),
            bottomRightY = (bestBottomRightY.toFloat() / height.toFloat()).coerceIn(0f, 1f),
            bottomLeftX = (bestBottomLeftX.toFloat() / width.toFloat()).coerceIn(0f, 1f),
            bottomLeftY = (bestBottomLeftY.toFloat() / height.toFloat()).coerceIn(0f, 1f)
        )

        return if (candidateQuad.isValidConvex()) {
            candidateQuad
        } else {
            DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }
    }
}
