package com.leshoraa.scanorea.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [ImageAnalyzer] lighting and dynamic range auto-calibration algorithms.
 */
class ImageAnalyzerTest {

    @Test
    fun analyzePixels_emptyOrZeroPixels_returnsDefaultResult() {
        val result = ImageAnalyzer.analyzePixels(IntArray(0), 0, 0)
        assertEquals(1.0f, result.optimalContrast)
        assertEquals(0.0f, result.optimalBrightness)
    }

    @Test
    fun analyzePixels_darkUnderexposedDocument_boostsBrightness() {
        // Create an image where luminance is dark (average ~50)
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) {
            // Dark gray/shadow paper: R=60, G=60, B=60
            0xFF3C3C3C.toInt()
        }

        val result = ImageAnalyzer.analyzePixels(pixels, width, height)

        // p95 < 140, so brightness should be boosted significantly (+30f)
        assertTrue("Brightness should be boosted for dark document", result.optimalBrightness >= 20f)
    }

    @Test
    fun analyzePixels_lowContrastWashedOut_boostsContrast() {
        // Low contrast: all pixels concentrated in a narrow band (e.g. 120-140)
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { idx ->
            val v = 120 + (idx % 20)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

        val result = ImageAnalyzer.analyzePixels(pixels, width, height)

        // Dynamic range < 70, contrast should be high
        assertTrue("Contrast should be boosted for low contrast images", result.optimalContrast >= 1.5f)
    }

    @Test
    fun analyzePixels_wellBalancedDocument_keepsNaturalContrast() {
        // Standard high dynamic range document: dark text (10%) and white paper (90%)
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { idx ->
            if (idx % 10 == 0) {
                // Dark ink: 0x101010
                0xFF101010.toInt()
            } else {
                // Crisp white paper: 0xF5F5F5 (luminance 245)
                0xFFF5F5F5.toInt()
            }
        }

        val result = ImageAnalyzer.analyzePixels(pixels, width, height)

        // High dynamic range (> 190), contrast should be near 1.0f
        assertEquals(1.0f, result.optimalContrast)
        // Paper is already bright white, no need for positive brightness boost
        assertTrue("Brightness should not be boosted when paper is already crisp white", result.optimalBrightness <= 0f)
    }
}
