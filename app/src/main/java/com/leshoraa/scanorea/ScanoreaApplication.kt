package com.leshoraa.scanorea

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.leshoraa.scanorea.di.AppContainer
import com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Custom application class configuring global, performance-tuned Coil image caching
 * to drastically reduce memory pressure and eliminate UI stutters.
 */
class ScanoreaApplication : Application(), ImageLoaderFactory {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        container.settingsRepository // Eagerly loads settings and custom tags
        PdfThumbnailLoader.initCacheDir(cacheDir)
        cleanStaleCacheFiles()
    }

    private fun cleanStaleCacheFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val maxAgeMillis = 24 * 60 * 60 * 1000L // 24 hours
                val cutoffTime = System.currentTimeMillis() - maxAgeMillis

                // Clean stale temporary camera scans and opened external PDFs in cache/pdfs
                val pdfCacheDir = File(cacheDir, "pdfs")
                if (pdfCacheDir.exists() && pdfCacheDir.isDirectory) {
                    pdfCacheDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.lastModified() < cutoffTime) {
                            file.delete()
                        }
                    }
                }

                // Also clean orphaned scan/opened temp files in the root cache directory
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile &&
                        (file.name.startsWith("scan_") || file.name.startsWith("opened_") || file.name.endsWith(".tmp")) &&
                        file.lastModified() < cutoffTime
                    ) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // Non-critical background maintenance; suppress
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
