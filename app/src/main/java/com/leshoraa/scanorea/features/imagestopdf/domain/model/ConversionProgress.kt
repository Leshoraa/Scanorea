package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Real-time progress update during the PDF generation pipeline.
 *
 * @param currentPageIndex 1-based index of the page currently being processed.
 * @param totalPages Total number of pages to process.
 */
data class ConversionProgress(
    val currentPageIndex: Int,
    val totalPages: Int
) {
    val progressFraction: Float
        get() = if (totalPages > 0) currentPageIndex.toFloat() / totalPages.toFloat() else 0f
}
