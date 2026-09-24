package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Canvas layer that draws both committed page annotations and any live in-progress annotation.
 */
@Composable
fun EditorAnnotationCanvas(
    annotations: List<PageAnnotation>,
    activeAnnotation: PageAnnotation? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val density = this.density

        // Render committed annotations
        annotations.forEach { annotation ->
            drawAnnotation(annotation, density)
        }

        // Render live in-progress annotation
        if (activeAnnotation != null) {
            drawAnnotation(activeAnnotation, density)
        }
    }
}

private fun DrawScope.drawAnnotation(annotation: PageAnnotation, density: Float) {
    val canvasW = size.width
    val canvasH = size.height
    if (canvasW <= 0f || canvasH <= 0f) return

    when (annotation) {
        is PageAnnotation.FreehandPath -> {
            val pointsList = annotation.points
            if (pointsList.size > 1) {
                val path = Path()
                val firstPoint = pointsList.first()
                path.moveTo(firstPoint.x * canvasW, firstPoint.y * canvasH)
                for (i in 1 until pointsList.size) {
                    val currentPoint = pointsList[i]
                    path.lineTo(currentPoint.x * canvasW, currentPoint.y * canvasH)
                }
                drawPath(
                    path = path,
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    style = Stroke(
                        width = (annotation.strokeWidth * density).coerceAtLeast(1.5f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (pointsList.size == 1) {
                val singlePoint = pointsList.first()
                drawCircle(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    radius = (annotation.strokeWidth * density / 2f).coerceAtLeast(2f),
                    center = Offset(singlePoint.x * canvasW, singlePoint.y * canvasH)
                )
            }
        }

        is PageAnnotation.RectBox -> {
            val leftBound = min(annotation.left, annotation.right) * canvasW
            val topBound = min(annotation.top, annotation.bottom) * canvasH
            val rightBound = max(annotation.left, annotation.right) * canvasW
            val bottomBound = max(annotation.top, annotation.bottom) * canvasH
            val rectWidth = (rightBound - leftBound).coerceAtLeast(1f)
            val rectHeight = (bottomBound - topBound).coerceAtLeast(1f)

            if (annotation.isFilled) {
                drawRect(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    topLeft = Offset(leftBound, topBound),
                    size = Size(rectWidth, rectHeight)
                )
            } else {
                drawRect(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    topLeft = Offset(leftBound, topBound),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(
                        width = (annotation.strokeWidth * density).coerceAtLeast(1.5f)
                    )
                )
            }
        }

        is PageAnnotation.OvalShape -> {
            val leftBound = min(annotation.left, annotation.right) * canvasW
            val topBound = min(annotation.top, annotation.bottom) * canvasH
            val rightBound = max(annotation.left, annotation.right) * canvasW
            val bottomBound = max(annotation.top, annotation.bottom) * canvasH
            val ovalWidth = (rightBound - leftBound).coerceAtLeast(1f)
            val ovalHeight = (bottomBound - topBound).coerceAtLeast(1f)

            drawOval(
                color = Color(annotation.color).copy(alpha = annotation.alpha),
                topLeft = Offset(leftBound, topBound),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(
                    width = (annotation.strokeWidth * density).coerceAtLeast(1.5f)
                )
            )
        }

        is PageAnnotation.ArrowLine -> {
            val startX = annotation.startX * canvasW
            val startY = annotation.startY * canvasH
            val endX = annotation.endX * canvasW
            val endY = annotation.endY * canvasH
            val strokePx = (annotation.strokeWidth * density).coerceAtLeast(2f)
            val strokeColor = Color(annotation.color).copy(alpha = annotation.alpha)

            drawLine(
                color = strokeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Arrow head calculation
            val angle = atan2(endY - startY, endX - startX)
            val arrowLength = (strokePx * 3.8f).coerceIn(16f * (density / 2.5f), 48f * (density / 2.5f))
            val arrowAngle = Math.PI / 6.0
            val arrowTipLeftX = endX - arrowLength * cos(angle - arrowAngle).toFloat()
            val arrowTipLeftY = endY - arrowLength * sin(angle - arrowAngle).toFloat()
            val arrowTipRightX = endX - arrowLength * cos(angle + arrowAngle).toFloat()
            val arrowTipRightY = endY - arrowLength * sin(angle + arrowAngle).toFloat()

            drawLine(
                color = strokeColor,
                start = Offset(endX, endY),
                end = Offset(arrowTipLeftX, arrowTipLeftY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = Offset(endX, endY),
                end = Offset(arrowTipRightX, arrowTipRightY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}
