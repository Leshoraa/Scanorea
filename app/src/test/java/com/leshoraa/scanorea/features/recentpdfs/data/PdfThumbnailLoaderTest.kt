package com.leshoraa.scanorea.features.recentpdfs.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

/**
 * Unit tests for [PdfThumbnailLoader.memoryCacheKey], verifying that the memory cache key
 * is consistent between pregeneration and on-demand thumbnail rendering.
 *
 * The cache key must be identical when [PdfThumbnailLoader.pregenerateThumbnail] and
 * [PdfThumbnailLoader.getCachedThumbnail] / [PdfThumbnailLoader.loadFirstPageThumbnail]
 * use the same [lastModifiedMillis], otherwise freshly converted PDFs show no thumbnail
 * in the Results screen.
 */
class PdfThumbnailLoaderTest {

    private val testFile = File("/data/user/0/com.leshoraa.scanorea/files/pdfs/Document.pdf")
    private val timestamp = 1_700_000_000_000L
    private val targetWidth = 140

    @Test
    fun memoryCacheKey_sameFileSameTimestampSameWidth_returnsSameKey() {
        val key1 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)
        val key2 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)

        assertEquals(key1, key2)
    }

    @Test
    fun memoryCacheKey_differentTimestamp_returnsDifferentKey() {
        val key1 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)
        val key2 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp + 1, targetWidth)

        assertNotEquals(key1, key2)
    }

    @Test
    fun memoryCacheKey_differentWidth_returnsDifferentKey() {
        val key1 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)
        val key2 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth * 2)

        assertNotEquals(key1, key2)
    }

    @Test
    fun memoryCacheKey_differentFile_returnsDifferentKey() {
        val otherFile = File("/data/user/0/com.leshoraa.scanorea/files/pdfs/Other.pdf")
        val key1 = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)
        val key2 = PdfThumbnailLoader.memoryCacheKey(otherFile, timestamp, targetWidth)

        assertNotEquals(key1, key2)
    }

    @Test
    fun memoryCacheKey_keyContainsAllComponents() {
        val key = PdfThumbnailLoader.memoryCacheKey(testFile, timestamp, targetWidth)

        assertEquals("${testFile.absolutePath}_${timestamp}_${targetWidth}", key)
    }
}
