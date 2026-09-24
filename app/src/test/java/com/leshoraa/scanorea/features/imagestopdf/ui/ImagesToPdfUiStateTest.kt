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

    @Test
    fun defaultFilter_defaultsToBlackAndWhite() {
        val state = ImagesToPdfUiState()
        assertEquals(com.leshoraa.scanorea.core.filter.ImageFilterType.BLACK_AND_WHITE, state.defaultFilter)
    }

    @Test
    fun defaultFilter_canBeCustomized() {
        val state = ImagesToPdfUiState(defaultFilter = com.leshoraa.scanorea.core.filter.ImageFilterType.ORIGINAL)
        assertEquals(com.leshoraa.scanorea.core.filter.ImageFilterType.ORIGINAL, state.defaultFilter)
    }

    @Test
    fun canUndoAnnotation_reflectsAnnotationState() {
        val dummyUri = android.net.TestUri("content://media/1")
        val annotation = com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.FreehandPath(
            color = 0xFFFF0000,
            alpha = 1.0f,
            points = emptyList()
        )
        val pageWithAnno = ImagePage(id = "p1", uri = dummyUri, annotations = listOf(annotation))
        val pageWithoutAnno = ImagePage(id = "p2", uri = dummyUri, annotations = emptyList())

        val state = ImagesToPdfUiState(pages = listOf(pageWithAnno, pageWithoutAnno))

        assertTrue(state.canUndoAnnotation("p1"))
        assertFalse(state.canUndoAnnotation("p2"))
        assertFalse(state.canUndoAnnotation("p_unknown"))
    }

    @Test
    fun canRedoAnnotation_reflectsRedoMapState() {
        val annotation = com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.FreehandPath(
            color = 0xFFFF0000,
            alpha = 0.5f,
            points = emptyList()
        )
        val state = ImagesToPdfUiState(
            redoAnnotationsMap = mapOf(
                "p1" to listOf(annotation),
                "p2" to emptyList()
            )
        )

        assertTrue(state.canRedoAnnotation("p1"))
        assertFalse(state.canRedoAnnotation("p2"))
        assertFalse(state.canRedoAnnotation("p_unknown"))
    }
}
