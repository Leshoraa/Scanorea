package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Handle positions for resizing and moving selected markup layer boxes.
 */
enum class AnnotationResizeHandle {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    RIGHT_EDGE
}

/**
 * Helper object providing geometric calculations and hit testing for box markup annotations.
 */
object AnnotationLayerHelper {

    fun getNormalizedBounds(annotation: PageAnnotation): Rect? {
        return when (annotation) {
            is PageAnnotation.RectBox -> {
                Rect(
                    left = min(annotation.left, annotation.right),
                    top = min(annotation.top, annotation.bottom),
                    right = max(annotation.left, annotation.right),
                    bottom = max(annotation.top, annotation.bottom)
                )
            }
            is PageAnnotation.OvalShape -> {
                Rect(
                    left = min(annotation.left, annotation.right),
                    top = min(annotation.top, annotation.bottom),
                    right = max(annotation.left, annotation.right),
                    bottom = max(annotation.top, annotation.bottom)
                )
            }
            else -> null
        }
    }

    fun updateBounds(annotation: PageAnnotation, newBounds: Rect): PageAnnotation {
        return when (annotation) {
            is PageAnnotation.RectBox -> annotation.copy(
                left = newBounds.left,
                top = newBounds.top,
                right = newBounds.right,
                bottom = newBounds.bottom
            )
            is PageAnnotation.OvalShape -> annotation.copy(
                left = newBounds.left,
                top = newBounds.top,
                right = newBounds.right,
                bottom = newBounds.bottom
            )
            else -> annotation
        }
    }

    fun calculateResizedBounds(
        bounds: Rect,
        handle: AnnotationResizeHandle,
        deltaX: Float,
        deltaY: Float,
        minSize: Float = 0.005f
    ): Rect {
        return when (handle) {
            AnnotationResizeHandle.BODY -> {
                val w = bounds.width
                val h = bounds.height
                val newLeft = (bounds.left + deltaX).coerceIn(0f, (1f - w).coerceAtLeast(0f))
                val newTop = (bounds.top + deltaY).coerceIn(0f, (1f - h).coerceAtLeast(0f))
                Rect(newLeft, newTop, newLeft + w, newTop + h)
            }
            AnnotationResizeHandle.TOP_LEFT -> {
                val newLeft = (bounds.left + deltaX).coerceIn(0f, bounds.right - minSize)
                val newTop = (bounds.top + deltaY).coerceIn(0f, bounds.bottom - minSize)
                Rect(newLeft, newTop, bounds.right, bounds.bottom)
            }
            AnnotationResizeHandle.TOP_RIGHT -> {
                val newRight = (bounds.right + deltaX).coerceIn(bounds.left + minSize, 1f)
                val newTop = (bounds.top + deltaY).coerceIn(0f, bounds.bottom - minSize)
                Rect(bounds.left, newTop, newRight, bounds.bottom)
            }
            AnnotationResizeHandle.BOTTOM_LEFT -> {
                val newLeft = (bounds.left + deltaX).coerceIn(0f, bounds.right - minSize)
                val newBottom = (bounds.bottom + deltaY).coerceIn(bounds.top + minSize, 1f)
                Rect(newLeft, bounds.top, bounds.right, newBottom)
            }
            AnnotationResizeHandle.BOTTOM_RIGHT -> {
                val newRight = (bounds.right + deltaX).coerceIn(bounds.left + minSize, 1f)
                val newBottom = (bounds.bottom + deltaY).coerceIn(bounds.top + minSize, 1f)
                Rect(bounds.left, bounds.top, newRight, newBottom)
            }
            AnnotationResizeHandle.TOP_EDGE -> {
                val newTop = (bounds.top + deltaY).coerceIn(0f, bounds.bottom - minSize)
                Rect(bounds.left, newTop, bounds.right, bounds.bottom)
            }
            AnnotationResizeHandle.BOTTOM_EDGE -> {
                val newBottom = (bounds.bottom + deltaY).coerceIn(bounds.top + minSize, 1f)
                Rect(bounds.left, bounds.top, bounds.right, newBottom)
            }
            AnnotationResizeHandle.LEFT_EDGE -> {
                val newLeft = (bounds.left + deltaX).coerceIn(0f, bounds.right - minSize)
                Rect(newLeft, bounds.top, bounds.right, bounds.bottom)
            }
            AnnotationResizeHandle.RIGHT_EDGE -> {
                val newRight = (bounds.right + deltaX).coerceIn(bounds.left + minSize, 1f)
                Rect(bounds.left, bounds.top, newRight, bounds.bottom)
            }
            AnnotationResizeHandle.NONE -> bounds
        }
    }

    fun isTouchOnToolbar(
        touchPos: Offset,
        bounds: Rect,
        imgLeft: Float,
        imgTop: Float,
        imgW: Float,
        imgH: Float,
        density: Float
    ): Boolean {
        val screenLeft = imgLeft + bounds.left * imgW
        val screenRight = imgLeft + bounds.right * imgW
        val screenTop = imgTop + bounds.top * imgH
        val screenBottom = imgTop + bounds.bottom * imgH
        val screenMidX = (screenLeft + screenRight) / 2f

        val toolbarW = 86f * density
        val toolbarH = 40f * density
        val toolbarMargin = 8f * density

        val toolbarTop = if (screenTop - imgTop > toolbarH + toolbarMargin) {
            screenTop - toolbarH - toolbarMargin
        } else {
            screenBottom + toolbarMargin
        }
        val toolbarLeft = (screenMidX - toolbarW / 2f).coerceIn(imgLeft + 4f, (imgLeft + imgW - toolbarW - 4f).coerceAtLeast(imgLeft))

        return touchPos.x in (toolbarLeft - 10f)..(toolbarLeft + toolbarW + 10f) &&
                touchPos.y in (toolbarTop - 10f)..(toolbarTop + toolbarH + 10f)
    }

    fun hitTestHandle(
        touchPos: Offset,
        bounds: Rect,
        imgLeft: Float,
        imgTop: Float,
        imgW: Float,
        imgH: Float,
        cornerHitRadius: Float,
        edgeHitRadius: Float
    ): AnnotationResizeHandle {
        val screenLeft = imgLeft + bounds.left * imgW
        val screenRight = imgLeft + bounds.right * imgW
        val screenTop = imgTop + bounds.top * imgH
        val screenBottom = imgTop + bounds.bottom * imgH
        val screenMidX = (screenLeft + screenRight) / 2f
        val screenMidY = (screenTop + screenBottom) / 2f

        val x = touchPos.x
        val y = touchPos.y

        val boxW = screenRight - screenLeft
        val boxH = screenBottom - screenTop
        val effectiveCornerRadius = min(cornerHitRadius, min(boxW, boxH) * 0.45f)
        val effectiveEdgeRadius = min(edgeHitRadius, min(boxW, boxH) * 0.35f)

        // Check 4 corners with scaled hit threshold
        if (hypot(x - screenLeft, y - screenTop) < effectiveCornerRadius) return AnnotationResizeHandle.TOP_LEFT
        if (hypot(x - screenRight, y - screenTop) < effectiveCornerRadius) return AnnotationResizeHandle.TOP_RIGHT
        if (hypot(x - screenLeft, y - screenBottom) < effectiveCornerRadius) return AnnotationResizeHandle.BOTTOM_LEFT
        if (hypot(x - screenRight, y - screenBottom) < effectiveCornerRadius) return AnnotationResizeHandle.BOTTOM_RIGHT

        // Check 4 edges (prioritizing midpoint handles)
        val edgePaddingX = min(12f, boxW * 0.25f)
        val edgePaddingY = min(12f, boxH * 0.25f)

        if (hypot(x - screenMidX, y - screenTop) < effectiveEdgeRadius ||
            (kotlin.math.abs(y - screenTop) < effectiveEdgeRadius && x in (screenLeft + edgePaddingX)..(screenRight - edgePaddingX))
        ) return AnnotationResizeHandle.TOP_EDGE

        if (hypot(x - screenMidX, y - screenBottom) < effectiveEdgeRadius ||
            (kotlin.math.abs(y - screenBottom) < effectiveEdgeRadius && x in (screenLeft + edgePaddingX)..(screenRight - edgePaddingX))
        ) return AnnotationResizeHandle.BOTTOM_EDGE

        if (hypot(x - screenLeft, y - screenMidY) < effectiveEdgeRadius ||
            (kotlin.math.abs(x - screenLeft) < effectiveEdgeRadius && y in (screenTop + edgePaddingY)..(screenBottom - edgePaddingY))
        ) return AnnotationResizeHandle.LEFT_EDGE

        if (hypot(x - screenRight, y - screenMidY) < effectiveEdgeRadius ||
            (kotlin.math.abs(x - screenRight) < effectiveEdgeRadius && y in (screenTop + edgePaddingY)..(screenBottom - edgePaddingY))
        ) return AnnotationResizeHandle.RIGHT_EDGE

        // Check body
        if (x in screenLeft..screenRight && y in screenTop..screenBottom) {
            return AnnotationResizeHandle.BODY
        }

        return AnnotationResizeHandle.NONE
    }
}

/**
 * Interactive visual layer overlay for selected markup boxes.
 * Draws a subtle dashed bounding box, sleek corner nodes, compact edge resize bars, and a floating action pill.
 */
@Composable
fun EditorAnnotationLayerOverlay(
    annotation: PageAnnotation,
    canvasWidth: Float,
    canvasHeight: Float,
    onDelete: () -> Unit = {},
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bounds = AnnotationLayerHelper.getNormalizedBounds(annotation) ?: return
    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    val screenLeft = bounds.left * canvasWidth
    val screenTop = bounds.top * canvasHeight
    val screenRight = bounds.right * canvasWidth
    val screenBottom = bounds.bottom * canvasHeight
    val boxWidth = (screenRight - screenLeft).coerceAtLeast(1f)
    val boxHeight = (screenBottom - screenTop).coerceAtLeast(1f)
    val screenMidX = (screenLeft + screenRight) / 2f
    val screenMidY = (screenTop + screenBottom) / 2f

    val cornerRadiusPx = with(density) { 4.5.dp.toPx() }
    val edgePillW = with(density) { 11.dp.toPx() }
    val edgePillH = with(density) { 3.2.dp.toPx() }
    val strokeWidthPx = with(density) { 1.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // Visual bounding box & resize handles canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

            // Dashed selection bounding box (translucent, subtle)
            drawRect(
                color = primaryColor.copy(alpha = 0.55f),
                topLeft = Offset(screenLeft, screenTop),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = strokeWidthPx, pathEffect = dashEffect)
            )

            // Corner circular nodes (filled green with subtle halo)
            val cornerPositions = listOf(
                Offset(screenLeft, screenTop),
                Offset(screenRight, screenTop),
                Offset(screenLeft, screenBottom),
                Offset(screenRight, screenBottom)
            )

            cornerPositions.forEach { center ->
                // Outer soft halo
                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f),
                    radius = cornerRadiusPx + with(density) { 2.5.dp.toPx() },
                    center = center
                )
                // Solid green fill
                drawCircle(
                    color = primaryColor,
                    radius = cornerRadiusPx,
                    center = center
                )
            }

            // Edge midpoint grab bars (Pill bars - filled green)
            // Top and Bottom edge bars (horizontal)
            listOf(
                Offset(screenMidX - edgePillW / 2f, screenTop - edgePillH / 2f),
                Offset(screenMidX - edgePillW / 2f, screenBottom - edgePillH / 2f)
            ).forEach { topLeft ->
                drawRoundRect(
                    color = primaryColor,
                    topLeft = topLeft,
                    size = Size(edgePillW, edgePillH),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f)
                )
            }

            // Left and Right edge bars (vertical)
            listOf(
                Offset(screenLeft - edgePillH / 2f, screenMidY - edgePillW / 2f),
                Offset(screenRight - edgePillH / 2f, screenMidY - edgePillW / 2f)
            ).forEach { topLeft ->
                drawRoundRect(
                    color = primaryColor,
                    topLeft = topLeft,
                    size = Size(edgePillH, edgePillW),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f)
                )
            }
        }
    }
}
