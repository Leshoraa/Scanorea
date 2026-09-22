package com.leshoraa.scanorea.features.imagestopdf.domain.model

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
        val trimmed = fileName.trim()
        val baseName = if (trimmed.isBlank()) {
            "Scanorea_${System.currentTimeMillis()}"
        } else {
            trimmed.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        }
        return if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
    }
}
