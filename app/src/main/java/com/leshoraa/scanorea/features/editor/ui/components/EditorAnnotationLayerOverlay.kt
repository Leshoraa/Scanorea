package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
        minSize: Float = 0.02f
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

        // 1. Check 4 corners
        if (hypot(x - screenLeft, y - screenTop) < cornerHitRadius) return AnnotationResizeHandle.TOP_LEFT
        if (hypot(x - screenRight, y - screenTop) < cornerHitRadius) return AnnotationResizeHandle.TOP_RIGHT
        if (hypot(x - screenLeft, y - screenBottom) < cornerHitRadius) return AnnotationResizeHandle.BOTTOM_LEFT
        if (hypot(x - screenRight, y - screenBottom) < cornerHitRadius) return AnnotationResizeHandle.BOTTOM_RIGHT

        // 2. Check 4 edges
        val edgePadding = 12f
        if (hypot(x - screenMidX, y - screenTop) < edgeHitRadius ||
            (y in (screenTop - edgeHitRadius)..(screenTop + edgeHitRadius) && x in (screenLeft + edgePadding)..(screenRight - edgePadding))
        ) return AnnotationResizeHandle.TOP_EDGE

        if (hypot(x - screenMidX, y - screenBottom) < edgeHitRadius ||
            (y in (screenBottom - edgeHitRadius)..(screenBottom + edgeHitRadius) && x in (screenLeft + edgePadding)..(screenRight - edgePadding))
        ) return AnnotationResizeHandle.BOTTOM_EDGE

        if (hypot(x - screenLeft, y - screenMidY) < edgeHitRadius ||
            (x in (screenLeft - edgeHitRadius)..(screenLeft + edgeHitRadius) && y in (screenTop + edgePadding)..(screenBottom - edgePadding))
        ) return AnnotationResizeHandle.LEFT_EDGE

        if (hypot(x - screenRight, y - screenMidY) < edgeHitRadius ||
            (x in (screenRight - edgeHitRadius)..(screenRight + edgeHitRadius) && y in (screenTop + edgePadding)..(screenBottom - edgePadding))
        ) return AnnotationResizeHandle.RIGHT_EDGE

        // 3. Check body
        if (x in screenLeft..screenRight && y in screenTop..screenBottom) {
            return AnnotationResizeHandle.BODY
        }

        return AnnotationResizeHandle.NONE
    }
}

/**
 * Interactive visual layer overlay for selected markup boxes.
 * Draws a dashed bounding box, corner nodes, edge resize bars, and a floating action pill.
 */
@Composable
fun EditorAnnotationLayerOverlay(
    annotation: PageAnnotation,
    canvasWidth: Float,
    canvasHeight: Float,
    onDelete: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bounds = AnnotationLayerHelper.getNormalizedBounds(annotation) ?: return
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val density = LocalDensity.current

    val screenLeft = bounds.left * canvasWidth
    val screenTop = bounds.top * canvasHeight
    val screenRight = bounds.right * canvasWidth
    val screenBottom = bounds.bottom * canvasHeight
    val boxWidth = (screenRight - screenLeft).coerceAtLeast(1f)
    val boxHeight = (screenBottom - screenTop).coerceAtLeast(1f)
    val screenMidX = (screenLeft + screenRight) / 2f
    val screenMidY = (screenTop + screenBottom) / 2f

    val cornerRadiusPx = with(density) { 6.dp.toPx() }
    val edgePillW = with(density) { 16.dp.toPx() }
    val edgePillH = with(density) { 4.5.dp.toPx() }
    val strokeWidthPx = with(density) { 1.5.dp.toPx() }

    val toolbarWPx = with(density) { 84.dp.toPx() }
    val toolbarHPx = with(density) { 36.dp.toPx() }
    val toolbarMarginPx = with(density) { 8.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // Visual bounding box & resize handles canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // 1. Dashed selection bounding box
            drawRect(
                color = primaryColor,
                topLeft = Offset(screenLeft, screenTop),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = strokeWidthPx, pathEffect = dashEffect)
            )

            // 2. Corner circular nodes (Surface fill + primary border)
            val cornerPositions = listOf(
                Offset(screenLeft, screenTop),
                Offset(screenRight, screenTop),
                Offset(screenLeft, screenBottom),
                Offset(screenRight, screenBottom)
            )

            cornerPositions.forEach { center ->
                drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = cornerRadiusPx + 1f, center = center + Offset(0f, 1f))
                drawCircle(color = surfaceColor, radius = cornerRadiusPx, center = center)
                drawCircle(color = primaryColor, radius = cornerRadiusPx, center = center, style = Stroke(width = 2.dp.toPx()))
            }

            // 3. Edge midpoint grab bars (Pill bars)
            // Top and Bottom edge bars (horizontal)
            listOf(
                Offset(screenMidX - edgePillW / 2f, screenTop - edgePillH / 2f),
                Offset(screenMidX - edgePillW / 2f, screenBottom - edgePillH / 2f)
            ).forEach { topLeft ->
                drawRoundRect(
                    color = surfaceColor,
                    topLeft = topLeft,
                    size = Size(edgePillW, edgePillH),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f)
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = topLeft,
                    size = Size(edgePillW, edgePillH),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Left and Right edge bars (vertical)
            listOf(
                Offset(screenLeft - edgePillH / 2f, screenMidY - edgePillW / 2f),
                Offset(screenRight - edgePillH / 2f, screenMidY - edgePillW / 2f)
            ).forEach { topLeft ->
                drawRoundRect(
                    color = surfaceColor,
                    topLeft = topLeft,
                    size = Size(edgePillH, edgePillW),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f)
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = topLeft,
                    size = Size(edgePillH, edgePillW),
                    cornerRadius = CornerRadius(edgePillH / 2f, edgePillH / 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // 4. Floating Action Mini Toolbar (Delete & Done)
        // Position above the box if room allows; otherwise place below
        val toolbarOffsetY = if (screenTop > toolbarHPx + toolbarMarginPx) {
            (screenTop - toolbarHPx - toolbarMarginPx).roundToInt()
        } else {
            (screenBottom + toolbarMarginPx).roundToInt()
        }
        val toolbarOffsetX = (screenMidX - toolbarWPx / 2f)
            .coerceIn(4f, (canvasWidth - toolbarWPx - 4f).coerceAtLeast(4f))
            .roundToInt()

        Surface(
            modifier = Modifier
                .offset { IntOffset(toolbarOffsetX, toolbarOffsetY) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 3.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Delete button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete markup layer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Done / Deselect button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDeselect),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done adjusting markup layer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
