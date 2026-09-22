package com.leshoraa.scanorea.features.presets.domain.model

import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests validating [ConversionPreset] initialization and defaults.
 */
class ConversionPresetTest {

    @Test
    fun conversionPreset_defaultValues_areCorrect() {
        val preset = ConversionPreset(
            id = "test_preset",
            name = "Test Preset",
            fileNameTemplate = "Test_{DD:MM:YYYY}"
        )

        assertEquals("test_preset", preset.id)
        assertEquals("Test Preset", preset.name)
        assertEquals("Test_{DD:MM:YYYY}", preset.fileNameTemplate)
        assertEquals(PdfPageSize.A4, preset.pageSize)
        assertEquals(PdfPageOrientation.PORTRAIT, preset.orientation)
        assertEquals(CompressionProfile.AUTO_BALANCED, preset.compressionProfile)
    }
}
