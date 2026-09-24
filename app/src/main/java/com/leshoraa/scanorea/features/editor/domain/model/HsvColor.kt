package com.leshoraa.scanorea.features.editor.domain.model

import kotlin.math.roundToInt

/**
 * Value object representing a color in HSV (Hue, Saturation, Value) color space
 * with an optional alpha channel.
 *
 * Encapsulates color coordinate conversions between HSV, 32-bit ARGB Long,
 * and uppercase hexadecimal color representations.
 */
data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
    val alpha: Float = MAX_ALPHA
) {
    init {
        require(hue in MIN_HUE..MAX_HUE) { "Hue must be in range $MIN_HUE..$MAX_HUE, but was $hue" }
        require(saturation in MIN_FRACTION..MAX_FRACTION) { "Saturation must be in range $MIN_FRACTION..$MAX_FRACTION, but was $saturation" }
        require(value in MIN_FRACTION..MAX_FRACTION) { "Value must be in range $MIN_FRACTION..$MAX_FRACTION, but was $value" }
        require(alpha in MIN_ALPHA..MAX_ALPHA) { "Alpha must be in range $MIN_ALPHA..$MAX_ALPHA, but was $alpha" }
    }

    /**
     * Converts HSV components to a 32-bit ARGB color integer formatted as a Long.
     * The alpha channel of the returned color integer is fully opaque (0xFF).
     */
    fun toColorLong(): Long {
        val rgb = hsvToRgb(hue, saturation, value)
        return (0xFFL shl 24) or (rgb.red.toLong() shl 16) or (rgb.green.toLong() shl 8) or rgb.blue.toLong()
    }

    /**
     * Formats the color as an uppercase hexadecimal string (#RRGGBB).
     */
    fun toHexString(): String {
        val rgb = hsvToRgb(hue, saturation, value)
        return String.format("#%02X%02X%02X", rgb.red, rgb.green, rgb.blue)
    }

    data class RgbComponents(val red: Int, val green: Int, val blue: Int)

    companion object {
        const val MIN_HUE = 0f
        const val MAX_HUE = 360f
        const val MIN_FRACTION = 0f
        const val MAX_FRACTION = 1f
        const val MIN_ALPHA = 0.05f
        const val MAX_ALPHA = 1.0f

        /**
         * Pure Kotlin implementation of HSV to RGB conversion.
         * Runs deterministically across any platform runtime without platform graphics dependencies.
         */
        fun hsvToRgb(hue: Float, saturation: Float, value: Float): RgbComponents {
            val clampedSaturation = saturation.coerceIn(MIN_FRACTION, MAX_FRACTION)
            val clampedValue = value.coerceIn(MIN_FRACTION, MAX_FRACTION)

            if (clampedSaturation <= 0f) {
                val grayComponent = (clampedValue * 255f).roundToInt().coerceIn(0, 255)
                return RgbComponents(grayComponent, grayComponent, grayComponent)
            }

            val normalizedHue = (hue % MAX_HUE + MAX_HUE) % MAX_HUE
            val sectorIndex = (normalizedHue / 60f).toInt()
            val sectorFraction = (normalizedHue / 60f) - sectorIndex

            val p = (clampedValue * (1f - clampedSaturation) * 255f).roundToInt().coerceIn(0, 255)
            val q = (clampedValue * (1f - clampedSaturation * sectorFraction) * 255f).roundToInt().coerceIn(0, 255)
            val t = (clampedValue * (1f - clampedSaturation * (1f - sectorFraction)) * 255f).roundToInt().coerceIn(0, 255)
            val v = (clampedValue * 255f).roundToInt().coerceIn(0, 255)

            return when (sectorIndex) {
                0 -> RgbComponents(v, t, p)
                1 -> RgbComponents(q, v, p)
                2 -> RgbComponents(p, v, t)
                3 -> RgbComponents(p, q, v)
                4 -> RgbComponents(t, p, v)
                else -> RgbComponents(v, p, q)
            }
        }

        /**
         * Converts a 32-bit ARGB color Long into an [HsvColor] instance.
         */
        fun fromColorLong(colorLong: Long, alpha: Float = MAX_ALPHA): HsvColor {
            val red = ((colorLong shr 16) and 0xFF).toInt()
            val green = ((colorLong shr 8) and 0xFF).toInt()
            val blue = (colorLong and 0xFF).toInt()

            val normalizedRed = red / 255f
            val normalizedGreen = green / 255f
            val normalizedBlue = blue / 255f

            val maxComponent = maxOf(normalizedRed, normalizedGreen, normalizedBlue)
            val minComponent = minOf(normalizedRed, normalizedGreen, normalizedBlue)
            val delta = maxComponent - minComponent

            val value = maxComponent
            val saturation = if (maxComponent > 0f) delta / maxComponent else 0f

            val rawHue = if (delta == 0f) {
                0f
            } else {
                val computedHue = when (maxComponent) {
                    normalizedRed -> 60f * (((normalizedGreen - normalizedBlue) / delta) % 6f)
                    normalizedGreen -> 60f * (((normalizedBlue - normalizedRed) / delta) + 2f)
                    else -> 60f * (((normalizedRed - normalizedGreen) / delta) + 4f)
                }
                if (computedHue < 0f) computedHue + MAX_HUE else computedHue
            }

            return HsvColor(
                hue = rawHue.coerceIn(MIN_HUE, MAX_HUE),
                saturation = saturation.coerceIn(MIN_FRACTION, MAX_FRACTION),
                value = value.coerceIn(MIN_FRACTION, MAX_FRACTION),
                alpha = alpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
            )
        }
    }
}
