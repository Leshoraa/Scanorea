package com.leshoraa.scanorea.features.pdfviewer.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * Native platform-based PDF rendering engine utilizing [android.graphics.pdf.PdfRenderer].
 * Renders individual pages into high-fidelity bitmaps for on-screen preview.
 */
class PdfRendererDataSource(val file: File) : Closeable {

    private val parcelFileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IOException("Unable to open ParcelFileDescriptor for PDF: ${file.absolutePath}")

    private val pdfRenderer: PdfRenderer = PdfRenderer(parcelFileDescriptor)

    val pageCount: Int
        get() = pdfRenderer.pageCount

    /**
     * Returns the aspect ratio (width / height) of the given [pageIndex].
     * Defaults to A4 ratio (1 / 1.4142) if out of range or unreadable.
     */
    fun getPageAspectRatio(pageIndex: Int): Float {
        if (pageIndex < 0 || pageIndex >= pageCount) return 1f / 1.4142f
        return try {
            val page = pdfRenderer.openPage(pageIndex)
            val ratio = page.width.toFloat() / page.height.toFloat()
            page.close()
            ratio
        } catch (_: Exception) {
            1f / 1.4142f
        }
    }

    /**
     * Renders a specific 0-based [pageIndex] into a [Bitmap] scaled to [targetWidth].
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) {
            throw IndexOutOfBoundsException("Page index $pageIndex is out of bounds (0..${pageCount - 1})")
        }

        val page = pdfRenderer.openPage(pageIndex)
        try {
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // PDFs default to transparent background, fill with solid white first for realistic paper rendering
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } finally {
            page.close()
        }
    }

    override fun close() {
        try {
            pdfRenderer.close()
        } finally {
            parcelFileDescriptor.close()
        }
    }
}
