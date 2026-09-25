package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.editor.domain.model.CornerHandle
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import kotlin.math.hypot

private val CORNER_PIN_OUTER_RADIUS = 16.dp
private val CORNER_PIN_INNER_RADIUS = 6.dp
private val CORNER_PIN_ACTIVE_RADIUS = 22.dp
private val CORNER_TOUCH_THRESHOLD = 44.dp
private val BORDER_STROKE_WIDTH = 2.5.dp
private val SCRIM_COLOR = Color.Black.copy(alpha = 0.52f)

/**
 * Interactive overlay allowing users to view and manually adjust the 4 corner pins
 * of a document quadrilateral for perspective keystone correction.
 */
@Composable
fun EditorPerspectiveOverlay(
    documentQuad: DocumentQuad,
    onQuadChange: (DocumentQuad) -> Unit,
    onQuadCommit: (DocumentQuad) -> Unit,
    modifier: Modifier = Modifier,
    touchMargin: Dp = 24.dp
) {
    val hapticFeedback = LocalHapticFeedback.current
    var activeHandle by remember { mutableStateOf(CornerHandle.NONE) }
    var localQuad by remember(documentQuad) { mutableStateOf(documentQuad) }

    val currentOnQuadChange by rememberUpdatedState(onQuadChange)
    val currentOnQuadCommit by rememberUpdatedState(onQuadCommit)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchMarginPx = touchMargin.toPx()
                val touchThresholdPx = CORNER_TOUCH_THRESHOLD.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val imageWidth = (size.width - 2 * touchMarginPx).coerceAtLeast(1f)
                    val imageHeight = (size.height - 2 * touchMarginPx).coerceAtLeast(1f)

                    val downPosition = down.position
                    val quad = localQuad

                    val tlPx = Offset(touchMarginPx + quad.topLeftX * imageWidth, touchMarginPx + quad.topLeftY * imageHeight)
                    val trPx = Offset(touchMarginPx + quad.topRightX * imageWidth, touchMarginPx + quad.topRightY * imageHeight)
                    val brPx = Offset(touchMarginPx + quad.bottomRightX * imageWidth, touchMarginPx + quad.bottomRightY * imageHeight)
                    val blPx = Offset(touchMarginPx + quad.bottomLeftX * imageWidth, touchMarginPx + quad.bottomLeftY * imageHeight)

                    val distTl = hypot(downPosition.x - tlPx.x, downPosition.y - tlPx.y)
                    val distTr = hypot(downPosition.x - trPx.x, downPosition.y - trPx.y)
                    val distBr = hypot(downPosition.x - brPx.x, downPosition.y - brPx.y)
                    val distBl = hypot(downPosition.x - blPx.x, downPosition.y - blPx.y)

                    val detectedHandle = when {
                        distTl <= touchThresholdPx && distTl <= distTr && distTl <= distBr && distTl <= distBl -> CornerHandle.TOP_LEFT
                        distTr <= touchThresholdPx && distTr <= distBr && distTr <= distBl -> CornerHandle.TOP_RIGHT
                        distBr <= touchThresholdPx && distBr <= distBl -> CornerHandle.BOTTOM_RIGHT
                        distBl <= touchThresholdPx -> CornerHandle.BOTTOM_LEFT
                        else -> CornerHandle.NONE
                    }

                    if (detectedHandle != CornerHandle.NONE) {
                        activeHandle = detectedHandle
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val normalizedX = ((change.position.x - touchMarginPx) / imageWidth).coerceIn(0f, 1f)
                            val normalizedY = ((change.position.y - touchMarginPx) / imageHeight).coerceIn(0f, 1f)

                            val updatedQuad = localQuad.withCorner(detectedHandle, normalizedX, normalizedY)
                            localQuad = updatedQuad
                            currentOnQuadChange(updatedQuad)
                            change.consume()
                        }

                        currentOnQuadCommit(localQuad)
                        activeHandle = CornerHandle.NONE
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val touchMarginPx = touchMargin.toPx()
            val imageWidth = (size.width - 2 * touchMarginPx).coerceAtLeast(1f)
            val imageHeight = (size.height - 2 * touchMarginPx).coerceAtLeast(1f)

            val quad = localQuad
            val tl = Offset(touchMarginPx + quad.topLeftX * imageWidth, touchMarginPx + quad.topLeftY * imageHeight)
            val tr = Offset(touchMarginPx + quad.topRightX * imageWidth, touchMarginPx + quad.topRightY * imageHeight)
            val br = Offset(touchMarginPx + quad.bottomRightX * imageWidth, touchMarginPx + quad.bottomRightY * imageHeight)
            val bl = Offset(touchMarginPx + quad.bottomLeftX * imageWidth, touchMarginPx + quad.bottomLeftY * imageHeight)

            val quadPath = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            // Darken outside quadrilateral
            clipPath(quadPath, clipOp = ClipOp.Difference) {
                drawRect(
                    color = SCRIM_COLOR,
                    topLeft = Offset(touchMarginPx, touchMarginPx),
                    size = Size(imageWidth, imageHeight)
                )
            }

            // Quadrilateral boundary stroke
            drawPath(
                path = quadPath,
                color = primaryColor,
                style = Stroke(
                    width = BORDER_STROKE_WIDTH.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw 4 corner pins
            drawCornerPin(tl, activeHandle == CornerHandle.TOP_LEFT, primaryColor, surfaceColor)
            drawCornerPin(tr, activeHandle == CornerHandle.TOP_RIGHT, primaryColor, surfaceColor)
            drawCornerPin(br, activeHandle == CornerHandle.BOTTOM_RIGHT, primaryColor, surfaceColor)
            drawCornerPin(bl, activeHandle == CornerHandle.BOTTOM_LEFT, primaryColor, surfaceColor)
        }
    }
}

private fun DrawScope.drawCornerPin(
    center: Offset,
    isActive: Boolean,
    primaryColor: Color,
    surfaceColor: Color
) {
    val outerRadius = if (isActive) CORNER_PIN_ACTIVE_RADIUS.toPx() else CORNER_PIN_OUTER_RADIUS.toPx()
    val innerRadius = CORNER_PIN_INNER_RADIUS.toPx()

    // Outer halo
    drawCircle(
        color = primaryColor.copy(alpha = if (isActive) 0.35f else 0.22f),
        radius = outerRadius,
        center = center
    )

    // Main pin circle
    drawCircle(
        color = primaryColor,
        radius = 11.dp.toPx(),
        center = center
    )

    // Inner white dot
    drawCircle(
        color = Color.White,
        radius = innerRadius,
        center = center
    )

    // Center focal point
    drawCircle(
        color = primaryColor,
        radius = 2.5.dp.toPx(),
        center = center
    )
}
