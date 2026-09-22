package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Standard page sizes supported for PDF generation.
 * Dimension points are based on 72 points per inch (standard PDF coordinate system).
 */
enum class PdfPageSize(val defaultWidthPoints: Int, val defaultHeightPoints: Int, val label: String) {
    A4(595, 842, "A4 (210 x 297 mm)"),
    LETTER(612, 792, "US Letter (8.5 x 11 in)"),
    FIT_TO_IMAGE(0, 0, "Fit to Image")
}
