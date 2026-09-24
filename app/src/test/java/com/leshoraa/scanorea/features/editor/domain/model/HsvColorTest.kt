package com.leshoraa.scanorea.features.editor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HsvColorTest {

    @Test
    fun fromColorLong_pureRed_returnsExpectedHsvComponents() {
        val redColorLong = 0xFFFF0000L
        val hsv = HsvColor.fromColorLong(redColorLong)

        assertEquals(0f, hsv.hue, 0.5f)
        assertEquals(1f, hsv.saturation, 0.01f)
        assertEquals(1f, hsv.value, 0.01f)
        assertEquals("#FF0000", hsv.toHexString())
    }

    @Test
    fun fromColorLong_pureGreen_returnsExpectedHsvComponents() {
        val greenColorLong = 0xFF00FF00L
        val hsv = HsvColor.fromColorLong(greenColorLong)

        assertEquals(120f, hsv.hue, 0.5f)
        assertEquals(1f, hsv.saturation, 0.01f)
        assertEquals(1f, hsv.value, 0.01f)
        assertEquals("#00FF00", hsv.toHexString())
    }

    @Test
    fun fromColorLong_pureBlue_returnsExpectedHsvComponents() {
        val blueColorLong = 0xFF0000FFL
        val hsv = HsvColor.fromColorLong(blueColorLong)

        assertEquals(240f, hsv.hue, 0.5f)
        assertEquals(1f, hsv.saturation, 0.01f)
        assertEquals(1f, hsv.value, 0.01f)
        assertEquals("#0000FF", hsv.toHexString())
    }

    @Test
    fun fromColorLong_pureWhite_returnsZeroSaturationAndFullValue() {
        val whiteColorLong = 0xFFFFFFFFL
        val hsv = HsvColor.fromColorLong(whiteColorLong)

        assertEquals(0f, hsv.saturation, 0.01f)
        assertEquals(1f, hsv.value, 0.01f)
        assertEquals("#FFFFFF", hsv.toHexString())
        assertEquals(whiteColorLong, hsv.toColorLong())
    }

    @Test
    fun fromColorLong_pureBlack_returnsZeroSaturationAndZeroValue() {
        val blackColorLong = 0xFF000000L
        val hsv = HsvColor.fromColorLong(blackColorLong)

        assertEquals(0f, hsv.saturation, 0.01f)
        assertEquals(0f, hsv.value, 0.01f)
        assertEquals("#000000", hsv.toHexString())
        assertEquals(blackColorLong, hsv.toColorLong())
    }

    @Test
    fun toColorLong_roundTrip_matchesPresetsAccurately() {
        AnnotationColors.PRESETS.forEach { presetColorLong ->
            val hsv = HsvColor.fromColorLong(presetColorLong)
            val reconstructedColorLong = hsv.toColorLong()

            val expectedRed = ((presetColorLong shr 16) and 0xFF).toInt()
            val expectedGreen = ((presetColorLong shr 8) and 0xFF).toInt()
            val expectedBlue = (presetColorLong and 0xFF).toInt()

            val actualRed = ((reconstructedColorLong shr 16) and 0xFF).toInt()
            val actualGreen = ((reconstructedColorLong shr 8) and 0xFF).toInt()
            val actualBlue = (reconstructedColorLong and 0xFF).toInt()

            assertTrue(
                "Color mismatch for preset $presetColorLong: expected ($expectedRed, $expectedGreen, $expectedBlue) vs ($actualRed, $actualGreen, $actualBlue)",
                kotlin.math.abs(expectedRed - actualRed) <= 2 &&
                        kotlin.math.abs(expectedGreen - actualGreen) <= 2 &&
                        kotlin.math.abs(expectedBlue - actualBlue) <= 2
            )
        }
    }

    @Test
    fun fromColorLong_clampsAlphaWithinAllowedRange() {
        val tooLowAlpha = HsvColor.fromColorLong(0xFFFF0000L, alpha = 0.01f)
        assertEquals(HsvColor.MIN_ALPHA, tooLowAlpha.alpha, 0.001f)

        val tooHighAlpha = HsvColor.fromColorLong(0xFFFF0000L, alpha = 1.5f)
        assertEquals(HsvColor.MAX_ALPHA, tooHighAlpha.alpha, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_outOfBoundsHue_throwsIllegalArgumentException() {
        HsvColor(hue = 400f, saturation = 0.5f, value = 0.5f)
    }
}
