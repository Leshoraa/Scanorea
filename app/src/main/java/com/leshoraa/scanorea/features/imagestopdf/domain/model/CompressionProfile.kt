package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Intelligent compression profiles with estimated size budgets.
 */
enum class CompressionProfile(
    val label: String,
    val description: String,
    val maxDimensionPixels: Int,
    val jpegQuality: Int,
    private val estimatedBytesPerPage: Long
) {
    AUTO_BALANCED(
        label = "Auto / Balanced",
        description = "Smart optimization for fast sharing & clear text",
        maxDimensionPixels = 1600,
        jpegQuality = 75,
        estimatedBytesPerPage = 250 * 1024L
    ),
    SMALL_FILE(
        label = "Smallest Size",
        description = "Maximum compression for email & message attachments",
        maxDimensionPixels = 1200,
        jpegQuality = 55,
        estimatedBytesPerPage = 120 * 1024L
    ),
    HIGH_QUALITY(
        label = "High Quality",
        description = "Crisp detail for fine print and high-res archiving",
        maxDimensionPixels = 2400,
        jpegQuality = 88,
        estimatedBytesPerPage = 650 * 1024L
    ),
    MAXIMUM(
        label = "Original / Maximum",
        description = "No downsampling, maximum photographic fidelity",
        maxDimensionPixels = 4096,
        jpegQuality = 98,
        estimatedBytesPerPage = 1500 * 1024L
    );

    /**
     * Estimates the generated PDF file size dynamically based on actual pages,
     * accounting for crop dimensions, image resolutions, applied color filters,
     * and compression quality profiles.
     */
    fun estimateSizeBytes(pages: List<ImagePage>): Long {
        if (pages.isEmpty()) return 0L
        val baseOverhead = 12 * 1024L // Base PDF document structures & metadata
        val pagesPayload = pages.sumOf { page -> estimatePageBytes(page) }
        return baseOverhead + pagesPayload
    }

    /**
     * Computes the estimated encoded byte size of an individual page.
     */
    fun estimatePageBytes(page: ImagePage): Long {
        val cropFactor = ((page.cropBounds.right - page.cropBounds.left) * (page.cropBounds.bottom - page.cropBounds.top))
            .coerceIn(0.01f, 1.0f)

        val filterFactor = when (page.filter) {
            com.leshoraa.scanorea.core.filter.ImageFilterType.BLACK_AND_WHITE -> 0.22f
            com.leshoraa.scanorea.core.filter.ImageFilterType.GRAYSCALE -> 0.45f
            com.leshoraa.scanorea.core.filter.ImageFilterType.ORIGINAL,
            com.leshoraa.scanorea.core.filter.ImageFilterType.ENHANCED -> 1.0f
        }

        val baseBytes = if (page.width > 0 && page.height > 0) {
            val longestEdge = kotlin.math.max(page.width, page.height)
            val scale = if (longestEdge > maxDimensionPixels) {
                maxDimensionPixels.toFloat() / longestEdge
            } else {
                1.0f
            }
            val effectivePixels = (page.width * scale * page.height * scale).coerceAtLeast(10000f)
            val bytesPerPixel = (0.28f * (jpegQuality / 75f)).coerceIn(0.12f, 0.75f)
            (effectivePixels * bytesPerPixel).toLong()
        } else if (page.sizeInBytes != null && page.sizeInBytes > 0) {
            val profileRatio = maxDimensionPixels.toFloat() / 2000f
            (page.sizeInBytes * profileRatio * 0.45f).toLong().coerceIn(40 * 1024L, 2000 * 1024L)
        } else {
            estimatedBytesPerPage
        }

        val estimated = (baseBytes * cropFactor * filterFactor).toLong()
        return estimated.coerceAtLeast(15 * 1024L)
    }

    /**
     * Fallback estimator for a given [pageCount] when full page objects are unavailable.
     */
    fun estimateSizeBytes(pageCount: Int): Long {
        if (pageCount <= 0) return 0L
        // Base PDF catalog overhead (~15KB) + per-page payload
        val baseOverhead = 15 * 1024L
        return baseOverhead + (pageCount * estimatedBytesPerPage)
    }
}

