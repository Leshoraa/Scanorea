package com.leshoraa.scanorea.features.imagestopdf.ui

import android.net.Uri
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [ImagesToPdfUiState] calculated properties.
 */
class ImagesToPdfUiStateTest {

    @Test
    fun hasPages_whenPagesListIsEmpty_returnsFalse() {
        val state = ImagesToPdfUiState(pages = emptyList())
        assertFalse(state.hasPages)
        assertEquals(0, state.pageCount)
    }

    @Test
    fun hasPages_whenPagesListHasItems_returnsTrueAndCorrectCount() {
        val dummyUri = android.net.TestUri("content://media/1")
        val pages = listOf(
            ImagePage(id = "1", uri = dummyUri),
            ImagePage(id = "2", uri = dummyUri)
        )
        val state = ImagesToPdfUiState(pages = pages)

        assertTrue(state.hasPages)
        assertEquals(2, state.pageCount)
    }
}
