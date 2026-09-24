package com.leshoraa.scanorea.features.editor.domain.model

import java.util.UUID

/**
 * Annotation tools available for document markup.
 */
enum class AnnotationTool(val label: String) {
    PEN("Pen"),
    HIGHLIGHTER("Highlighter"),
    REDACT("Redact"),
    RECT_SHAPE("Rectangle"),
    OVAL_SHAPE("Circle"),
    ARROW_SHAPE("Arrow")
}

/**
 * Predefined stroke width presets for pen, shapes, and arrows.
 */
enum class StrokeSize(val label: String, val widthDp: Float) {
    THIN("Thin", 2.5f),
    MEDIUM("Med", 5.0f),
    THICK("Thick", 10.0f)
}

/**
 * Standard preset colors for document markup.
 */
object AnnotationColors {
    const val YELLOW = 0xFFFFEB3B
    const val RED = 0xFFE53935
    const val GREEN = 0xFF4CAF50
    const val BLUE = 0xFF2196F3
    const val ORANGE = 0xFFFF9800
    const val PURPLE = 0xFF9C27B0
    const val BLACK = 0xFF111111
    const val WHITE = 0xFFFFFFFF

    val PRESETS: List<Long> = listOf(
        YELLOW,
        RED,
        GREEN,
        BLUE,
        ORANGE,
        PURPLE,
        BLACK,
        WHITE
    )
}

/**
 * Normalized 2D coordinate on a document page where (0,0) is top-left and (1,1) is bottom-right.
 */
data class NormalizedPoint(val x: Float, val y: Float)

/**
 * Sealed hierarchy of vector annotations supported on an ImagePage.
 */
sealed interface PageAnnotation {
    val id: String
    val color: Long
    val strokeWidth: Float
    val alpha: Float
    val tool: AnnotationTool

    /**
     * Freehand curved drawing path.
     */
    data class FreehandPath(
        override val id: String = UUID.randomUUID().toString(),
        override val color: Long,
        override val strokeWidth: Float = 4f,
        override val alpha: Float = 1.0f,
        val points: List<NormalizedPoint>
    ) : PageAnnotation {
        override val tool: AnnotationTool = AnnotationTool.PEN
    }

    /**
     * Rectangle annotation: can be a semi-transparent highlighter box, solid blackout redact box, or outlined border.
     */
    data class RectBox(
        override val id: String = UUID.randomUUID().toString(),
        override val color: Long,
        override val strokeWidth: Float = 3f,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val isFilled: Boolean,
        override val alpha: Float = 1.0f,
        override val tool: AnnotationTool = if (isFilled && alpha < 1.0f) AnnotationTool.HIGHLIGHTER
        else if (isFilled) AnnotationTool.REDACT
        else AnnotationTool.RECT_SHAPE
    ) : PageAnnotation

    /**
     * Outlined ellipse / oval shape.
     */
    data class OvalShape(
        override val id: String = UUID.randomUUID().toString(),
        override val color: Long,
        override val strokeWidth: Float = 3f,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        override val alpha: Float = 1.0f
    ) : PageAnnotation {
        override val tool: AnnotationTool = AnnotationTool.OVAL_SHAPE
    }

    /**
     * Directed arrow annotation pointing from (startX, startY) to (endX, endY).
     */
    data class ArrowLine(
        override val id: String = UUID.randomUUID().toString(),
        override val color: Long,
        override val strokeWidth: Float = 4f,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        override val alpha: Float = 1.0f
    ) : PageAnnotation {
        override val tool: AnnotationTool = AnnotationTool.ARROW_SHAPE
    }
}
