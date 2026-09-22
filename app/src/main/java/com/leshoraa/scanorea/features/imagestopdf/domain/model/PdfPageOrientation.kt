package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Orientation options for PDF pages.
 */
enum class PdfPageOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto (Match Image)")
}
