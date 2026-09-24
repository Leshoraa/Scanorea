package com.leshoraa.scanorea.features.recentpdfs.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying [RecentPdf] domain model properties, defaults, and copy transformations.
 */
class RecentPdfTest {

    @Test
    fun recentPdf_defaults_haveExpectedValues() {
        val dummyFile = File("/mock/path/doc.pdf")
        val pdf = RecentPdf(
            file = dummyFile,
            name = "doc.pdf",
            sizeBytes = 1024L,
            lastModifiedMillis = 5000L
        )

        assertEquals("doc.pdf", pdf.name)
        assertEquals(1024L, pdf.sizeBytes)
        assertEquals(5000L, pdf.lastModifiedMillis)
        assertNull(pdf.category)
        assertFalse(pdf.isFavorite)
        assertTrue(pdf.tags.isEmpty())
    }

    @Test
    fun recentPdf_withCategoryAndFavorite_holdsSuppliedValues() {
        val dummyFile = File("/mock/path/receipt.pdf")
        val pdf = RecentPdf(
            file = dummyFile,
            name = "receipt.pdf",
            sizeBytes = 2048L,
            lastModifiedMillis = 6000L,
            category = "Receipts",
            isFavorite = true,
            tags = listOf("finance", "2026")
        )

        assertEquals("Receipts", pdf.category)
        assertTrue(pdf.isFavorite)
        assertEquals(listOf("finance", "2026"), pdf.tags)
    }

    @Test
    fun recentPdf_copy_togglesFavoriteCorrectly() {
        val dummyFile = File("/mock/path/study_notes.pdf")
        val original = RecentPdf(
            file = dummyFile,
            name = "study_notes.pdf",
            sizeBytes = 4096L,
            lastModifiedMillis = 7000L,
            category = "Study",
            isFavorite = false
        )

        val favorited = original.copy(isFavorite = true)
        assertTrue(favorited.isFavorite)
        assertEquals("Study", favorited.category)

        val unfavorited = favorited.copy(isFavorite = false)
        assertFalse(unfavorited.isFavorite)
    }

    @Test
    fun recentPdf_copy_updatesCategoryCorrectly() {
        val dummyFile = File("/mock/path/work_doc.pdf")
        val original = RecentPdf(
            file = dummyFile,
            name = "work_doc.pdf",
            sizeBytes = 8192L,
            lastModifiedMillis = 8000L,
            category = null
        )

        val assigned = original.copy(category = "Work")
        assertEquals("Work", assigned.category)

        val unassigned = assigned.copy(category = null)
        assertNull(unassigned.category)
    }
}
