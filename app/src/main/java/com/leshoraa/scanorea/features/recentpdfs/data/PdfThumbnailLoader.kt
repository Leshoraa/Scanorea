package com.leshoraa.scanorea.features.recentpdfs.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Thread-safe two-tier (L1 Memory + L2 Disk) loader and cache for PDF thumbnails.
 *
 * Checks in-memory cache first, then persistent disk cache, falling back to
 * native [PdfRenderer] only when no cached thumbnail exists.
 */
object PdfThumbnailLoader {

    private const val TAG = "PdfThumbnailLoader"

    /** Width used by every list thumbnail; keep the cache key and render size in sync. */
    private const val THUMBNAIL_WIDTH = 140

    private const val MEMORY_CACHE_MAX_BYTES = 8 * 1024 * 1024
    private const val THUMBNAIL_QUALITY = 85
    private const val THUMBNAILS_DIR_NAME = "pdf_thumbnails"

    private val renderSemaphore = Semaphore(2)

    @Volatile
    private var appCacheDir: File? = null

    // L1 in-memory cache bounded by bytes so wide pages cannot silently inflate memory use.
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_MAX_BYTES) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.allocationByteCount
    }

    /**
     * Optional initialization method to configure the base cache directory once.
     */
    fun initCacheDir(cacheDir: File) {
        appCacheDir = cacheDir
    }

    /**
     * Pre-generates the scaled L2 disk cache (.webp) in the background so that
     * when the document list is presented, thumbnail loading is instantaneous.
     */
    suspend fun pregenerateThumbnail(file: File, diskCacheDir: File? = null) {
        loadFirstPageThumbnail(file = file, diskCacheDir = diskCacheDir)
    }

    /**
     * Returns an already-loaded L1 thumbnail without touching disk.
     *
     * Composables use this as their initial state so recycled list items can show
     * their thumbnail immediately instead of flashing a placeholder while a
     * coroutine re-enters the IO path.
     */
    fun getCachedThumbnail(
        file: File,
        lastModifiedMillis: Long,
        targetWidth: Int = THUMBNAIL_WIDTH
    ): Bitmap? = memoryCache.get(memoryCacheKey(file, lastModifiedMillis, targetWidth))

    /**
     * Renders page 0 of the specified [file] into a scaled [Bitmap].
     * Uses L1 memory cache, L2 disk cache, or renders on demand.
     *
     * [lastModifiedMillis] should come from the document metadata when available.
     * Passing it keeps the cache key consistent and avoids another file stat on
     * the caller's thread.
     */
    suspend fun loadFirstPageThumbnail(
        file: File,
        targetWidth: Int = THUMBNAIL_WIDTH,
        diskCacheDir: File? = null,
        lastModifiedMillis: Long? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext null
        }

        val modifiedMillis = lastModifiedMillis ?: file.lastModified()
        val cacheKey = memoryCacheKey(file, modifiedMillis, targetWidth)

        // Memory cache lookup
        memoryCache.get(cacheKey)?.let { cachedBitmap ->
            return@withContext cachedBitmap
        }

        val effectiveCacheDir = diskCacheDir ?: appCacheDir ?: file.parentFile?.parentFile?.let { File(it, "cache") }
        val diskCacheFile = getDiskCacheFile(file, modifiedMillis, targetWidth, effectiveCacheDir)

        // Disk cache lookup
        if (diskCacheFile != null && diskCacheFile.exists() && diskCacheFile.length() > 0L) {
            try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val diskBitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath, decodeOptions)
                if (diskBitmap != null) {
                    memoryCache.put(cacheKey, diskBitmap)
                    return@withContext diskBitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode disk thumbnail for ${file.name}", e)
                diskCacheFile.delete()
            }
        }

        // Fallback: Render Page 0 via Native PdfRenderer with concurrency control
        renderSemaphore.withPermit {
            // Re-check memoryCache inside permit in case a concurrent coroutine just rendered it
            memoryCache.get(cacheKey)?.let { return@withPermit it }

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null

            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                if (renderer.pageCount <= 0) {
                    return@withPermit null
                }

                page = renderer.openPage(0)
                val aspectRatio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                val targetHeight = (targetWidth * aspectRatio).toInt().coerceIn(targetWidth, targetWidth * 2)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Cache in memory
                memoryCache.put(cacheKey, bitmap)

                // Persist to L2 Disk Cache asynchronously
                if (diskCacheFile != null) {
                    saveToDiskCache(bitmap, diskCacheFile)
                }

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render thumbnail for ${file.name}", e)
                null
            } finally {
                try {
                    page?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing PdfRenderer.Page", e)
                }
                try {
                    renderer?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing PdfRenderer", e)
                }
                try {
                    pfd?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing ParcelFileDescriptor", e)
                }
            }
        }
    }

    private fun memoryCacheKey(
        file: File,
        lastModifiedMillis: Long,
        targetWidth: Int
    ): String = "${file.absolutePath}_${lastModifiedMillis}_$targetWidth"

    private fun getDiskCacheFile(
        file: File,
        lastModifiedMillis: Long,
        targetWidth: Int,
        baseDir: File?
    ): File? {
        if (baseDir == null) return null
        return try {
            val thumbDir = File(baseDir, THUMBNAILS_DIR_NAME)
            if (!thumbDir.exists()) {
                thumbDir.mkdirs()
            }
            val safeName = "${file.nameWithoutExtension}_${lastModifiedMillis}_${targetWidth}.webp"
            File(thumbDir, safeName)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToDiskCache(bitmap: Bitmap, targetFile: File) {
        try {
            FileOutputStream(targetFile).use { fos ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, THUMBNAIL_QUALITY, fos)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, fos)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write disk cache for ${targetFile.name}", e)
        }
    }

    /**
     * Clears cached thumbnails (both memory and disk) if files are modified or deleted.
     */
    fun evict(file: File, diskCacheDir: File? = null) {
        val prefix = file.absolutePath
        val snapshot = memoryCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(prefix)) {
                memoryCache.remove(key)
            }
        }

        val effectiveCacheDir = diskCacheDir ?: appCacheDir ?: file.parentFile?.parentFile?.let { File(it, "cache") }
        if (effectiveCacheDir != null) {
            try {
                val thumbDir = File(effectiveCacheDir, THUMBNAILS_DIR_NAME)
                if (thumbDir.exists() && thumbDir.isDirectory) {
                    val matchingFiles = thumbDir.listFiles { dir, name ->
                        name.startsWith("${file.nameWithoutExtension}_")
                    }
                    matchingFiles?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error evicting disk thumbnails for ${file.name}", e)
            }
        }
    }
}
