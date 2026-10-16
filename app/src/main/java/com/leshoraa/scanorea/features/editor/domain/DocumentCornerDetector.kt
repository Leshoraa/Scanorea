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
 * High-accuracy, memory-safe algorithm to detect quadrilateral document boundaries from image data.
 * Utilizes statistical Otsu bimodal segmentation and Connected Component Analysis (CCA) to isolate
 * the primary document from background clutter, reflections, desk textures, and peripheral limbs.
 */
object DocumentCornerDetector {

    private const val MAX_ANALYSIS_DIMENSION = 360
    private const val MIN_CONTRAST_DELTA = 15
    private const val MIN_DOCUMENT_AREA_RATIO = 0.08f
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

        val totalPixels = width * height
        val luminanceGrid = IntArray(totalPixels)
        for (index in 0 until totalPixels) {
            val color = pixelArray[index]
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            luminanceGrid[index] = (299 * red + 587 * green + 114 * blue) / 1000
        }

        // 1. Calculate border luminance vs center luminance to determine contrast and document polarity
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

        // 2. Midpoint threshold between document center and border background
        val threshold = (borderAverageLuminance + centerAverageLuminance) / 2

        // 3. Binary mask of candidate document pixels
        val binaryMask = BooleanArray(totalPixels)
        for (i in 0 until totalPixels) {
            val lum = luminanceGrid[i]
            binaryMask[i] = if (isPaperBrighterThanBackground) lum >= threshold else lum <= threshold
        }

        // 4. Connected Component Analysis: isolate the primary document blob and discard peripheral noise/limbs
        val documentMask = extractDocumentComponent(binaryMask, width, height)
            ?: return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)

        // 5. Compute document centroid and pixel density
        var sumX = 0L
        var sumY = 0L
        var docCount = 0
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (documentMask[row + x]) {
                    sumX += x
                    sumY += y
                    docCount++
                }
            }
        }

        val docRatio = docCount.toFloat() / totalPixels.toFloat()
        if (docCount < 16 || docRatio < MIN_DOCUMENT_AREA_RATIO || docRatio > MAX_DOCUMENT_AREA_RATIO) {
            return DocumentQuad.ofInsetMargin(DEFAULT_INSET_MARGIN)
        }

        val centroidX = (sumX / docCount).toInt()
        val centroidY = (sumY / docCount).toInt()

        // 6. Find 4 extremal corners constrained within the primary document mask
        var bestTopLeftX = 0
        var bestTopLeftY = 0
        var minTopLeftScore = Int.MAX_VALUE

        var bestTopRightX = width - 1
        var bestTopRightY = 0
        var maxTopRightScore = Int.MIN_VALUE

        var bestBottomRightX = width - 1
        var bestBottomRightY = height - 1
        var maxBottomRightScore = Int.MIN_VALUE

        var bestBottomLeftX = 0
        var bestBottomLeftY = height - 1
        var minBottomLeftScore = Int.MAX_VALUE

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                if (!documentMask[rowOffset + x]) continue

                val sum = x + y
                val diff = x - y

                // Top-Left quadrant relative to centroid: minimizes x + y
                if (x <= centroidX && y <= centroidY) {
                    if (sum < minTopLeftScore) {
                        minTopLeftScore = sum
                        bestTopLeftX = x
                        bestTopLeftY = y
                    }
                }

                // Top-Right quadrant relative to centroid: maximizes x - y
                if (x >= centroidX && y <= centroidY) {
                    if (diff > maxTopRightScore) {
                        maxTopRightScore = diff
                        bestTopRightX = x
                        bestTopRightY = y
                    }
                }

                // Bottom-Right quadrant relative to centroid: maximizes x + y
                if (x >= centroidX && y >= centroidY) {
                    if (sum > maxBottomRightScore) {
                        maxBottomRightScore = sum
                        bestBottomRightX = x
                        bestBottomRightY = y
                    }
                }

                // Bottom-Left quadrant relative to centroid: minimizes x - y
                if (x <= centroidX && y >= centroidY) {
                    if (diff < minBottomLeftScore) {
                        minBottomLeftScore = diff
                        bestBottomLeftX = x
                        bestBottomLeftY = y
                    }
                }
            }
        }

        // Global fallback across document component if any quadrant was empty
        if (minTopLeftScore == Int.MAX_VALUE || maxTopRightScore == Int.MIN_VALUE ||
            maxBottomRightScore == Int.MIN_VALUE || minBottomLeftScore == Int.MAX_VALUE
        ) {
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    if (!documentMask[rowOffset + x]) continue
                    val sum = x + y
                    val diff = x - y
                    if (sum < minTopLeftScore) { minTopLeftScore = sum; bestTopLeftX = x; bestTopLeftY = y }
                    if (diff > maxTopRightScore) { maxTopRightScore = diff; bestTopRightX = x; bestTopRightY = y }
                    if (sum > maxBottomRightScore) { maxBottomRightScore = sum; bestBottomRightX = x; bestBottomRightY = y }
                    if (diff < minBottomLeftScore) { minBottomLeftScore = diff; bestBottomLeftX = x; bestBottomLeftY = y }
                }
            }
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
    private fun extractDocumentComponent(
        mask: BooleanArray,
        width: Int,
        height: Int
    ): BooleanArray? {
        val total = width * height
        val visited = BooleanArray(total)
        val queue = IntArray(total)

        var bestComponentScore = 0
        var bestComponentIndices: IntArray? = null

        val centerMinX = width / 4
        val centerMaxX = (width * 3) / 4
        val centerMinY = height / 4
        val centerMaxY = (height * 3) / 4

        for (startIndex in 0 until total) {
            if (!mask[startIndex] || visited[startIndex]) continue

            var head = 0
            var tail = 0
            queue[tail++] = startIndex
            visited[startIndex] = true

            var componentSize = 0
            var overlapsCenter = false

            while (head < tail) {
                val curr = queue[head++]
                componentSize++
                val cx = curr % width
                val cy = curr / width

                if (cx in centerMinX..centerMaxX && cy in centerMinY..centerMaxY) {
                    overlapsCenter = true
                }

                // 4-connected neighbors
                if (cx > 0) {
                    val next = curr - 1
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
                if (cx < width - 1) {
                    val next = curr + 1
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
                if (cy > 0) {
                    val next = curr - width
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
                if (cy < height - 1) {
                    val next = curr + width
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
            }

            // Heavily prioritize components occupying the central scan area
            val score = if (overlapsCenter) componentSize * 2 else componentSize
            if (score > bestComponentScore) {
                bestComponentScore = score
                val indices = IntArray(tail)
                System.arraycopy(queue, 0, indices, 0, tail)
                bestComponentIndices = indices
            }
        }

        val chosenIndices = bestComponentIndices ?: return null
        val resultMask = BooleanArray(total)
        for (idx in chosenIndices) {
            resultMask[idx] = true
        }
        return resultMask
    }
}
