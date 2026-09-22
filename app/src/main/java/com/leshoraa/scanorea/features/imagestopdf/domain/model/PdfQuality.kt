package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Quality and compression profiles for optimizing PDF size and memory consumption.
 *
 * @param maxDimensionPixels Maximum width or height constraint before downsampling.
 * @param jpegQuality Compression quality for bitmaps (0-100).
 * @param label Human-readable description.
 */
enum class PdfQuality(
    val maxDimensionPixels: Int,
    val jpegQuality: Int,
    val label: String
) {
    LOW(1280, 60, "Low (Smallest file size)"),
    MEDIUM(1920, 80, "Medium (Balanced)"),
    HIGH(2560, 92, "High (Best clarity)"),
    ORIGINAL(4096, 100, "Original (Maximum quality)")
}
