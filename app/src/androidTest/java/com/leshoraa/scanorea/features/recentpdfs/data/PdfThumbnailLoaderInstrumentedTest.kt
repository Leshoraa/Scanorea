package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Instrumented regression tests for [PdfThumbnailLoader].
 *
 * Verifies that Native PdfRenderer correctly renders page 0 into an ARGB_8888 Bitmap
 * on an actual Android device/emulator without throwing "Unsupported pixel format".
 */
@RunWith(AndroidJUnit4::class)
class PdfThumbnailLoaderInstrumentedTest {

    private lateinit var context: Context
    private lateinit var testPdfFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testPdfFile = File(context.cacheDir, "test_thumbnail_sample.pdf")

        // Create a minimal valid 1-page PDF
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 400, 1).create()
        val page = doc.startPage(pageInfo)
        val paint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
        }
        page.canvas.drawRect(20f, 20f, 280f, 380f, paint)
        doc.finishPage(page)

        FileOutputStream(testPdfFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()

        PdfThumbnailLoader.initCacheDir(context.cacheDir)
    }

    @After
    fun tearDown() {
        PdfThumbnailLoader.evict(testPdfFile, context.cacheDir)
        if (testPdfFile.exists()) {
            testPdfFile.delete()
        }
    }

    @Test
    fun loadFirstPageThumbnail_rendersValidArgb8888Bitmap() = runBlocking {
        val bitmap = PdfThumbnailLoader.loadFirstPageThumbnail(
            file = testPdfFile,
            targetWidth = 140,
            diskCacheDir = context.cacheDir
        )

        assertNotNull("Thumbnail bitmap should not be null", bitmap)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap!!.config)
        assertEquals(140, bitmap.width)
        assertTrue("Height should be positive", bitmap.height > 0)
    }

    @Test
    fun getCachedThumbnail_returnsMemoryCachedBitmapAfterLoad() = runBlocking {
        val loadedBitmap = PdfThumbnailLoader.loadFirstPageThumbnail(
            file = testPdfFile,
            targetWidth = 140,
            diskCacheDir = context.cacheDir
        )
        assertNotNull(loadedBitmap)

        val cached = PdfThumbnailLoader.getCachedThumbnail(
            file = testPdfFile,
            lastModifiedMillis = testPdfFile.lastModified(),
            targetWidth = 140
        )
        assertNotNull("L1 memory cache hit should succeed", cached)
        assertEquals(loadedBitmap, cached)
    }
}
