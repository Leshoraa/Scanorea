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
     * Estimates the generated PDF file size for a given [pageCount].
     */
    fun estimateSizeBytes(pageCount: Int): Long {
        if (pageCount <= 0) return 0L
        // Base PDF catalog overhead (~15KB) + per-page payload
        val baseOverhead = 15 * 1024L
        return baseOverhead + (pageCount * estimatedBytesPerPage)
    }
}
