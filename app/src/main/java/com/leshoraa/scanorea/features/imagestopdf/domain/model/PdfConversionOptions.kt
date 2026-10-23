package com.leshoraa.scanorea.features.imagestopdf.domain.model

import com.leshoraa.scanorea.features.imagestopdf.domain.PdfFileNameResolver

/**
 * User-configurable settings for generating the PDF document.
 */
data class PdfConversionOptions(
    val fileName: String = "",
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfPageOrientation = PdfPageOrientation.AUTO,
    val compressionProfile: CompressionProfile = CompressionProfile.AUTO_BALANCED,
    val marginPoints: Float = 16f
) {
    /**
     * Returns a sanitized file name ending with .pdf, eliminating invalid filesystem characters.
     */
    fun sanitizedFileName(): String {
        return PdfFileNameResolver.sanitize(fileName, System.currentTimeMillis())
    }
}
