package com.leshoraa.scanorea.features.pdfviewer.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Native platform-based PDF rendering engine utilizing [android.graphics.pdf.PdfRenderer].
 * Renders individual pages into high-fidelity bitmaps for on-screen preview.
 *
 * Thread safety: All calls into [pdfRenderer] are protected by a [Mutex] so that
 * concurrent requests from UI pager/scroll listeners never violate the single-open-page
 * invariant of [android.graphics.pdf.PdfRenderer].
 */
class PdfRendererDataSource(val file: File) : Closeable {

    private val parcelFileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IOException("Unable to open ParcelFileDescriptor for PDF: ${file.absolutePath}")

    private val pdfRenderer: PdfRenderer = PdfRenderer(parcelFileDescriptor)

    private val mutex = Mutex()
    private val cachedAspectRatios = ConcurrentHashMap<Int, Float>()

    @Volatile
    private var isClosed = false

    val pageCount: Int = pdfRenderer.pageCount

    /**
     * Returns the aspect ratio (width / height) of the given [pageIndex].
     * Defaults to A4 ratio (1 / 1.4142) if out of range or unreadable.
     */
    suspend fun getPageAspectRatio(pageIndex: Int): Float = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext 1f / 1.4142f
        cachedAspectRatios[pageIndex]?.let { return@withContext it }

        mutex.withLock {
            if (isClosed) return@withContext 1f / 1.4142f
            cachedAspectRatios[pageIndex]?.let { return@withContext it }
            try {
                val page = pdfRenderer.openPage(pageIndex)
                try {
                    val ratio = page.width.toFloat() / page.height.toFloat()
                    cachedAspectRatios[pageIndex] = ratio
                    ratio
                } finally {
                    page.close()
                }
            } catch (_: Exception) {
                1f / 1.4142f
            }
        }
    }

    /**
     * Renders a specific 0-based [pageIndex] into a [Bitmap] scaled to [targetWidth].
     * Retries once on transient render failure to prevent persistent loading spinners.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) {
            throw IndexOutOfBoundsException("Page index $pageIndex is out of bounds (0..${pageCount - 1})")
        }

        mutex.withLock {
            if (isClosed) throw IllegalStateException("PdfRendererDataSource is already closed")
            renderPageInternal(pageIndex, targetWidth)
        }
    }

    private fun renderPageInternal(pageIndex: Int, targetWidth: Int): Bitmap {
        var lastError: Exception? = null
        for (attempt in 0..1) {
            try {
                val page = pdfRenderer.openPage(pageIndex)
                try {
                    val aspectRatio = page.height.toFloat() / page.width.toFloat()
                    val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cachedAspectRatios[pageIndex] = page.width.toFloat() / page.height.toFloat()
                    return bitmap
                } finally {
                    page.close()
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0) {
                    Thread.sleep(20)
                }
            }
        }
        throw lastError ?: IOException("Failed to render page $pageIndex after retry")
    }

    override fun close() {
        runBlocking {
            mutex.withLock {
                if (isClosed) return@withLock
                isClosed = true
                try {
                    pdfRenderer.close()
                } finally {
                    parcelFileDescriptor.close()
                }
            }
        }
    }
}
