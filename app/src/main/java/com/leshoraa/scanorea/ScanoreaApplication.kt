package com.leshoraa.scanorea

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.leshoraa.scanorea.di.AppContainer
import com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader

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
        PdfThumbnailLoader.initCacheDir(cacheDir)
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
