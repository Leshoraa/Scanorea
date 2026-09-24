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
            val newL = (currentBounds.left + deltaX).coerceIn(0f, 1f - boxWidth)
            val newT = (currentBounds.top + deltaY).coerceIn(0f, 1f - boxHeight)
            return ImageCropBounds(newL, newT, newL + boxWidth, newT + boxHeight)
        }

        if (cropAspectRatio == CropAspectRatio.FREE) {
            var newL = currentBounds.left
            var newT = currentBounds.top
            var newR = currentBounds.right
            var newB = currentBounds.bottom

            when (activeHandle) {
                DragHandle.TOP_LEFT -> {
                    newL = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                    newT = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.TOP_RIGHT -> {
                    newR = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                    newT = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.BOTTOM_LEFT -> {
                    newL = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                    newB = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.BOTTOM_RIGHT -> {
                    newR = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                    newB = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.TOP_EDGE -> {
                    newT = (currentBounds.top + deltaY).coerceIn(0f, currentBounds.bottom - minSize)
                }
                DragHandle.BOTTOM_EDGE -> {
                    newB = (currentBounds.bottom + deltaY).coerceIn(currentBounds.top + minSize, 1f)
                }
                DragHandle.LEFT_EDGE -> {
                    newL = (currentBounds.left + deltaX).coerceIn(0f, currentBounds.right - minSize)
                }
                DragHandle.RIGHT_EDGE -> {
                    newR = (currentBounds.right + deltaX).coerceIn(currentBounds.left + minSize, 1f)
                }
                DragHandle.BODY, DragHandle.NONE -> {}
            }

            return if (newL < newR && newT < newB) {
                ImageCropBounds.ofClamped(newL, newT, newR, newB, minSize)
            } else {
                currentBounds
            }
        }

        // Constrained aspect ratio calculations
        val targetPhysicalRatio: Float = when (cropAspectRatio) {
            CropAspectRatio.FREE -> 1.0f
            CropAspectRatio.ORIGINAL -> effectiveAspectRatio
            CropAspectRatio.A4 -> CropAspectRatio.A4.ratio ?: (1f / 1.4142f)
            CropAspectRatio.SQUARE -> 1.0f
        }
        val normRatio = (targetPhysicalRatio / effectiveAspectRatio).coerceIn(0.01f, 100f)
        val prevW = currentBounds.right - currentBounds.left
        val prevH = currentBounds.bottom - currentBounds.top
        val minW = if (normRatio >= 1f) (minSize * normRatio).coerceIn(minSize, 0.9f) else minSize
        val minH = if (normRatio >= 1f) minSize else (minSize / normRatio).coerceIn(minSize, 0.9f)

        var newL = currentBounds.left
        var newT = currentBounds.top
        var newR = currentBounds.right
        var newB = currentBounds.bottom

        when (activeHandle) {
            DragHandle.BOTTOM_RIGHT -> {
                val maxW = (1f - currentBounds.left).coerceAtLeast(minW)
                val maxH = (1f - currentBounds.top).coerceAtLeast(minH)
                val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                val limitH = limitW / normRatio

                val reqW = prevW + deltaX
                val reqH = prevH + deltaY
                var w = if (abs(deltaX) >= abs(deltaY * normRatio)) {
                    reqW.coerceIn(minW, limitW)
                } else {
                    (reqH * normRatio).coerceIn(minW, limitW)
                }
                var h = w / normRatio
                if (h > limitH) {
                    h = limitH
                    w = h * normRatio
                }
                newR = newL + w
                newB = newT + h
            }
            DragHandle.TOP_LEFT -> {
                val maxW = currentBounds.right.coerceAtLeast(minW)
                val maxH = currentBounds.bottom.coerceAtLeast(minH)
                val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                val limitH = limitW / normRatio

                val reqW = prevW - deltaX
                val reqH = prevH - deltaY
                var w = if (abs(deltaX) >= abs(deltaY * normRatio)) {
                    reqW.coerceIn(minW, limitW)
                } else {
                    (reqH * normRatio).coerceIn(minW, limitW)
                }
                var h = w / normRatio
                if (h > limitH) {
                    h = limitH
                    w = h * normRatio
                }
                newL = newR - w
                newT = newB - h
            }
            DragHandle.TOP_RIGHT -> {
                val maxW = (1f - currentBounds.left).coerceAtLeast(minW)
                val maxH = currentBounds.bottom.coerceAtLeast(minH)
                val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                val limitH = limitW / normRatio

                val reqW = prevW + deltaX
                val reqH = prevH - deltaY
                var w = if (abs(deltaX) >= abs(deltaY * normRatio)) {
                    reqW.coerceIn(minW, limitW)
                } else {
                    (reqH * normRatio).coerceIn(minW, limitW)
                }
                var h = w / normRatio
                if (h > limitH) {
                    h = limitH
                    w = h * normRatio
                }
                newR = newL + w
                newT = newB - h
            }
            DragHandle.BOTTOM_LEFT -> {
                val maxW = currentBounds.right.coerceAtLeast(minW)
                val maxH = (1f - currentBounds.top).coerceAtLeast(minH)
                val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                val limitH = limitW / normRatio

                val reqW = prevW - deltaX
                val reqH = prevH + deltaY
                var w = if (abs(deltaX) >= abs(deltaY * normRatio)) {
                    reqW.coerceIn(minW, limitW)
                } else {
                    (reqH * normRatio).coerceIn(minW, limitW)
                }
                var h = w / normRatio
                if (h > limitH) {
                    h = limitH
                    w = h * normRatio
                }
                newL = newR - w
                newB = newT + h
            }
            DragHandle.LEFT_EDGE, DragHandle.RIGHT_EDGE -> {
                val maxW = (if (normRatio <= 1f) normRatio else 1f).coerceAtLeast(minW)
                val reqW = if (activeHandle == DragHandle.RIGHT_EDGE) prevW + deltaX else prevW - deltaX
                val w = reqW.coerceIn(minW, maxW)
                val h = w / normRatio
                val centerH = (currentBounds.top + currentBounds.bottom) / 2f
                var topCandidate = centerH - h / 2f
                var bottomCandidate = centerH + h / 2f
                if (topCandidate < 0f) {
                    bottomCandidate += (0f - topCandidate)
                    topCandidate = 0f
                }
                if (bottomCandidate > 1f) {
                    topCandidate -= (bottomCandidate - 1f)
                    bottomCandidate = 1f
                }
                newT = topCandidate.coerceIn(0f, 1f - h)
                newB = (newT + h).coerceIn(newT + minH, 1f)
                if (activeHandle == DragHandle.RIGHT_EDGE) {
                    newR = (currentBounds.left + w).coerceIn(currentBounds.left + minW, 1f)
                } else {
                    newL = (currentBounds.right - w).coerceIn(0f, currentBounds.right - minW)
                }
            }
            DragHandle.TOP_EDGE, DragHandle.BOTTOM_EDGE -> {
                val maxH = (if (normRatio >= 1f) 1f / normRatio else 1f).coerceAtLeast(minH)
                val reqH = if (activeHandle == DragHandle.BOTTOM_EDGE) prevH + deltaY else prevH - deltaY
                val h = reqH.coerceIn(minH, maxH)
                val w = h * normRatio
                val centerW = (currentBounds.left + currentBounds.right) / 2f
                var leftCandidate = centerW - w / 2f
                var rightCandidate = centerW + w / 2f
                if (leftCandidate < 0f) {
                    rightCandidate += (0f - leftCandidate)
                    leftCandidate = 0f
                }
                if (rightCandidate > 1f) {
                    leftCandidate -= (rightCandidate - 1f)
                    rightCandidate = 1f
                }
                newL = leftCandidate.coerceIn(0f, 1f - w)
                newR = (newL + w).coerceIn(newL + minW, 1f)
                if (activeHandle == DragHandle.BOTTOM_EDGE) {
                    newB = (currentBounds.top + h).coerceIn(currentBounds.top + minH, 1f)
                } else {
                    newT = (currentBounds.bottom - h).coerceIn(0f, currentBounds.bottom - minH)
                }
            }
            DragHandle.BODY, DragHandle.NONE -> {}
        }

        return if (newL < newR && newT < newB) {
            ImageCropBounds(newL, newT, newR, newB)
        } else {
            currentBounds
        }
    }
}
