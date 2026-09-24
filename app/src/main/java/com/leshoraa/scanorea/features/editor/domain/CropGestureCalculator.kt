package com.leshoraa.scanorea.features.editor.domain

import com.leshoraa.scanorea.features.editor.ui.components.DragHandle
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import kotlin.math.abs

/**
 * Pure calculation module for crop boundary drag gestures.
 * Handles freeform cropping, aspect-ratio locked resizing, and whole-body dragging.
 */
object CropGestureCalculator {

    fun updateBounds(
        currentBounds: ImageCropBounds,
        activeHandle: DragHandle,
        deltaX: Float,
        deltaY: Float,
        cropAspectRatio: CropAspectRatio = CropAspectRatio.FREE,
        effectiveAspectRatio: Float = 1.0f,
        minSize: Float = 0.08f
    ): ImageCropBounds {
        if (activeHandle == DragHandle.NONE) return currentBounds

        if (activeHandle == DragHandle.BODY) {
            val boxWidth = currentBounds.right - currentBounds.left
            val boxHeight = currentBounds.bottom - currentBounds.top
            val newLeft = (currentBounds.left + deltaX).coerceIn(0f, 1f - boxWidth)
            val newTop = (currentBounds.top + deltaY).coerceIn(0f, 1f - boxHeight)
            return ImageCropBounds(newLeft, newTop, newLeft + boxWidth, newTop + boxHeight)
        }

        if (cropAspectRatio == CropAspectRatio.FREE) {
            var newLeft = currentBounds.left
            var newTop = currentBounds.top
            var newRight = currentBounds.right
            var newBottom = currentBounds.bottom

            when (activeHandle) {
                DragHandle.TOP_LEFT -> {
                    newLeft = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                    newTop = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.TOP_RIGHT -> {
                    newRight = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                    newTop = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.BOTTOM_LEFT -> {
                    newLeft = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                    newBottom = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.BOTTOM_RIGHT -> {
                    newRight = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                    newBottom = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.TOP_EDGE -> {
                    newTop = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.BOTTOM_EDGE -> {
                    newBottom = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.LEFT_EDGE -> {
                    newLeft = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                }
                DragHandle.RIGHT_EDGE -> {
                    newRight = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                }
                DragHandle.BODY, DragHandle.NONE -> {}
            }

            return if (newLeft < newRight && newTop < newBottom) {
                ImageCropBounds.ofClamped(newLeft, newTop, newRight, newBottom, minSize)
            } else {
                currentBounds
            }
        }

        val targetPhysicalRatio: Float = when (cropAspectRatio) {
            CropAspectRatio.FREE -> 1.0f
            CropAspectRatio.ORIGINAL -> effectiveAspectRatio
            CropAspectRatio.A4 -> CropAspectRatio.A4.ratio ?: (1f / 1.4142f)
            CropAspectRatio.SQUARE -> 1.0f
        }
        val normalizedRatio = (targetPhysicalRatio / effectiveAspectRatio).coerceIn(0.01f, 100f)
        val previousWidth = currentBounds.right - currentBounds.left
        val previousHeight = currentBounds.bottom - currentBounds.top
        val minimumWidth = if (normalizedRatio >= 1f) (minSize * normalizedRatio).coerceIn(minSize, 0.9f) else minSize
        val minimumHeight = if (normalizedRatio >= 1f) minSize else (minSize / normalizedRatio).coerceIn(minSize, 0.9f)

        var newLeft = currentBounds.left
        var newTop = currentBounds.top
        var newRight = currentBounds.right
        var newBottom = currentBounds.bottom

        when (activeHandle) {
            DragHandle.BOTTOM_RIGHT -> {
                val maximumWidth = (1f - currentBounds.left).coerceAtLeast(minimumWidth)
                val maximumHeight = (1f - currentBounds.top).coerceAtLeast(minimumHeight)
                val limitWidth = if (maximumWidth / maximumHeight > normalizedRatio) maximumHeight * normalizedRatio else maximumWidth
                val limitHeight = limitWidth / normalizedRatio

                val requestedWidth = previousWidth + deltaX
                val requestedHeight = previousHeight + deltaY
                var constrainedWidth = if (abs(deltaX) >= abs(deltaY * normalizedRatio)) {
                    requestedWidth.coerceIn(minimumWidth, limitWidth)
                } else {
                    (requestedHeight * normalizedRatio).coerceIn(minimumWidth, limitWidth)
                }
                var constrainedHeight = constrainedWidth / normalizedRatio
                if (constrainedHeight > limitHeight) {
                    constrainedHeight = limitHeight
                    constrainedWidth = constrainedHeight * normalizedRatio
                }
                newRight = newLeft + constrainedWidth
                newBottom = newTop + constrainedHeight
            }
            DragHandle.TOP_LEFT -> {
                val maximumWidth = currentBounds.right.coerceAtLeast(minimumWidth)
                val maximumHeight = currentBounds.bottom.coerceAtLeast(minimumHeight)
                val limitWidth = if (maximumWidth / maximumHeight > normalizedRatio) maximumHeight * normalizedRatio else maximumWidth
                val limitHeight = limitWidth / normalizedRatio

                val requestedWidth = previousWidth - deltaX
                val requestedHeight = previousHeight - deltaY
                var constrainedWidth = if (abs(deltaX) >= abs(deltaY * normalizedRatio)) {
                    requestedWidth.coerceIn(minimumWidth, limitWidth)
                } else {
                    (requestedHeight * normalizedRatio).coerceIn(minimumWidth, limitWidth)
                }
                var constrainedHeight = constrainedWidth / normalizedRatio
                if (constrainedHeight > limitHeight) {
                    constrainedHeight = limitHeight
                    constrainedWidth = constrainedHeight * normalizedRatio
                }
                newLeft = newRight - constrainedWidth
                newTop = newBottom - constrainedHeight
            }
            DragHandle.TOP_RIGHT -> {
                val maximumWidth = (1f - currentBounds.left).coerceAtLeast(minimumWidth)
                val maximumHeight = currentBounds.bottom.coerceAtLeast(minimumHeight)
                val limitWidth = if (maximumWidth / maximumHeight > normalizedRatio) maximumHeight * normalizedRatio else maximumWidth
                val limitHeight = limitWidth / normalizedRatio

                val requestedWidth = previousWidth + deltaX
                val requestedHeight = previousHeight - deltaY
                var constrainedWidth = if (abs(deltaX) >= abs(deltaY * normalizedRatio)) {
                    requestedWidth.coerceIn(minimumWidth, limitWidth)
                } else {
                    (requestedHeight * normalizedRatio).coerceIn(minimumWidth, limitWidth)
                }
                var constrainedHeight = constrainedWidth / normalizedRatio
                if (constrainedHeight > limitHeight) {
                    constrainedHeight = limitHeight
                    constrainedWidth = constrainedHeight * normalizedRatio
                }
                newRight = newLeft + constrainedWidth
                newTop = newBottom - constrainedHeight
            }
            DragHandle.BOTTOM_LEFT -> {
                val maximumWidth = currentBounds.right.coerceAtLeast(minimumWidth)
                val maximumHeight = (1f - currentBounds.top).coerceAtLeast(minimumHeight)
                val limitWidth = if (maximumWidth / maximumHeight > normalizedRatio) maximumHeight * normalizedRatio else maximumWidth
                val limitHeight = limitWidth / normalizedRatio

                val requestedWidth = previousWidth - deltaX
                val requestedHeight = previousHeight + deltaY
                var constrainedWidth = if (abs(deltaX) >= abs(deltaY * normalizedRatio)) {
                    requestedWidth.coerceIn(minimumWidth, limitWidth)
                } else {
                    (requestedHeight * normalizedRatio).coerceIn(minimumWidth, limitWidth)
                }
                var constrainedHeight = constrainedWidth / normalizedRatio
                if (constrainedHeight > limitHeight) {
                    constrainedHeight = limitHeight
                    constrainedWidth = constrainedHeight * normalizedRatio
                }
                newLeft = newRight - constrainedWidth
                newBottom = newTop + constrainedHeight
            }
            DragHandle.LEFT_EDGE, DragHandle.RIGHT_EDGE -> {
                val maximumWidth = (if (normalizedRatio <= 1f) normalizedRatio else 1f).coerceAtLeast(minimumWidth)
                val requestedWidth = if (activeHandle == DragHandle.RIGHT_EDGE) previousWidth + deltaX else previousWidth - deltaX
                val constrainedWidth = requestedWidth.coerceIn(minimumWidth, maximumWidth)
                val constrainedHeight = constrainedWidth / normalizedRatio
                val centerVertical = (currentBounds.top + currentBounds.bottom) / 2f
                var topCandidate = centerVertical - constrainedHeight / 2f
                var bottomCandidate = centerVertical + constrainedHeight / 2f
                if (topCandidate < 0f) {
                    bottomCandidate += (0f - topCandidate)
                    topCandidate = 0f
                }
                if (bottomCandidate > 1f) {
                    topCandidate -= (bottomCandidate - 1f)
                    bottomCandidate = 1f
                }
                newTop = topCandidate.coerceIn(0f, 1f - constrainedHeight)
                newBottom = (newTop + constrainedHeight).coerceIn(newTop + minimumHeight, 1f)
                if (activeHandle == DragHandle.RIGHT_EDGE) {
                    newRight = (currentBounds.left + constrainedWidth).coerceIn(currentBounds.left + minimumWidth, 1f)
                } else {
                    newLeft = (currentBounds.right - constrainedWidth).coerceIn(0f, currentBounds.right - minimumWidth)
                }
            }
            DragHandle.TOP_EDGE, DragHandle.BOTTOM_EDGE -> {
                val maximumHeight = (if (normalizedRatio >= 1f) 1f / normalizedRatio else 1f).coerceAtLeast(minimumHeight)
                val requestedHeight = if (activeHandle == DragHandle.BOTTOM_EDGE) previousHeight + deltaY else previousHeight - deltaY
                val constrainedHeight = requestedHeight.coerceIn(minimumHeight, maximumHeight)
                val constrainedWidth = constrainedHeight * normalizedRatio
                val centerHorizontal = (currentBounds.left + currentBounds.right) / 2f
                var leftCandidate = centerHorizontal - constrainedWidth / 2f
                var rightCandidate = centerHorizontal + constrainedWidth / 2f
                if (leftCandidate < 0f) {
                    rightCandidate += (0f - leftCandidate)
                    leftCandidate = 0f
                }
                if (rightCandidate > 1f) {
                    leftCandidate -= (rightCandidate - 1f)
                    rightCandidate = 1f
                }
                newLeft = leftCandidate.coerceIn(0f, 1f - constrainedWidth)
                newRight = (newLeft + constrainedWidth).coerceIn(newLeft + minimumWidth, 1f)
                if (activeHandle == DragHandle.BOTTOM_EDGE) {
                    newBottom = (currentBounds.top + constrainedHeight).coerceIn(currentBounds.top + minimumHeight, 1f)
                } else {
                    newTop = (currentBounds.bottom - constrainedHeight).coerceIn(0f, currentBounds.bottom - minimumHeight)
                }
            }
            DragHandle.BODY, DragHandle.NONE -> {}
        }

        return if (newLeft < newRight && newTop < newBottom) {
            ImageCropBounds(newLeft, newTop, newRight, newBottom)
        } else {
            currentBounds
        }
    }
}
