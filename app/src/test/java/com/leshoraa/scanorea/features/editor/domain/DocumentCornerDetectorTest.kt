package com.leshoraa.scanorea.features.editor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCornerDetectorTest {

    @Test
    fun detectFromPixelArray_uniformColor_returnsInsetMarginFallback() {
        val width = 100
        val height = 100
        val uniformPixels = IntArray(width * height) { 0xFF888888.toInt() }

        val detectedQuad = DocumentCornerDetector.detectFromPixelArray(uniformPixels, width, height)
        assertTrue(detectedQuad.isValidConvex())
        assertEquals(0.04f, detectedQuad.topLeftX, 0.01f)
        assertEquals(0.04f, detectedQuad.topLeftY, 0.01f)
    }

    @Test
    fun detectFromPixelArray_brightDocumentOnDarkTable_detectsCornersAccurately() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { 0xFF222222.toInt() } // Dark background

        // Draw white paper from (20, 20) to (80, 80)
        for (y in 20 until 80) {
            for (x in 20 until 80) {
                pixels[y * width + x] = 0xFFFFFFFF.toInt()
            }
        }

        val detectedQuad = DocumentCornerDetector.detectFromPixelArray(pixels, width, height)
        assertTrue(detectedQuad.isValidConvex())

        assertEquals(0.20f, detectedQuad.topLeftX, 0.03f)
        assertEquals(0.20f, detectedQuad.topLeftY, 0.03f)
        assertEquals(0.79f, detectedQuad.topRightX, 0.03f)
        assertEquals(0.20f, detectedQuad.topRightY, 0.03f)
        assertEquals(0.79f, detectedQuad.bottomRightX, 0.03f)
        assertEquals(0.79f, detectedQuad.bottomRightY, 0.03f)
        assertEquals(0.20f, detectedQuad.bottomLeftX, 0.03f)
        assertEquals(0.79f, detectedQuad.bottomLeftY, 0.03f)
    }

    @Test
    fun detectFromPixelArray_darkDocumentOnLightTable_detectsCornersAccurately() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { 0xFFEEEEEE.toInt() } // Light background

        // Draw dark document from (15, 15) to (85, 85)
        for (y in 15 until 85) {
            for (x in 15 until 85) {
                pixels[y * width + x] = 0xFF111111.toInt()
            }
        }

        val detectedQuad = DocumentCornerDetector.detectFromPixelArray(pixels, width, height)
        assertTrue(detectedQuad.isValidConvex())

        assertEquals(0.15f, detectedQuad.topLeftX, 0.03f)
        assertEquals(0.15f, detectedQuad.topLeftY, 0.03f)
        assertEquals(0.84f, detectedQuad.topRightX, 0.03f)
        assertEquals(0.15f, detectedQuad.topRightY, 0.03f)
    }
}
