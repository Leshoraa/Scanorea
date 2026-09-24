package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import kotlin.math.hypot

enum class DragHandle {
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
 * Visual Canvas rendering the crop scrim, outline, rule-of-thirds grid,
 * corner brackets, and mid-edge grab bars.
 */
@Composable
fun EditorCropOverlayCanvas(
    cropBounds: ImageCropBounds,
    activeHandle: DragHandle = DragHandle.NONE,
    touchMargin: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val touchMarginPx = touchMargin.toPx()
        val totalW = size.width
        val totalH = size.height
        val imageW = (totalW - touchMarginPx * 2f).coerceAtLeast(1f)
        val imageH = (totalH - touchMarginPx * 2f).coerceAtLeast(1f)

        val leftPx = touchMarginPx + cropBounds.left * imageW
        val topPx = touchMarginPx + cropBounds.top * imageH
        val rightPx = touchMarginPx + cropBounds.right * imageW
        val bottomPx = touchMarginPx + cropBounds.bottom * imageH
        val rectWidth = rightPx - leftPx
        val rectHeight = bottomPx - topPx

        // 1. Dark Scrim outside crop boundary (covering image area)
        val cropPath = Path().apply {
            addRect(Rect(leftPx, topPx, rightPx, bottomPx))
        }
        clipPath(cropPath, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(touchMarginPx, touchMarginPx),
                size = Size(imageW, imageH)
            )
        }

        // Highlight whole-frame when dragging the body
        if (activeHandle == DragHandle.BODY) {
            drawRect(
                color = primaryColor.copy(alpha = 0.12f),
                topLeft = Offset(leftPx, topPx),
                size = Size(rectWidth, rectHeight)
            )
        }

        // 2. Crop border outline (Solid, clean 1.5.dp line)
        drawRect(
            color = if (activeHandle == DragHandle.BODY) primaryColor else Color.White.copy(alpha = 0.85f),
            topLeft = Offset(leftPx, topPx),
            size = Size(rectWidth, rectHeight),
            style = Stroke(width = if (activeHandle == DragHandle.BODY) 2.dp.toPx() else 1.5.dp.toPx())
        )

        // 3. Rule of Thirds Grid Lines (Subtle guide lines)
        val colStep = rectWidth / 3f
        val rowStep = rectHeight / 3f
        val gridColor = Color.White.copy(alpha = 0.30f)
        val gridStroke = 0.85.dp.toPx()

        // Vertical grid lines
        drawLine(gridColor, Offset(leftPx + colStep, topPx), Offset(leftPx + colStep, bottomPx), strokeWidth = gridStroke)
        drawLine(gridColor, Offset(leftPx + colStep * 2, topPx), Offset(leftPx + colStep * 2, bottomPx), strokeWidth = gridStroke)

        // Horizontal grid lines
        drawLine(gridColor, Offset(leftPx, topPx + rowStep), Offset(rightPx, topPx + rowStep), strokeWidth = gridStroke)
        drawLine(gridColor, Offset(leftPx, topPx + rowStep * 2), Offset(rightPx, topPx + rowStep * 2), strokeWidth = gridStroke)

        // 4. Solid L-shaped corner brackets (3.dp stroke, seamless rounded join, no circles)
        val cornerLen = 22.dp.toPx()
        val cornerStrokeStyle = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Top-Left corner
        val topLeftPath = Path().apply {
            moveTo(leftPx, topPx + cornerLen)
            lineTo(leftPx, topPx)
            lineTo(leftPx + cornerLen, topPx)
        }
        drawPath(topLeftPath, color = primaryColor, style = cornerStrokeStyle)

        // Top-Right corner
        val topRightPath = Path().apply {
            moveTo(rightPx - cornerLen, topPx)
            lineTo(rightPx, topPx)
            lineTo(rightPx, topPx + cornerLen)
        }
        drawPath(topRightPath, color = primaryColor, style = cornerStrokeStyle)

        // Bottom-Left corner
        val bottomLeftPath = Path().apply {
            moveTo(leftPx, bottomPx - cornerLen)
            lineTo(leftPx, bottomPx)
            lineTo(leftPx + cornerLen, bottomPx)
        }
        drawPath(bottomLeftPath, color = primaryColor, style = cornerStrokeStyle)

        // Bottom-Right corner
        val bottomRightPath = Path().apply {
            moveTo(rightPx - cornerLen, bottomPx)
            lineTo(rightPx, bottomPx)
            lineTo(rightPx, bottomPx - cornerLen)
        }
        drawPath(bottomRightPath, color = primaryColor, style = cornerStrokeStyle)

        // 5. Mid-edge Drag Indicators (Horizontal and Vertical Grab Bars)
        val edgeBarLength = 28.dp.toPx()
        val edgeBarStrokeWidth = 3.5.dp.toPx()

        val midX = (leftPx + rightPx) / 2f
        val midY = (topPx + bottomPx) / 2f

        // Top Edge Handle (Horizontal pill)
        if (rectWidth > edgeBarLength * 1.5f) {
            drawLine(
                color = primaryColor,
                start = Offset(midX - edgeBarLength / 2f, topPx),
                end = Offset(midX + edgeBarLength / 2f, topPx),
                strokeWidth = if (activeHandle == DragHandle.TOP_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Bottom Edge Handle (Horizontal pill)
        if (rectWidth > edgeBarLength * 1.5f) {
            drawLine(
                color = primaryColor,
                start = Offset(midX - edgeBarLength / 2f, bottomPx),
                end = Offset(midX + edgeBarLength / 2f, bottomPx),
                strokeWidth = if (activeHandle == DragHandle.BOTTOM_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Left Edge Handle (Vertical pill)
        if (rectHeight > edgeBarLength * 1.5f) {
            drawLine(
                color = primaryColor,
                start = Offset(leftPx, midY - edgeBarLength / 2f),
                end = Offset(leftPx, midY + edgeBarLength / 2f),
                strokeWidth = if (activeHandle == DragHandle.LEFT_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Right Edge Handle (Vertical pill)
        if (rectHeight > edgeBarLength * 1.5f) {
            drawLine(
                color = primaryColor,
                start = Offset(rightPx, midY - edgeBarLength / 2f),
                end = Offset(rightPx, midY + edgeBarLength / 2f),
                strokeWidth = if (activeHandle == DragHandle.RIGHT_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Interactive touch overlay allowing users to drag corners or edges
 * to crop document images with pixel precision.
 *
 * Implements persistent pointer gesture detection decoupled from recomposition,
 * tactile haptic feedback on handle acquisition, mid-edge grab handles for effortless
 * horizontal & vertical dragging, touch target extension beyond boundary edges,
 * and seamless multi-touch pinch-to-zoom / pan delegation.
 *
 * @param cropBounds Active normalized crop boundaries [0..1].
 * @param onCropChange Callback invoked when crop bounds are committed upon gesture completion.
 * @param touchMargin Outward touch padding margin around image boundaries.
 * @param zoomScale Current viewport zoom scale (1f..4f).
 * @param onTransform Multi-touch pinch-zoom and pan callback.
 * @param onPan 1-finger viewport pan callback when zoomed in.
 * @param onDoubleTap Double-tap zoom toggle callback.
 */
@Composable
fun EditorCropOverlay(
    cropBounds: ImageCropBounds,
    onCropChange: (ImageCropBounds) -> Unit,
    modifier: Modifier = Modifier,
    touchMargin: Dp = 24.dp,
    zoomScale: Float = 1.0f,
    onTransform: (zoomChange: Float, panChange: Offset) -> Unit = { _, _ -> },
    onPan: (Offset) -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var isDraggingHandle by remember { mutableStateOf(false) }
    val localBoundsState = remember { mutableStateOf(cropBounds) }
    val currentOnCropChange by rememberUpdatedState(onCropChange)
    val currentZoomScale by rememberUpdatedState(zoomScale)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Synchronize external bounds updates (e.g. from preset chips or reset button)
    LaunchedEffect(cropBounds) {
        if (!isDraggingHandle && localBoundsState.value != cropBounds) {
            localBoundsState.value = cropBounds
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchMarginPx = touchMargin.toPx()
                val cornerHitRadius = 26.dp.toPx()
                val edgeHitRadius = 44.dp.toPx()

                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startOffset = down.position

                    val totalW = size.width.toFloat()
                    val totalH = size.height.toFloat()
                    val imageW = (totalW - touchMarginPx * 2f).coerceAtLeast(1f)
                    val imageH = (totalH - touchMarginPx * 2f).coerceAtLeast(1f)

                    val bounds = localBoundsState.value
                    val leftPx = touchMarginPx + bounds.left * imageW
                    val topPx = touchMarginPx + bounds.top * imageH
                    val rightPx = touchMarginPx + bounds.right * imageW
                    val bottomPx = touchMarginPx + bounds.bottom * imageH

                    val x = startOffset.x
                    val y = startOffset.y

                    // Distances to the 4 corner points
                    val dTL = hypot(x - leftPx, y - topPx)
                    val dTR = hypot(x - rightPx, y - topPx)
                    val dBL = hypot(x - leftPx, y - bottomPx)
                    val dBR = hypot(x - rightPx, y - bottomPx)

                    // Perpendicular distances to the 4 edge segments
                    val edgePad = 12.dp.toPx()
                    val nearTop = x in (leftPx - edgePad)..(rightPx + edgePad)
                    val dTop = if (nearTop) kotlin.math.abs(y - topPx) else Float.MAX_VALUE

                    val nearBottom = x in (leftPx - edgePad)..(rightPx + edgePad)
                    val dBottom = if (nearBottom) kotlin.math.abs(y - bottomPx) else Float.MAX_VALUE

                    val nearLeft = y in (topPx - edgePad)..(bottomPx + edgePad)
                    val dLeft = if (nearLeft) kotlin.math.abs(x - leftPx) else Float.MAX_VALUE

                    val nearRight = y in (topPx - edgePad)..(bottomPx + edgePad)
                    val dRight = if (nearRight) kotlin.math.abs(x - rightPx) else Float.MAX_VALUE

                    val candidates = mutableListOf<Pair<DragHandle, Float>>()

                    // Corner vertex touches (prioritize corner if user tapped right at the corner bracket)
                    if (dTL < cornerHitRadius) candidates.add(DragHandle.TOP_LEFT to dTL)
                    if (dTR < cornerHitRadius) candidates.add(DragHandle.TOP_RIGHT to dTR)
                    if (dBL < cornerHitRadius) candidates.add(DragHandle.BOTTOM_LEFT to dBL)
                    if (dBR < cornerHitRadius) candidates.add(DragHandle.BOTTOM_RIGHT to dBR)

                    // Edge touches
                    if (dTop < edgeHitRadius) candidates.add(DragHandle.TOP_EDGE to dTop)
                    if (dBottom < edgeHitRadius) candidates.add(DragHandle.BOTTOM_EDGE to dBottom)
                    if (dLeft < edgeHitRadius) candidates.add(DragHandle.LEFT_EDGE to dLeft)
                    if (dRight < edgeHitRadius) candidates.add(DragHandle.RIGHT_EDGE to dRight)

                    // Select candidate with the smallest distance
                    var handle = candidates.minByOrNull { it.second }?.first ?: DragHandle.NONE
                    if (handle == DragHandle.NONE && x in leftPx..rightPx && y in topPx..bottomPx) {
                        handle = DragHandle.BODY
                    }

                    if (handle != DragHandle.NONE) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isDraggingHandle = true
                    }
                    activeHandle = handle

                    var isMultiTouch = false
                    var prevSpan = 0f
                    var prevCentroid = Offset.Zero
                    val startTime = System.currentTimeMillis()
                    var totalDragDistance = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            // Touch released
                            val duration = System.currentTimeMillis() - startTime
                            if (totalDragDistance < 15f && duration < 300L) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 350L && hypot(startOffset.x - lastTapPos.x, startOffset.y - lastTapPos.y) < 60.dp.toPx()) {
                                    currentOnDoubleTap()
                                    lastTapTime = 0L
                                } else {
                                    lastTapTime = now
                                    lastTapPos = startOffset
                                }
                            }

                            if (activeHandle != DragHandle.NONE) {
                                val committed = localBoundsState.value
                                if (committed != cropBounds) {
                                    currentOnCropChange(committed)
                                }
                            }
                            activeHandle = DragHandle.NONE
                            isDraggingHandle = false
                            break
                        }

                        if (pressed.size >= 2) {
                            // Multi-touch pinch-to-zoom & pan
                            isMultiTouch = true
                            activeHandle = DragHandle.NONE
                            isDraggingHandle = false

                            val p0 = pressed[0].position
                            val p1 = pressed[1].position
                            val currentSpan = hypot(p0.x - p1.x, p0.y - p1.y)
                            val currentCentroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                            if (prevSpan > 0f && currentSpan > 0f) {
                                val zoomChange = currentSpan / prevSpan
                                val panChange = currentCentroid - prevCentroid
                                currentOnTransform(zoomChange, panChange)
                            }

                            prevSpan = currentSpan
                            prevCentroid = currentCentroid
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1 && !isMultiTouch) {
                            // Single pointer drag
                            val change = pressed[0]
                            val dragAmount = change.positionChange()
                            totalDragDistance += hypot(dragAmount.x, dragAmount.y)

                            if (activeHandle != DragHandle.NONE) {
                                change.consume()
                                val deltaX = dragAmount.x / imageW
                                val deltaY = dragAmount.y / imageH
                                val minSize = 0.08f

                                val prev = localBoundsState.value
                                var newL = prev.left
                                var newT = prev.top
                                var newR = prev.right
                                var newB = prev.bottom

                                when (activeHandle) {
                                    DragHandle.TOP_LEFT -> {
                                        newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                        newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                    }
                                    DragHandle.TOP_RIGHT -> {
                                        newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                        newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                    }
                                    DragHandle.BOTTOM_LEFT -> {
                                        newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                        newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                    }
                                    DragHandle.BOTTOM_RIGHT -> {
                                        newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                        newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                    }
                                    DragHandle.TOP_EDGE -> {
                                        newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                    }
                                    DragHandle.BOTTOM_EDGE -> {
                                        newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                    }
                                    DragHandle.LEFT_EDGE -> {
                                        newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                    }
                                    DragHandle.RIGHT_EDGE -> {
                                        newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                    }
                                    DragHandle.BODY -> {
                                        val boxWidth = prev.right - prev.left
                                        val boxHeight = prev.bottom - prev.top
                                        newL = (prev.left + deltaX).coerceIn(0f, 1f - boxWidth)
                                        newT = (prev.top + deltaY).coerceIn(0f, 1f - boxHeight)
                                        newR = newL + boxWidth
                                        newB = newT + boxHeight
                                    }
                                    DragHandle.NONE -> {}
                                }

                                if (newL < newR && newT < newB) {
                                    localBoundsState.value = ImageCropBounds.ofClamped(newL, newT, newR, newB, minSize)
                                }
                            } else if (currentZoomScale > 1.05f) {
                                change.consume()
                                currentOnPan(dragAmount)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val touchMarginPx = touchMargin.toPx()
            val totalW = size.width
            val totalH = size.height
            val imageW = (totalW - touchMarginPx * 2f).coerceAtLeast(1f)
            val imageH = (totalH - touchMarginPx * 2f).coerceAtLeast(1f)

            val currentBounds = localBoundsState.value
            val leftPx = touchMarginPx + currentBounds.left * imageW
            val topPx = touchMarginPx + currentBounds.top * imageH
            val rightPx = touchMarginPx + currentBounds.right * imageW
            val bottomPx = touchMarginPx + currentBounds.bottom * imageH
            val rectWidth = rightPx - leftPx
            val rectHeight = bottomPx - topPx

            // 1. Dark Scrim outside crop boundary (covering image area)
            val cropPath = Path().apply {
                addRect(Rect(leftPx, topPx, rightPx, bottomPx))
            }
            clipPath(cropPath, clipOp = ClipOp.Difference) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(touchMarginPx, touchMarginPx),
                    size = Size(imageW, imageH)
                )
            }

            // 2. Crop border outline (Solid, clean 1.5.dp line)
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(leftPx, topPx),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Rule of Thirds Grid Lines (Subtle guide lines)
            val colStep = rectWidth / 3f
            val rowStep = rectHeight / 3f
            val gridColor = Color.White.copy(alpha = 0.30f)
            val gridStroke = 0.85.dp.toPx()

            // Vertical grid lines
            drawLine(gridColor, Offset(leftPx + colStep, topPx), Offset(leftPx + colStep, bottomPx), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(leftPx + colStep * 2, topPx), Offset(leftPx + colStep * 2, bottomPx), strokeWidth = gridStroke)

            // Horizontal grid lines
            drawLine(gridColor, Offset(leftPx, topPx + rowStep), Offset(rightPx, topPx + rowStep), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(leftPx, topPx + rowStep * 2), Offset(rightPx, topPx + rowStep * 2), strokeWidth = gridStroke)

            // 4. Solid L-shaped corner brackets (3.dp stroke, seamless rounded join, no circles)
            val cornerLen = 22.dp.toPx()
            val cornerStrokeStyle = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Top-Left corner
            val topLeftPath = Path().apply {
                moveTo(leftPx, topPx + cornerLen)
                lineTo(leftPx, topPx)
                lineTo(leftPx + cornerLen, topPx)
            }
            drawPath(topLeftPath, color = primaryColor, style = cornerStrokeStyle)

            // Top-Right corner
            val topRightPath = Path().apply {
                moveTo(rightPx - cornerLen, topPx)
                lineTo(rightPx, topPx)
                lineTo(rightPx, topPx + cornerLen)
            }
            drawPath(topRightPath, color = primaryColor, style = cornerStrokeStyle)

            // Bottom-Left corner
            val bottomLeftPath = Path().apply {
                moveTo(leftPx, bottomPx - cornerLen)
                lineTo(leftPx, bottomPx)
                lineTo(leftPx + cornerLen, bottomPx)
            }
            drawPath(bottomLeftPath, color = primaryColor, style = cornerStrokeStyle)

            // Bottom-Right corner
            val bottomRightPath = Path().apply {
                moveTo(rightPx - cornerLen, bottomPx)
                lineTo(rightPx, bottomPx)
                lineTo(rightPx, bottomPx - cornerLen)
            }
            drawPath(bottomRightPath, color = primaryColor, style = cornerStrokeStyle)

            // 5. Mid-edge Drag Indicators (Horizontal and Vertical Grab Bars)
            val edgeBarLength = 28.dp.toPx()
            val edgeBarStrokeWidth = 3.5.dp.toPx()

            val midX = (leftPx + rightPx) / 2f
            val midY = (topPx + bottomPx) / 2f

            // Top Edge Handle (Horizontal pill)
            if (rectWidth > edgeBarLength * 1.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(midX - edgeBarLength / 2f, topPx),
                    end = Offset(midX + edgeBarLength / 2f, topPx),
                    strokeWidth = if (activeHandle == DragHandle.TOP_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Bottom Edge Handle (Horizontal pill)
            if (rectWidth > edgeBarLength * 1.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(midX - edgeBarLength / 2f, bottomPx),
                    end = Offset(midX + edgeBarLength / 2f, bottomPx),
                    strokeWidth = if (activeHandle == DragHandle.BOTTOM_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Left Edge Handle (Vertical pill)
            if (rectHeight > edgeBarLength * 1.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(leftPx, midY - edgeBarLength / 2f),
                    end = Offset(leftPx, midY + edgeBarLength / 2f),
                    strokeWidth = if (activeHandle == DragHandle.LEFT_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Right Edge Handle (Vertical pill)
            if (rectHeight > edgeBarLength * 1.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(rightPx, midY - edgeBarLength / 2f),
                    end = Offset(rightPx, midY + edgeBarLength / 2f),
                    strokeWidth = if (activeHandle == DragHandle.RIGHT_EDGE) edgeBarStrokeWidth * 1.3f else edgeBarStrokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
