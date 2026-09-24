package com.leshoraa.scanorea.features.imagestopdf.domain.model

import android.net.Uri

import com.leshoraa.scanorea.core.filter.ImageFilterType

import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation

/**
 * Represents a single image page intended to be included in the PDF.
 *
 * @param id Unique identifier for list diffing and reordering.
 * @param uri Content URI pointing to the image file.
 * @param displayName Optional file name for display purposes.
 * @param sizeInBytes Optional size of the file for user information.
 * @param filter Color enhancement or monochrome filter applied to this page (defaults to B&W).
 * @param contrast Contrast/sharpness multiplier (default 1.0f).
 * @param brightness Brightness offset (default 0.0f).
 * @param rotationDegrees Clockwise rotation in degrees: 0, 90, 180, or 270 (default 0).
 * @param cropBounds Normalized crop boundaries (default full image).
 * @param width Original intrinsic image width in pixels.
 * @param height Original intrinsic image height in pixels.
 * @param initialContrast Auto-calibrated initial contrast recommendation from device analysis.
 * @param initialBrightness Auto-calibrated initial brightness recommendation from device analysis.
 * @param annotations Vector annotations drawn on this page.
 */
data class ImagePage(
    val id: String,
    val uri: Uri,
    val displayName: String? = null,
    val sizeInBytes: Long? = null,
    val filter: ImageFilterType = ImageFilterType.BLACK_AND_WHITE,
    val contrast: Float = 1.0f,
    val brightness: Float = 0.0f,
    val rotationDegrees: Int = 0,
    val cropBounds: ImageCropBounds = ImageCropBounds(),
    val width: Int = 0,
    val height: Int = 0,
    val initialContrast: Float = 1.0f,
    val initialBrightness: Float = 0.0f,
    val annotations: List<PageAnnotation> = emptyList()
) {
    /**
     * Resolves the effective uncropped aspect ratio (width / height) accounting for 90° / 270° orientation.
     */
    val effectiveAspectRatio: Float
        get() {
            if (width <= 0 || height <= 0) return 1.0f
            val isRotatedSideways = (rotationDegrees / 90) % 2 != 0
            val effectiveW = if (isRotatedSideways) height else width
            val effectiveH = if (isRotatedSideways) width else height
            return (effectiveW.toFloat() / effectiveH.toFloat()).coerceIn(0.05f, 20f)
        }
}
