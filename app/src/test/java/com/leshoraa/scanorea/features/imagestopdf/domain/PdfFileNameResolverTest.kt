package com.leshoraa.scanorea.features.imagestopdf.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PdfFileNameResolver], the guard that prevents PDF files from being
 * silently overwritten when the user reuses the same document name.
 */
class PdfFileNameResolverTest {

    private val timestamp = 1_700_000_000_000L

    @Test
    fun sanitize_whenNameHasNoExtension_appendsPdfExtension() {
        assertEquals("Invoice_2026.pdf", PdfFileNameResolver.sanitize("Invoice_2026", timestamp))
    }

    @Test
    fun sanitize_whenNameAlreadyHasExtension_preservesSingleExtension() {
        assertEquals("report.pdf", PdfFileNameResolver.sanitize("report.pdf", timestamp))
        assertEquals("report.pdf", PdfFileNameResolver.sanitize("report.PDF", timestamp))
    }

    @Test
    fun sanitize_whenNameContainsInvalidCharacters_replacesWithUnderscore() {
        assertEquals(
            "my_illegal_name__test.pdf",
            PdfFileNameResolver.sanitize("my/illegal:name?*test", timestamp)
        )
    }

    @Test
    fun sanitize_whenNameIsBlank_fallsBackToTimestampName() {
        val result = PdfFileNameResolver.sanitize("   ", timestamp)

        assertEquals("Scanorea_$timestamp.pdf", result)
    }

    @Test
    fun sanitize_whenNameIsOnlyDots_fallsBackToTimestampName() {
        assertEquals("Scanorea_$timestamp.pdf", PdfFileNameResolver.sanitize("...", timestamp))
        assertEquals("Scanorea_$timestamp.pdf", PdfFileNameResolver.sanitize(".pdf", timestamp))
    }

    @Test
    fun sanitize_whenNameIsTooLong_truncatesBaseName() {
        val result = PdfFileNameResolver.sanitize("a".repeat(500), timestamp)

        assertEquals(
            "a".repeat(PdfFileNameResolver.MAX_BASE_NAME_LENGTH) + ".pdf",
            result
        )
        assertTrue(result.length <= PdfFileNameResolver.MAX_BASE_NAME_LENGTH + 4)
    }

    @Test
    fun resolveUnique_whenNoCollision_returnsSanitizedName() {
        val result = PdfFileNameResolver.resolveUnique(
            desiredName = "Invoice",
            existingNames = listOf("Receipt.pdf", "Summary.pdf"),
            fallbackTimestampMillis = timestamp
        )

        assertEquals("Invoice.pdf", result)
    }

    @Test
    fun resolveUnique_whenNameAlreadyExists_appendsCounterSuffix() {
        val result = PdfFileNameResolver.resolveUnique(
            desiredName = "Invoice",
            existingNames = listOf("Invoice.pdf"),
            fallbackTimestampMillis = timestamp
        )

        assertEquals("Invoice (1).pdf", result)
    }

    @Test
    fun resolveUnique_whenSuffixedNamesAlsoExist_incrementsUntilFree() {
        val result = PdfFileNameResolver.resolveUnique(
            desiredName = "Invoice.pdf",
            existingNames = listOf("Invoice.pdf", "Invoice (1).pdf", "Invoice (2).pdf"),
            fallbackTimestampMillis = timestamp
        )

        assertEquals("Invoice (3).pdf", result)
    }

    @Test
    fun resolveUnique_comparisonIsCaseInsensitive() {
        val result = PdfFileNameResolver.resolveUnique(
            desiredName = "invoice",
            existingNames = listOf("INVOICE.PDF"),
            fallbackTimestampMillis = timestamp
        )

        assertEquals("invoice (1).pdf", result)
    }

    @Test
    fun resolveUnique_twoConsecutiveConversions_produceDifferentNames() {
        val first = PdfFileNameResolver.resolveUnique(
            desiredName = "Business Plan",
            existingNames = emptyList(),
            fallbackTimestampMillis = timestamp
        )
        val second = PdfFileNameResolver.resolveUnique(
            desiredName = "Business Plan",
            existingNames = listOf(first),
            fallbackTimestampMillis = timestamp
        )

        assertNotEquals(first, second)
        assertEquals("Business Plan.pdf", first)
        assertEquals("Business Plan (1).pdf", second)
    }

    @Test
    fun resolveUnique_blankNamesProduceDistinctFilesForDifferentTimestamps() {
        val first = PdfFileNameResolver.resolveUnique(
            desiredName = "",
            existingNames = emptyList(),
            fallbackTimestampMillis = timestamp
        )
        val second = PdfFileNameResolver.resolveUnique(
            desiredName = "",
            existingNames = listOf(first),
            fallbackTimestampMillis = timestamp + 1
        )

        assertNotEquals(first, second)
        assertTrue(second.endsWith(".pdf"))
    }
}
