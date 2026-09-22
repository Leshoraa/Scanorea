package com.leshoraa.scanorea.features.imagestopdf.domain.repository

import com.leshoraa.scanorea.core.common.ResourceResult
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ConversionProgress
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface exposing PDF conversion capabilities to higher application layers.
 */
interface PdfConversionRepository {

    /**
     * Converts a collection of [ImagePage] items into a single PDF document.
     * Emits progress updates and concludes with [PdfConversionResult].
     * If [destinationFolderUri] is provided, an exported copy is saved to the selected SAF folder.
     */
    fun convertImagesToPdf(
        pages: List<ImagePage>,
        options: PdfConversionOptions,
        destinationFolderUri: android.net.Uri? = null,
        onProgress: (ConversionProgress) -> Unit = {}
    ): Flow<ResourceResult<PdfConversionResult>>
}
