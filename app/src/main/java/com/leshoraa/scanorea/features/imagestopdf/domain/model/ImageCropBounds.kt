package com.leshoraa.scanorea.features.imagestopdf.domain.model

/**
 * Normalized crop boundaries within the range [0.0f, 1.0f].
 *
 * @param left Normalized left coordinate (0.0f = left edge).
 * @param top Normalized top coordinate (0.0f = top edge).
 * @param right Normalized right coordinate (1.0f = right edge).
 * @param bottom Normalized bottom coordinate (1.0f = bottom edge).
 */
data class ImageCropBounds(
    val left: Float = 0.0f,
    val top: Float = 0.0f,
    val right: Float = 1.0f,
    val bottom: Float = 1.0f
) {
    init {
        require(left in 0.0f..1.0f) { "left must be in [0, 1], got $left" }
        require(top in 0.0f..1.0f) { "top must be in [0, 1], got $top" }
        require(right in 0.0f..1.0f) { "right must be in [0, 1], got $right" }
        require(bottom in 0.0f..1.0f) { "bottom must be in [0, 1], got $bottom" }
        require(left < right) { "left must be less than right ($left >= $right)" }
        require(top < bottom) { "top must be less than bottom ($top >= $bottom)" }
    }

    val isDefault: Boolean
        get() = left == 0.0f && top == 0.0f && right == 1.0f && bottom == 1.0f

    val widthFraction: Float
        get() = (right - left).coerceAtLeast(0.01f)

    val heightFraction: Float
        get() = (bottom - top).coerceAtLeast(0.01f)

    companion object {
        val DEFAULT = ImageCropBounds()

        /**
         * Creates centered crop bounds conforming to the specified width-to-height aspect ratio.
         *
         * @param targetRatio Desired physical width-to-height aspect ratio (e.g., 1.0 for square, 1/1.4142 for A4).
         * @param imageAspectRatio The physical aspect ratio of the uncropped image (width / height).
         */
        fun fromAspectRatio(targetRatio: Float, imageAspectRatio: Float = 1.0f): ImageCropBounds {
            val clampedTarget = targetRatio.coerceIn(0.05f, 20f)
            val clampedImgAspect = if (imageAspectRatio <= 0f) 1.0f else imageAspectRatio.coerceIn(0.05f, 20f)

            // In normalized [0..1] x [0..1] coordinates:
            // targetRatio = (w_norm * imageWidth) / (h_norm * imageHeight) = (w_norm / h_norm) * imageAspectRatio
            // Therefore: w_norm / h_norm = targetRatio / imageAspectRatio
            val normalizedRatio = (clampedTarget / clampedImgAspect).coerceIn(0.01f, 100f)

            return if (normalizedRatio <= 1.0f) {
                val targetWidthRatio = normalizedRatio
                val left = ((1.0f - targetWidthRatio) / 2f).coerceIn(0f, 0.5f)
                val right = (left + targetWidthRatio).coerceAtMost(1f)
                ImageCropBounds(
                    left = left,
                    top = 0f,
                    right = right,
                    bottom = 1f
                )
            } else {
                val targetHeightRatio = 1.0f / normalizedRatio
                val top = ((1.0f - targetHeightRatio) / 2f).coerceIn(0f, 0.5f)
                val bottom = (top + targetHeightRatio).coerceAtMost(1f)
                ImageCropBounds(
                    left = 0f,
                    top = top,
                    right = 1f,
                    bottom = bottom
                )
            }
        }

        /**
         * Clamps arbitrary input values to valid normalized bounds with a minimum size.
         */
        fun ofClamped(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            minDimension: Float = 0.05f
        ): ImageCropBounds {
            val clampedL = left.coerceIn(0f, 1f - minDimension)
            val clampedT = top.coerceIn(0f, 1f - minDimension)
            val clampedR = right.coerceIn(clampedL + minDimension, 1f)
            val clampedB = bottom.coerceIn(clampedT + minDimension, 1f)
            return ImageCropBounds(clampedL, clampedT, clampedR, clampedB)
        }
    }
}

/**
 * Aspect ratio presets for the document image crop tool.
 */
enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", null),
    A4("A4 Doc", 1f / 1.4142f),
    SQUARE("1:1", 1.0f)
}

