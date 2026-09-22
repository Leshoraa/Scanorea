package com.leshoraa.scanorea.features.imagestopdf.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating [PdfConversionOptions] business logic and file name sanitization.
 */
class PdfConversionOptionsTest {

    @Test
    fun sanitizedFileName_whenCustomNameProvidedWithoutExtension_appendsPdfExtension() {
        val options = PdfConversionOptions(fileName = "Invoice_2026")
        assertEquals("Invoice_2026.pdf", options.sanitizedFileName())
    }

    @Test
    fun sanitizedFileName_whenCustomNameWithExtension_preservesPdfExtension() {
        val options = PdfConversionOptions(fileName = "report.pdf")
        assertEquals("report.pdf", options.sanitizedFileName())
    }

    @Test
    fun sanitizedFileName_whenNameContainsInvalidCharacters_replacesWithUnderscore() {
        val options = PdfConversionOptions(fileName = "my/illegal:name?*test")
        assertEquals("my_illegal_name__test.pdf", options.sanitizedFileName())
    }

    @Test
    fun sanitizedFileName_whenBlankNameProvided_generatesDefaultNameWithScanoreaPrefix() {
        val options = PdfConversionOptions(fileName = "   ")
        val result = options.sanitizedFileName()

        assertTrue(result.startsWith("Scanorea_"))
        assertTrue(result.endsWith(".pdf"))
    }
}
