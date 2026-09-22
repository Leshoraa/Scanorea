package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import kotlin.math.hypot

private enum class DragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    RIGHT_EDGE,
    BODY
}

/**
 * Interactive touch overlay allowing users to drag corners, edges, or the interior body
 * to crop document images. Includes standard rule-of-thirds grid and L-bracket handles.
 */
@Composable
fun EditorCropOverlay(
    cropBounds: ImageCropBounds,
    onCropChange: (ImageCropBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropBounds) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectDragGestures

                        val cropRect = Rect(
                            left = cropBounds.left * w,
                            top = cropBounds.top * h,
                            right = cropBounds.right * w,
                            bottom = cropBounds.bottom * h
                        )

                        val touchRadius = 48.dp.toPx()

                        activeHandle = when {
                            hypot(startOffset.x - cropRect.left, startOffset.y - cropRect.top) < touchRadius -> DragHandle.TOP_LEFT
                            hypot(startOffset.x - cropRect.right, startOffset.y - cropRect.top) < touchRadius -> DragHandle.TOP_RIGHT
                            hypot(startOffset.x - cropRect.left, startOffset.y - cropRect.bottom) < touchRadius -> DragHandle.BOTTOM_LEFT
                            hypot(startOffset.x - cropRect.right, startOffset.y - cropRect.bottom) < touchRadius -> DragHandle.BOTTOM_RIGHT
                            kotlin.math.abs(startOffset.y - cropRect.top) < touchRadius && startOffset.x in cropRect.left..cropRect.right -> DragHandle.TOP_EDGE
                            kotlin.math.abs(startOffset.y - cropRect.bottom) < touchRadius && startOffset.x in cropRect.left..cropRect.right -> DragHandle.BOTTOM_EDGE
                            kotlin.math.abs(startOffset.x - cropRect.left) < touchRadius && startOffset.y in cropRect.top..cropRect.bottom -> DragHandle.LEFT_EDGE
                            kotlin.math.abs(startOffset.x - cropRect.right) < touchRadius && startOffset.y in cropRect.top..cropRect.bottom -> DragHandle.RIGHT_EDGE
                            cropRect.contains(startOffset) -> DragHandle.BODY
                            else -> DragHandle.NONE
                        }
                    },
                    onDragEnd = { activeHandle = DragHandle.NONE },
                    onDragCancel = { activeHandle = DragHandle.NONE },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectDragGestures

                        val deltaX = dragAmount.x / w
                        val deltaY = dragAmount.y / h
                        val minSize = 0.08f

                        var newL = cropBounds.left
                        var newT = cropBounds.top
                        var newR = cropBounds.right
                        var newB = cropBounds.bottom

                        when (activeHandle) {
                            DragHandle.TOP_LEFT -> {
                                newL = (cropBounds.left + deltaX).coerceIn(0f, cropBounds.right - minSize)
                                newT = (cropBounds.top + deltaY).coerceIn(0f, cropBounds.bottom - minSize)
                            }
                            DragHandle.TOP_RIGHT -> {
                                newR = (cropBounds.right + deltaX).coerceIn(cropBounds.left + minSize, 1f)
                                newT = (cropBounds.top + deltaY).coerceIn(0f, cropBounds.bottom - minSize)
                            }
                            DragHandle.BOTTOM_LEFT -> {
                                newL = (cropBounds.left + deltaX).coerceIn(0f, cropBounds.right - minSize)
                                newB = (cropBounds.bottom + deltaY).coerceIn(cropBounds.top + minSize, 1f)
                            }
                            DragHandle.BOTTOM_RIGHT -> {
                                newR = (cropBounds.right + deltaX).coerceIn(cropBounds.left + minSize, 1f)
                                newB = (cropBounds.bottom + deltaY).coerceIn(cropBounds.top + minSize, 1f)
                            }
                            DragHandle.TOP_EDGE -> {
                                newT = (cropBounds.top + deltaY).coerceIn(0f, cropBounds.bottom - minSize)
                            }
                            DragHandle.BOTTOM_EDGE -> {
                                newB = (cropBounds.bottom + deltaY).coerceIn(cropBounds.top + minSize, 1f)
                            }
                            DragHandle.LEFT_EDGE -> {
                                newL = (cropBounds.left + deltaX).coerceIn(0f, cropBounds.right - minSize)
                            }
                            DragHandle.RIGHT_EDGE -> {
                                newR = (cropBounds.right + deltaX).coerceIn(cropBounds.left + minSize, 1f)
                            }
                            DragHandle.BODY -> {
                                val currentW = cropBounds.right - cropBounds.left
                                val currentH = cropBounds.bottom - cropBounds.top

                                var shiftX = deltaX
                                var shiftY = deltaY

                                if (cropBounds.left + shiftX < 0f) shiftX = -cropBounds.left
                                if (cropBounds.right + shiftX > 1f) shiftX = 1f - cropBounds.right
                                if (cropBounds.top + shiftY < 0f) shiftY = -cropBounds.top
                                if (cropBounds.bottom + shiftY > 1f) shiftY = 1f - cropBounds.bottom

                                newL = cropBounds.left + shiftX
                                newR = newL + currentW
                                newT = cropBounds.top + shiftY
                                newB = newT + currentH
                            }
                            DragHandle.NONE -> {}
                        }

                        if (newL < newR && newT < newB) {
                            onCropChange(ImageCropBounds.ofClamped(newL, newT, newR, newB, minSize))
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val leftPx = cropBounds.left * w
            val topPx = cropBounds.top * h
            val rightPx = cropBounds.right * w
            val bottomPx = cropBounds.bottom * h
            val rectWidth = rightPx - leftPx
            val rectHeight = bottomPx - topPx

            // 1. Dark Scrim outside crop boundary
            val cropPath = Path().apply {
                addRect(Rect(leftPx, topPx, rightPx, bottomPx))
            }
            clipPath(cropPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.55f))
            }

            // 2. Crop border outline
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(leftPx, topPx),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Rule of Thirds Grid Lines
            val colStep = rectWidth / 3f
            val rowStep = rectHeight / 3f
            val gridColor = Color.White.copy(alpha = 0.35f)
            val gridStroke = 1.dp.toPx()

            // Vertical grid lines
            drawLine(gridColor, Offset(leftPx + colStep, topPx), Offset(leftPx + colStep, bottomPx), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(leftPx + colStep * 2, topPx), Offset(leftPx + colStep * 2, bottomPx), strokeWidth = gridStroke)

            // Horizontal grid lines
            drawLine(gridColor, Offset(leftPx, topPx + rowStep), Offset(rightPx, topPx + rowStep), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(leftPx, topPx + rowStep * 2), Offset(rightPx, topPx + rowStep * 2), strokeWidth = gridStroke)

            // 4. Solid L-shaped corner brackets (Material 3 primary colored)
            val cornerLen = 22.dp.toPx()
            val cornerStroke = 3.5.dp.toPx()

            // Top-Left corner
            drawLine(primaryColor, Offset(leftPx - 1, topPx), Offset(leftPx + cornerLen, topPx), strokeWidth = cornerStroke)
            drawLine(primaryColor, Offset(leftPx, topPx - 1), Offset(leftPx, topPx + cornerLen), strokeWidth = cornerStroke)

            // Top-Right corner
            drawLine(primaryColor, Offset(rightPx + 1, topPx), Offset(rightPx - cornerLen, topPx), strokeWidth = cornerStroke)
            drawLine(primaryColor, Offset(rightPx, topPx - 1), Offset(rightPx, topPx + cornerLen), strokeWidth = cornerStroke)

            // Bottom-Left corner
            drawLine(primaryColor, Offset(leftPx - 1, bottomPx), Offset(leftPx + cornerLen, bottomPx), strokeWidth = cornerStroke)
            drawLine(primaryColor, Offset(leftPx, bottomPx + 1), Offset(leftPx, bottomPx - cornerLen), strokeWidth = cornerStroke)

            // Bottom-Right corner
            drawLine(primaryColor, Offset(rightPx + 1, bottomPx), Offset(rightPx - cornerLen, bottomPx), strokeWidth = cornerStroke)
            drawLine(primaryColor, Offset(rightPx, bottomPx + 1), Offset(rightPx, bottomPx - cornerLen), strokeWidth = cornerStroke)
        }
    }
}
