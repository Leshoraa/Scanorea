package com.leshoraa.scanorea.core.filter

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Adaptive edge-enhancing unsharp mask filter designed specifically for text and document clarity.
 * Enhances edge definition and stroke contrast while suppressing background noise and preventing clipping or halo artifacts.
 */
object DocumentSharpnessFilter {

    /**
     * Applies adaptive edge sharpening to the input bitmap with sharpness strength in range 0.0f..1.0f.
     * Returns the same input bitmap if sharpness is at or below the minimum threshold.
     */
    fun apply(input: Bitmap, sharpness: Float): Bitmap {
        if (sharpness <= 0.01f) return input

        val width = input.width
        val height = input.height
        if (width < 3 || height < 3) return input

        val safeInput = try {
            if (input.isRecycled) return input
            if (input.config != Bitmap.Config.ARGB_8888) {
                input.copy(Bitmap.Config.ARGB_8888, false) ?: input
            } else {
                input
            }
        } catch (_: Throwable) {
            return input
        }

        val pixels = IntArray(width * height)
        try {
            safeInput.getPixels(pixels, 0, width, 0, 0, width, height)
        } catch (_: Throwable) {
            try {
                val copy = input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
                copy.getPixels(pixels, 0, width, 0, 0, width, height)
            } catch (_: Throwable) {
                return input
            }
        }

        val outputPixels = IntArray(width * height)
        val strength = sharpness * 4.5f
        val samplingStep = (minOf(width, height) / 450).coerceIn(1, 3)

        // Precompute perceptual luminance for all pixels to process edges in luminance domain
        val luma = IntArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            luma[i] = (299 * r + 587 * g + 114 * b) / 1000
        }

        for (y in 0 until height) {
            val rowOffset = y * width
            val topRow = (if (y >= samplingStep) y - samplingStep else 0) * width
            val bottomRow = (if (y < height - samplingStep) y + samplingStep else height - 1) * width

            for (x in 0 until width) {
                val left = if (x >= samplingStep) x - samplingStep else 0
                val right = if (x < width - samplingStep) x + samplingStep else width - 1

                val idx = rowOffset + x
                val centerLuma = luma[idx]

                val neighborAvg = (luma[topRow + x] + luma[bottomRow + x] + luma[rowOffset + left] + luma[rowOffset + right]) / 4
                val diff = centerLuma - neighborAvg

                if (diff == 0) {
                    outputPixels[idx] = pixels[idx]
                } else {
                    val boost = (diff * strength).coerceIn(-100f, 100f)
                    val origColor = pixels[idx]
                    val a = (origColor shr 24) and 0xFF
                    val r = (((origColor shr 16) and 0xFF) + boost).roundToInt().coerceIn(0, 255)
                    val g = (((origColor shr 8) and 0xFF) + boost).roundToInt().coerceIn(0, 255)
                    val b = ((origColor and 0xFF) + boost).roundToInt().coerceIn(0, 255)

                    outputPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
