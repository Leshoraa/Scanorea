package com.leshoraa.scanorea.features.recentpdfs.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thread-safe loader and in-memory cache for rendering first-page PDF thumbnails.
 *
 * Implements strict resource management ensuring [ParcelFileDescriptor] and [PdfRenderer]
 * are properly finalized after rendering.
 */
object PdfThumbnailLoader {

    // Maximum 40 cached thumbnail bitmaps (roughly ~4-6 MB in memory)
    private val memoryCache = object : LruCache<String, Bitmap>(40) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = 1
    }

    /**
     * Renders page 0 of the specified [file] into a scaled [Bitmap].
     * Returns null if file is missing, empty, password-protected, or unreadable.
     */
    suspend fun loadFirstPageThumbnail(file: File, targetWidth: Int = 140): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext null
        }

        val cacheKey = "${file.absolutePath}_${file.lastModified()}_$targetWidth"
        memoryCache.get(cacheKey)?.let { cachedBitmap ->
            return@withContext cachedBitmap
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (renderer.pageCount <= 0) {
                return@withContext null
            }

            page = renderer.openPage(0)
            val aspectRatio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceIn(targetWidth, targetWidth * 2)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Draw clean white background for transparency in PDF pages
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            memoryCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("PdfThumbnailLoader", "Failed to render thumbnail for ${file.name}", e)
            null
        } finally {
            try {
                page?.close()
            } catch (e: Exception) {
                android.util.Log.w("PdfThumbnailLoader", "Error closing PdfRenderer.Page", e)
            }
            try {
                renderer?.close()
            } catch (e: Exception) {
                android.util.Log.w("PdfThumbnailLoader", "Error closing PdfRenderer", e)
            }
            try {
                pfd?.close()
            } catch (e: Exception) {
                android.util.Log.w("PdfThumbnailLoader", "Error closing ParcelFileDescriptor", e)
            }
        }
    }

    /**
     * Clears cached thumbnails if files are modified or deleted.
     */
    fun evict(file: File) {
        val prefix = file.absolutePath
        val snapshot = memoryCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(prefix)) {
                memoryCache.remove(key)
            }
        }
    }
}
