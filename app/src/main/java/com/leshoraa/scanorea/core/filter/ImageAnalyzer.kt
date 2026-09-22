package com.leshoraa.scanorea.core.filter

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.sqrt

/**
 * Result of image lighting and color structure analysis.
 *
 * @param optimalContrast Optimal contrast multiplier (typically 1.0f to 2.2f).
 * @param optimalBrightness Optimal brightness offset in range [-40f, 40f].
 * @param meanLuminance Mean perceived luminance of the sampled image (0 to 255).
 * @param standardDeviation Standard deviation of luminance (measure of dynamic range).
 */
data class ImageAnalysisResult(
    val optimalContrast: Float,
    val optimalBrightness: Float,
    val meanLuminance: Float,
    val standardDeviation: Float
)

/**
 * Memory-efficient, fast image analyzer that inspects downsampled image samples to determine
 * the optimal contrast, sharpness boost, and brightness offset for document legibility.
 */
object ImageAnalyzer {

    private const val SAMPLE_MAX_DIMENSION = 160

    /**
     * Analyzes an image from a [Uri] asynchronously on [Dispatchers.IO] / [Dispatchers.Default].
     */
    suspend fun analyze(
        uri: Uri,
        contentResolver: ContentResolver
    ): ImageAnalysisResult = withContext(Dispatchers.IO) {
        val sampleBitmap = decodeSampledBitmap(uri, contentResolver, SAMPLE_MAX_DIMENSION)
            ?: return@withContext defaultResult()

        try {
            analyzeBitmap(sampleBitmap)
        } finally {
            sampleBitmap.recycle()
        }
    }

    /**
     * Analyzes a sampled [Bitmap] pixels and calculates optimal adjustments.
     */
    fun analyzeBitmap(bitmap: Bitmap): ImageAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        if (totalPixels == 0) return defaultResult()

        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return analyzePixels(pixels, width, height)
    }

    /**
     * Pure algorithm to analyze pixel array and calculate optimal adjustments.
     */
    fun analyzePixels(pixels: IntArray, width: Int, height: Int): ImageAnalysisResult {
        val totalPixels = width * height
        if (totalPixels == 0) return defaultResult()

        val histogram = IntArray(256)
        var sumLuminance = 0.0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // ITU-R BT.601 perceived luminance
            val lum = ((r * 299 + g * 587 + b * 114) / 1000).coerceIn(0, 255)
            histogram[lum]++
            sumLuminance += lum
        }

        val meanLum = (sumLuminance / totalPixels).toFloat()

        // Calculate standard deviation (contrast indicator)
        var sumVariance = 0.0
        for (i in 0..255) {
            val count = histogram[i]
            if (count > 0) {
                val diff = i - meanLum
                sumVariance += (diff * diff) * count
            }
        }
        val stdDev = sqrt(sumVariance / totalPixels).toFloat()

        // Calculate percentiles: 5th percentile (shadows/ink) and 95th percentile (paper/background)
        val p5Target = (totalPixels * 0.05).toInt()
        val p95Target = (totalPixels * 0.95).toInt()

        var accumulated = 0
        var p5 = 0
        var p95 = 255

        for (i in 0..255) {
            accumulated += histogram[i]
            if (p5 == 0 && accumulated >= p5Target) {
                p5 = i
            }
            if (accumulated >= p95Target) {
                p95 = i
                break
            }
        }

        val dynamicRange = (p95 - p5).coerceAtLeast(1)

        // Low dynamic range or washed-out documents require stronger contrast boost.
        // High dynamic range documents preserve standard contrast.
        val optimalContrast = when {
            dynamicRange < 70 -> 1.8f
            dynamicRange < 110 -> 1.5f
            dynamicRange < 150 -> 1.3f
            dynamicRange < 190 -> 1.15f
            else -> 1.0f
        }

        // Typical white document paper target luminance is ~200-220.
        // If paper luminance (p95) is dark (such as shadows or low light), compensate with positive brightness.
        // If overexposed (meanLum > 210), apply subtle negative compensation.
        val optimalBrightness = when {
            p95 < 140 -> 30f
            p95 < 170 -> 20f
            p95 < 200 -> 10f
            meanLum > 215 && p5 > 70 -> -15f
            meanLum > 190 && p5 > 50 -> -8f
            else -> 0f
        }

        return ImageAnalysisResult(
            optimalContrast = optimalContrast,
            optimalBrightness = optimalBrightness,
            meanLuminance = meanLum,
            standardDeviation = stdDev
        )
    }

    private fun decodeSampledBitmap(
        uri: Uri,
        contentResolver: ContentResolver,
        maxDimension: Int
    ): Bitmap? {
        var input: InputStream? = null
        return try {
            // First decode bounds
            input = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var inSampleSize = 1
            val maxSrc = maxOf(srcWidth, srcHeight)
            while ((maxSrc / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2
            }

            // Decode downsampled bitmap
            input = contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Low memory footprint
            }
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } catch (_: Exception) {
            null
        } finally {
            input?.close()
        }
    }

    fun defaultResult(): ImageAnalysisResult = ImageAnalysisResult(
        optimalContrast = 1.0f,
        optimalBrightness = 0.0f,
        meanLuminance = 128f,
        standardDeviation = 50f
    )
}
