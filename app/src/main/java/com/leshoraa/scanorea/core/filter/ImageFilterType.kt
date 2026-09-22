package com.leshoraa.scanorea.core.filter

import android.graphics.ColorMatrix

/**
 * Filter presets for document enhancement and monochrome scanning.
 */
enum class ImageFilterType(val label: String, val shortName: String) {
    ORIGINAL("Original Color", "Color"),
    BLACK_AND_WHITE("B&W Document", "B&W"),
    GRAYSCALE("Grayscale", "Gray"),
    ENHANCED("Magic Color", "Enhanced");

    /**
     * Creates an Android [ColorMatrix] corresponding to the filter with optional [contrast] multiplier
     * and [brightness] offset, or null if no transformation is required.
     */
    fun createColorMatrix(contrast: Float = 1.0f, brightness: Float = 0.0f): ColorMatrix? {
        return when (this) {
            ORIGINAL -> {
                if (contrast == 1.0f && brightness == 0.0f) {
                    null
                } else {
                    ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, brightness,
                            0f, contrast, 0f, 0f, brightness,
                            0f, 0f, contrast, 0f, brightness,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
            }
            BLACK_AND_WHITE -> {
                // High-contrast luminance thresholding to eliminate paper shadows and sharpen ink
                val scale = 2.2f * contrast
                val r = 0.2126f * scale
                val g = 0.7152f * scale
                val b = 0.0722f * scale
                val offset = -120f + brightness

                ColorMatrix(
                    floatArrayOf(
                        r, g, b, 0f, offset,
                        r, g, b, 0f, offset,
                        r, g, b, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            GRAYSCALE -> {
                val grayMatrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                if (contrast != 1.0f || brightness != 0.0f) {
                    val adjMatrix = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, brightness,
                            0f, contrast, 0f, 0f, brightness,
                            0f, 0f, contrast, 0f, brightness,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    adjMatrix.postConcat(grayMatrix)
                    adjMatrix
                } else {
                    grayMatrix
                }
            }
            ENHANCED -> {
                // Gentle contrast and brightness boost for vivid readability
                val c = 1.3f * contrast
                val b = 15f + brightness
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, b,
                        0f, c, 0f, 0f, b,
                        0f, 0f, c, 0f, b,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
    }
}
