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
            val pts = annotation.points
            if (pts.size > 1) {
                val path = Path()
                val first = pts.first()
                path.moveTo(first.x * canvasW, first.y * canvasH)
                for (i in 1 until pts.size) {
                    val p = pts[i]
                    path.lineTo(p.x * canvasW, p.y * canvasH)
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
            } else if (pts.size == 1) {
                val p = pts.first()
                drawCircle(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    radius = (annotation.strokeWidth * density / 2f).coerceAtLeast(2f),
                    center = Offset(p.x * canvasW, p.y * canvasH)
                )
            }
        }

        is PageAnnotation.RectBox -> {
            val l = min(annotation.left, annotation.right) * canvasW
            val t = min(annotation.top, annotation.bottom) * canvasH
            val r = max(annotation.left, annotation.right) * canvasW
            val b = max(annotation.top, annotation.bottom) * canvasH
            val w = (r - l).coerceAtLeast(1f)
            val h = (b - t).coerceAtLeast(1f)

            if (annotation.isFilled) {
                drawRect(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    topLeft = Offset(l, t),
                    size = Size(w, h)
                )
            } else {
                drawRect(
                    color = Color(annotation.color).copy(alpha = annotation.alpha),
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                    style = Stroke(
                        width = (annotation.strokeWidth * density).coerceAtLeast(1.5f)
                    )
                )
            }
        }

        is PageAnnotation.OvalShape -> {
            val l = min(annotation.left, annotation.right) * canvasW
            val t = min(annotation.top, annotation.bottom) * canvasH
            val r = max(annotation.left, annotation.right) * canvasW
            val b = max(annotation.top, annotation.bottom) * canvasH
            val w = (r - l).coerceAtLeast(1f)
            val h = (b - t).coerceAtLeast(1f)

            drawOval(
                color = Color(annotation.color).copy(alpha = annotation.alpha),
                topLeft = Offset(l, t),
                size = Size(w, h),
                style = Stroke(
                    width = (annotation.strokeWidth * density).coerceAtLeast(1.5f)
                )
            )
        }

        is PageAnnotation.ArrowLine -> {
            val sx = annotation.startX * canvasW
            val sy = annotation.startY * canvasH
            val ex = annotation.endX * canvasW
            val ey = annotation.endY * canvasH
            val strokePx = (annotation.strokeWidth * density).coerceAtLeast(2f)
            val strokeColor = Color(annotation.color).copy(alpha = annotation.alpha)

            drawLine(
                color = strokeColor,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Arrow head calculation
            val angle = atan2(ey - sy, ex - sx)
            val arrowLen = (strokePx * 3.8f).coerceIn(16f * (density / 2.5f), 48f * (density / 2.5f))
            val arrowAngle = Math.PI / 6.0
            val x1 = ex - arrowLen * cos(angle - arrowAngle).toFloat()
            val y1 = ey - arrowLen * sin(angle - arrowAngle).toFloat()
            val x2 = ex - arrowLen * cos(angle + arrowAngle).toFloat()
            val y2 = ey - arrowLen * sin(angle + arrowAngle).toFloat()

            drawLine(
                color = strokeColor,
                start = Offset(ex, ey),
                end = Offset(x1, y1),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = Offset(ex, ey),
                end = Offset(x2, y2),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}
