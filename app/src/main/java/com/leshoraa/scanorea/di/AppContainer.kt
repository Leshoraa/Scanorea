package com.leshoraa.scanorea.di

import android.content.Context
import com.leshoraa.scanorea.ScanoreaApplication
import com.leshoraa.scanorea.features.imagestopdf.data.ImageDecoderDataSource
import com.leshoraa.scanorea.features.imagestopdf.data.PdfGeneratorDataSource
import com.leshoraa.scanorea.features.imagestopdf.data.repository.PdfConversionRepositoryImpl
import com.leshoraa.scanorea.features.presets.data.PresetRepository
import com.leshoraa.scanorea.features.recentpdfs.data.RecentPdfsRepository
import com.leshoraa.scanorea.features.settings.data.SettingsRepository

/**
 * Application-scoped dependency container.
 *
 * Repositories hold in-memory caches (recent PDFs, pins, presets). Creating more than one
 * instance causes those caches to drift out of sync, which is why every ViewModel must
 * obtain its dependencies from here instead of calling `new`.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val recentPdfsRepository: RecentPdfsRepository by lazy {
        RecentPdfsRepository(appContext)
    }

    val presetRepository: PresetRepository by lazy {
        PresetRepository(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val imageDecoderDataSource: ImageDecoderDataSource by lazy {
        ImageDecoderDataSource(appContext)
    }

    val pdfGeneratorDataSource: PdfGeneratorDataSource by lazy {
        PdfGeneratorDataSource(imageDecoderDataSource)
    }

    val pdfConversionRepository: PdfConversionRepositoryImpl by lazy {
        PdfConversionRepositoryImpl(appContext, pdfGeneratorDataSource)
    }
}

/**
 * Returns the single application-wide [AppContainer].
 *
 * Falls back to a scoped container when the application class is not [ScanoreaApplication]
 * (for example in some test harnesses) so callers never crash.
 */
fun Context.appContainer(): AppContainer =
    (applicationContext as? ScanoreaApplication)?.container ?: AppContainer(applicationContext)
